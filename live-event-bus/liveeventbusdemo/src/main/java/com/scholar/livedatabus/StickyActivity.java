package com.scholar.livedatabus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.scholar.livedatabus.liveevent.LiveEventObserver;


public class StickyActivity extends FragmentActivity {
    private TextView tvSticky1, tvSticky2;

    private LiveEventObserver<String> observer = new LiveEventObserver<String>() {
        @Override
        public boolean onChanged(@Nullable String s) {
            tvSticky2.setText("observeStickyForever注册的观察者收到消息: " + s);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticky_demo);
        tvSticky1 = findViewById(R.id.tv_sticky1);
        tvSticky2 = findViewById(R.id.tv_sticky2);
        LiveEventBus.get()
                .with("sticky_key", String.class)
                .observeSticky(this, new LiveEventObserver<String>() {
                    @Override
                    public boolean onChanged(@Nullable String s) {
                        tvSticky1.setText("observeSticky注册的观察者收到消息: " + s);
                        return false;
                    }
                });
        LiveEventBus.get()
                .with("sticky_key", String.class)
                .observeStickyForever(observer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveEventBus.get()
                .with("sticky_key", String.class)
                .removeObserver(observer);
    }
}
