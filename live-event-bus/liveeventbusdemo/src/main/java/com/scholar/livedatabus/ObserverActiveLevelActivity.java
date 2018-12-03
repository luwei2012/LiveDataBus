package com.scholar.livedatabus;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

public class ObserverActiveLevelActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observer_active_level_demo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void sendMsgToPrevent(View v) {
        LiveEventBus.get().with("key_active_level").postValue("Send Msg To Prevent");
    }
}
