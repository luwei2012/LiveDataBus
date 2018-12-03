package com.scholar.livedatabus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.scholar.livedatabus.liveevent.LiveEventObserver;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.scholar.livedatabus.liveevent.LiveEvent.DEFAULT_PRIORITY;


public class LiveDataBusDemo extends FragmentActivity {

    private int sendCount = 0;
    private int receiveCount = 0;
    private LiveEventObserver<String> observer = new LiveEventObserver<String>() {
        @Override
        public boolean onChanged(@Nullable String s) {
            Toast.makeText(LiveDataBusDemo.this, s, Toast.LENGTH_SHORT).show();
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_data_bus_demo);
        //决定由第几个观察者消费事件
        final int flag = 3;
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LiveEventBus.get()
                    .with("key1", String.class)
                    .observe(this, new LiveEventObserver<String>() {
                        @Override
                        public boolean onChanged(@Nullable final String s) {
                            getWindow().getDecorView().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LiveDataBusDemo.this,
                                            "Receiver:" + index + " " + s, Toast.LENGTH_SHORT).show();
                                    if (flag == index) {
                                        //消费事件后再恢复事件
                                        getWindow().getDecorView().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                LiveEventBus.get()
                                                        .with("key1", String.class)
                                                        .resumeDispatch(s);
                                            }
                                        }, 1000 * index);
                                    }
                                }
                            }, 1000 * index);

                            return flag == index;
                        }
                        //把消费事件的观察者权重往前提，这样可以拦截所有观察者
                    }, DEFAULT_PRIORITY - (flag == index ? 1 : 0));
        }

        LiveEventBus.get()
                .with("key2", String.class)
                .observeForever(observer);
        LiveEventBus.get()
                .with("close_all_page", Boolean.class)
                .observe(this, new LiveEventObserver<Boolean>() {
                    @Override
                    public boolean onChanged(@Nullable Boolean b) {
                        if (b) {
                            finish();
                        }
                        return false;
                    }
                });
        LiveEventBus.get()
                .with("multi_thread_count", String.class)
                .observe(this, new LiveEventObserver<String>() {
                    @Override
                    public boolean onChanged(@Nullable String s) {
                        receiveCount++;
                        return false;
                    }
                });
        LiveEventBus.get()
                .with("key_active_level", String.class)
                .observe(this, new LiveEventObserver<String>() {
                    @Override
                    public boolean onChanged(@Nullable String s) {
                        Toast.makeText(LiveDataBusDemo.this, "Receive message: " + s,
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveEventBus.get()
                .with("key2", String.class)
                .removeObserver(observer);
    }

    public void sendMsgBySetValue(View v) {
        LiveEventBus.get().with("key1").setValue("Message By SetValue: "
                + new Random().nextInt(100));
    }

    public void sendMsgByPostValue(View v) {
        LiveEventBus.get().with("key1").postValue("Message By PostValue: "
                + new Random().nextInt(100));

    }

    public void sendMsgToForeverObserver(View v) {
        LiveEventBus.get().with("key2").setValue("Message To ForeverObserver: "
                + new Random().nextInt(100));
    }

    public void sendMsgToStickyReceiver(View v) {
        LiveEventBus.get().with("sticky_key").setValue("Message Sticky: "
                + new Random().nextInt(100));
    }

    public void startStickyActivity(View v) {
        startActivity(new Intent(this, StickyActivity.class));
    }

    public void startNewActivity(View v) {
        startActivity(new Intent(this, LiveDataBusDemo.class));
    }

    public void closeAll(View v) {
        LiveEventBus.get().with("close_all_page").setValue(true);
    }

    public void postValueCountTest(View v) {
        sendCount = 1000;
        receiveCount = 0;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
//        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        for (int i = 0; i < sendCount; i++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    LiveEventBus.get().with("multi_thread_count").postValue("test_data");
                }
            });
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LiveDataBusDemo.this, "sendCount: " + sendCount +
                        " | receiveCount: " + receiveCount, Toast.LENGTH_LONG).show();
            }
        }, 1000);
    }

    public void testObserverActiveLevel(View v) {
        startActivity(new Intent(this, ObserverActiveLevelActivity.class));
    }
}
