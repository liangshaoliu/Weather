package com.example.qingjiaxu.coolweather.task.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.qingjiaxu.coolweather.WeatherApplication;

public class TimingTaskReceiver extends BroadcastReceiver {
    public static final String TAG = WeatherApplication.TAG;

    @Override
    public void onReceive(Context context, Intent intent) {
        int what = intent.getIntExtra("what", 0);
        Log.d(TAG, String.format("======== Begin task, what = %d ========", what));
        WeatherApplication.sendMessage(what);
    }

}
