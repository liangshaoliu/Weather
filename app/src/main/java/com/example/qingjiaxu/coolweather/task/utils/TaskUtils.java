package com.example.qingjiaxu.coolweather.task.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.example.qingjiaxu.coolweather.WeatherApplication;
import com.example.qingjiaxu.coolweather.task.receiver.TimingTaskReceiver;

public class TaskUtils {
    public static final String TAG = WeatherApplication.TAG;
    /**
     * 计划周期，一周
     */
    public static final long ONE_WEEK = 604800000;
    /**
     * 计划周期，一天
     */
    public static final long ONE_DAY = 86400000;
    /**
     * 计划周期，一小时
     */
    public static final long ONE_HOUR = 1000 * 60 * 60;
    /**
     * 3小时
     */
    public static final long THREE_HOUR = 1000 * 60 * 60 * 3;
    public static final long FIVE_HOUR = 1000 * 60 * 60 * 5;
    /**
     * 任务头
     */
    private static final int TASK_HEAD = 2015000;
    private Context context;
    private Intent intent;

    {
        intent = new Intent(context, TimingTaskReceiver.class);
    }

    public TaskUtils(Context context) {
        this.context = context;
    }

    /**
     * 开始重复延时计划（不精准有延时）
     *
     * @param what      任务编号{@link com.example.qingjiaxu.coolweather.EventId}, 相同编号的任务会被覆盖
     * @param delayTime 延迟时间，不延时传0(单位毫秒)
     * @param interval  重复执行计划的时间间隔(单位毫秒)
     */
    public void startRepeatDelayPlan(int what, long delayTime, long interval) {
        try {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            intent.putExtra("what", what);
            PendingIntent pendIntent = PendingIntent.getBroadcast(context, getReqCode(what), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // 重复发送,以开机时间为基点，triggerAtTime发送广播，然后每个interval秒重复发送计划
            long triggerAtTime = (SystemClock.elapsedRealtime() + delayTime);
            alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, interval, pendIntent);
        } catch (Exception e) {
            Log.e(TAG, "repeat delay error");
        }
    }

    /**
     * 以周为单位进行循环计划任务（不精准有延时）
     *
     * @param what          任务编号{@link com.example.qingjiaxu.coolweather.EventId}, 相同编号的任务会被覆盖
     * @param triggerAtTime 触发时间
     */
    public void startSheduleWeekday(int what, long triggerAtTime) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            intent.putExtra("what", what);
            PendingIntent sender = PendingIntent.getBroadcast(context, getReqCode(what), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, ONE_WEEK, sender);
        } catch (Exception e) {
            Log.e(TAG, "shedule week error", e);
        }
    }

    /**
     * 取消计划任务
     *
     * @param what 计划类型,用来区分不同的定时编码（同一个intent可以被使用，根据code进行区分是一个定时，还是多个定时任务，code相同可以被覆盖）
     */
    public void cancelShedule(int what) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            intent.putExtra("what", what);
            PendingIntent sender = PendingIntent.getBroadcast(context, getReqCode(what), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(sender);
        } catch (Exception e) {
            Log.e(TAG, "cancel shedule error");
        }
    }

    private int getReqCode(int requestCode) {
        return (TASK_HEAD + requestCode);
    }
}
