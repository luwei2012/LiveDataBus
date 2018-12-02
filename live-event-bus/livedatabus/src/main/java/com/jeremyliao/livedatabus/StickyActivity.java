package com.jeremyliao.livedatabus;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.jeremyliao.livedatabus.databinding.ActivityStickyDemoBinding;
import com.jeremyliao.livedatabus.liveevent.LiveEventObserver;

public class StickyActivity extends AppCompatActivity {

    private ActivityStickyDemoBinding binding;
    private LiveEventObserver<String> observer = new LiveEventObserver<String>() {
        @Override
        public boolean onChanged(@Nullable String s) {
            binding.tvSticky2.setText("observeStickyForever注册的观察者收到消息: " + s);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sticky_demo);
        binding.setLifecycleOwner(this);
        LiveEventBus.get()
                .with("sticky_key", String.class)
                .observeSticky(this, new LiveEventObserver<String>() {
                    @Override
                    public boolean onChanged(@Nullable String s) {
                        binding.tvSticky1.setText("observeSticky注册的观察者收到消息: " + s);
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
