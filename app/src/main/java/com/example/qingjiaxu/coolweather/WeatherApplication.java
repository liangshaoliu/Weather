package com.example.qingjiaxu.coolweather;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.litepal.LitePalApplication;

public class WeatherApplication extends LitePalApplication {
    public static final String TAG = "Weather";

    public static final String EVENT_DATA = "data";
    private static Handler weatherHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case EventId.EVENT_0X001:
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        weatherHandler = null;
    }

    public static void sendMessage(int eventId) {
        sendMessage(eventId, null);
    }

    public static boolean sendMessage(int eventId, String data) {
        if (weatherHandler == null) {
            return false;
        }
        Message message = weatherHandler.obtainMessage(eventId);
        Bundle bundle = new Bundle();
        bundle.putString(EVENT_DATA, data);
        message.setData(bundle);
        message.sendToTarget();
        return true;
    }
}
