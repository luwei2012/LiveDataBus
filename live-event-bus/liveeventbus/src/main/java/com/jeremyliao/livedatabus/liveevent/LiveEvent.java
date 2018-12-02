/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jeremyliao.livedatabus.liveevent;

import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.Map;

import static android.arch.lifecycle.Lifecycle.State.CREATED;
import static android.arch.lifecycle.Lifecycle.State.DESTROYED;

/**
 */
public abstract class LiveEvent<T> {
    static final int START_VERSION = -1;
    static final int DEFAULT_PRIORITY = 0x10;
    private static final Object NOT_SET = new Object();
    private SafeIterableMap<LiveEventObserver<T>, ObserverWrapper> mObservers =
            new SafeIterableMap<>();

    // how many observers are in active state
    private int mActiveCount = 0;
    private volatile Object mData = NOT_SET;
    private int mVersion = START_VERSION;//全局的计数器，保证所有的事件id递增
    // 同时每个事件应该有一个id，所有观察者如果低于这个id表示没有处理过该事件

    private boolean mDispatchingValue;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean mDispatchInvalidated;

    private static void assertMainThread(String methodName) {
        if (!MainThreadManager.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }

    /**
     * 判断是否应该通知观察者
     *
     * @param observer 观察者
     * @return 观察者是否消费了事件
     */
    private boolean considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) {
            return false;
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check observer.active to keep it as the entrance for events. So even if
        // the observer moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return false;
        }
        if (mData == null || mData == NOT_SET || !(mData instanceof ObjectWrapper)
                || observer.mLastVersion >= ((ObjectWrapper) mData).getVersion()) {
            return false;
        }

        observer.mLastVersion = ((ObjectWrapper) mData).getVersion();
        //noinspection unchecked
        return observer.mObserver.onChanged((T) ((ObjectWrapper) mData).getData());
    }

    /**
     * 按序遍历，通知所有观察者，如果有观察者消费了事件，遍历会被中断
     *
     * @param initiator
     */
    private void dispatchingValue(@Nullable ObserverWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                for (Iterator<Map.Entry<LiveEventObserver<T>, ObserverWrapper>> iterator =
                     mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    if (considerNotify(iterator.next().getValue()) || mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }

    /**
     * Adds the given observer to the observers list within the lifespan of the given
     * owner. The events are dispatched on the main thread. If LiveData already has data
     * set, it will be delivered to the observer.
     * <p>
     * The observer will only receive events if the owner is in {@link Lifecycle.State#STARTED}
     * or {@link Lifecycle.State#RESUMED} state (active).
     * <p>
     * If the owner moves to the {@link Lifecycle.State#DESTROYED} state, the observer will
     * automatically be removed.
     * <p>
     * When data changes while the {@code owner} is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     * <p>
     * LiveData keeps a strong reference to the observer and the owner as long as the
     * given LifecycleOwner is not destroyed. When it is destroyed, LiveData removes references to
     * the observer &amp; the owner.
     * <p>
     * If the given owner is already in {@link Lifecycle.State#DESTROYED} state, LiveData
     * ignores the call.
     * <p>
     * If the given owner, observer tuple is already in the list, the call is ignored.
     * If the observer is already in the list with another owner, LiveData throws an
     * {@link IllegalArgumentException}.
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer) {
        observe(owner, observer, DEFAULT_PRIORITY);
    }

    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer, int priority) {
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        wrapper.mPriority = priority;
        wrapper.mLastVersion = getVersion();
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper, priority);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    @MainThread
    public void observeSticky(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer) {
        observeSticky(owner, observer, DEFAULT_PRIORITY);
    }

    @MainThread
    public void observeSticky(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer, int priority) {
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        wrapper.mPriority = priority;
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper, priority);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    /**
     * Adds the given observer to the observers list. This call is similar to
     * {@link LiveEvent#observe(LifecycleOwner, LiveEventObserver)} with a LifecycleOwner, which
     * is always active. This means that the given observer will receive all events and will never
     * be automatically removed. You should manually call {@link #removeObserver(LiveEventObserver)} to stop
     * observing this LiveData.
     * While LiveData has one of such observers, it will be considered
     * as active.
     * <p>
     * If the observer was already added with an owner to this LiveData, LiveData throws an
     * {@link IllegalArgumentException}.
     *
     * @param observer The observer that will receive the events
     */
    @MainThread
    public void observeForever(@NonNull LiveEventObserver<T> observer) {
        observeForever(observer, DEFAULT_PRIORITY);
    }

    @MainThread
    public void observeForever(@NonNull LiveEventObserver<T> observer, int priority) {
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        wrapper.mLastVersion = getVersion();
        wrapper.mPriority = priority;
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper, priority);
        if (existing != null && existing instanceof LiveEvent.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    @MainThread
    public void observeStickyForever(@NonNull LiveEventObserver<T> observer) {
        observeStickyForever(observer, DEFAULT_PRIORITY);
    }

    @MainThread
    public void observeStickyForever(@NonNull LiveEventObserver<T> observer, int priority) {
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        wrapper.mPriority = priority;
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper, priority);
        if (existing != null && existing instanceof LiveEvent.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    /**
     * Removes the given observer from the observers list.
     *
     * @param observer The Observer to receive events.
     */
    @MainThread
    public void removeObserver(@NonNull final LiveEventObserver<T> observer) {
        assertMainThread("removeObserver");
        ObserverWrapper removed = mObservers.remove(observer);
        if (removed == null) {
            return;
        }
        removed.detachObserver();
        removed.activeStateChanged(false);
    }

    /**
     * Removes all observers that are tied to the given {@link LifecycleOwner}.
     *
     * @param owner The {@code LifecycleOwner} scope for the observers to be removed.
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    public void removeObservers(@NonNull final LifecycleOwner owner) {
        assertMainThread("removeObservers");
        for (Map.Entry<LiveEventObserver<T>, ObserverWrapper> entry : mObservers) {
            if (entry.getValue().isAttachedTo(owner)) {
                removeObserver(entry.getKey());
            }
        }
    }

    /**
     * Posts a task to a main thread to set the given value. So if you have a following code
     * executed in the main thread:
     * <pre class="prettyprint">
     * liveData.postValue("a");
     * liveData.setValue("b");
     * </pre>
     * The value "b" would be set at first and later the main thread would override it with
     * the value "a".
     * <p>
     * If you called this method multiple times before a main thread executed a posted task, only
     * the last value would be dispatched.
     *
     * @param value The new value
     */
    public void postValue(T value) {
        MainThreadManager.getInstance().postToMainThread(new PostValueTask(value));
    }

    /**
     * Returns the current value.
     * Note that calling this method on a background thread does not guarantee that the latest
     * value set will be received.
     *
     * @return the current value
     */
    @Nullable
    public T getValue() {
        Object data = mData;
        if (data != NOT_SET) {
            //noinspection unchecked
            return (T) ((ObjectWrapper) mData).getData();
        }
        return null;
    }

    /**
     * Sets the value. If there are active observers, the value will be dispatched to them.
     * <p>
     * This method must be called from the main thread. If you need set a value from a background
     * thread, you can use {@link #postValue(Object)}
     *
     * @param value The new value
     */
    @MainThread
    public void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = new ObjectWrapper<>(value, mVersion);
        dispatchingValue(null);
    }

    /**
     * resume the last dispatching. If there are active observers, the value will be dispatched to them.
     * <p>
     * This method must be called from the main thread.
     *
     * @param value The older value
     */
    @MainThread
    public void resumeDispatch(T value) {
        assertMainThread("setValue");
        if (mData != NOT_SET && ((ObjectWrapper) mData).getData() == value) {
            dispatchingValue(null);
        }
    }

    int getVersion() {
        return mVersion;
    }

    /**
     * Called when the number of active observers change to 1 from 0.
     * <p>
     * This callback can be used to know that this LiveData is being used thus should be kept
     * up to date.
     */
    protected void onActive() {

    }

    /**
     * Called when the number of active observers change from 1 to 0.
     * <p>
     * This does not mean that there are no observers left, there may still be observers but their
     * lifecycle states aren't {@link Lifecycle.State#STARTED} or {@link Lifecycle.State#RESUMED}
     * (like an Activity in the back stack).
     * <p>
     * You can check if there are observers via {@link #hasObservers()}.
     */
    protected void onInactive() {

    }

    /**
     * Returns true if this LiveData has observers.
     *
     * @return true if this LiveData has observers
     */
    @SuppressWarnings("WeakerAccess")
    public boolean hasObservers() {
        return mObservers.size() > 0;
    }

    /**
     * Returns true if this LiveData has active observers.
     *
     * @return true if this LiveData has active observers
     */
    @SuppressWarnings("WeakerAccess")
    public boolean hasActiveObservers() {
        return mActiveCount > 0;
    }

    /**
     * determine when the observer is active, means the observer can receive message
     * the default value is CREATED, means if the observer's state is above create,
     * for example, the onCreate() of activity is called
     * you can change this value to CREATED/STARTED/RESUMED
     * determine on witch state, you can receive message
     *
     * @return
     */
    protected Lifecycle.State observerActiveLevel() {
        return CREATED;
    }

    private class PostValueTask implements Runnable {
        private Object newValue;

        public PostValueTask(@NonNull Object newValue) {
            this.newValue = newValue;
        }

        @Override
        public void run() {
            setValue((T) newValue);
        }
    }

    class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, LiveEventObserver<T> observer) {
            super(observer);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(observerActiveLevel());
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            activeStateChanged(shouldBeActive());
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ObserverWrapper {
        final LiveEventObserver<T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;
        int mPriority = DEFAULT_PRIORITY;

        ObserverWrapper(LiveEventObserver<T> observer) {
            mObserver = observer;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive;
            boolean wasInactive = LiveEvent.this.mActiveCount == 0;
            LiveEvent.this.mActiveCount += mActive ? 1 : -1;
            if (wasInactive && mActive) {
                onActive();
            }
            if (LiveEvent.this.mActiveCount == 0 && !mActive) {
                onInactive();
            }
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }

    private class AlwaysActiveObserver extends ObserverWrapper {

        AlwaysActiveObserver(LiveEventObserver<T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    private class ObjectWrapper<T> {
        private T mData;
        private int mVersion = START_VERSION;

        public ObjectWrapper() {
        }

        public ObjectWrapper(T mData, int mVersion) {
            this.mData = mData;
            this.mVersion = mVersion;
        }

        public T getData() {
            return mData;
        }

        public ObjectWrapper setData(T mData) {
            this.mData = mData;
            return this;
        }

        public int getVersion() {
            return mVersion;
        }

        public ObjectWrapper setVersion(int mLastVersion) {
            this.mVersion = mLastVersion;
            return this;
        }
    }
}
