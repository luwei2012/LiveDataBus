package com.scholar.livedatabus;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.scholar.livedatabus.liveevent.LiveEvent;
import com.scholar.livedatabus.liveevent.LiveEventObserver;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hailiangliao on 2018/7/4.
 */

public final class LiveEventBus {

    private final Map<Object, BusLiveEvent<Object>> bus;

    private LiveEventBus() {
        bus = new HashMap<>();
    }

    public static LiveEventBus get() {
        return SingletonHolder.DEFAULT_BUS;
    }

    public synchronized <T> Observable<T> with(Object key, Class<T> type) {
        if (!bus.containsKey(key)) {
            bus.put(key, new BusLiveEvent<>());
        }
        return (Observable<T>) bus.get(key);
    }

    public Observable<Object> with(Object key) {
        return with(key, Object.class);
    }

    public interface Observable<T> {
        void setValue(T value);

        void postValue(T value);

        void observe(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer);

        void observe(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer, int priority);

        void observeSticky(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer);

        void observeSticky(@NonNull LifecycleOwner owner, @NonNull LiveEventObserver<T> observer, int priority);

        void observeForever(@NonNull LiveEventObserver<T> observer);

        void observeForever(@NonNull LiveEventObserver<T> observer, int priority);

        void observeStickyForever(@NonNull LiveEventObserver<T> observer);

        void observeStickyForever(@NonNull LiveEventObserver<T> observer, int priority);

        void removeObserver(@NonNull LiveEventObserver<T> observer);

        void resumeDispatch(T value);
    }

    private static class SingletonHolder {
        private static final LiveEventBus DEFAULT_BUS = new LiveEventBus();
    }

    private static class BusLiveEvent<T> extends LiveEvent<T> implements Observable<T> {
        @Override
        protected Lifecycle.State observerActiveLevel() {
            return super.observerActiveLevel();
//            return Lifecycle.State.STARTED;
        }


    }
}
