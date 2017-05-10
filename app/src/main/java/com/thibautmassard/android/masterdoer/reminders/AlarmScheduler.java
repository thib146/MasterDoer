package com.thibautmassard.android.masterdoer.reminders;

/**
 * Created by thib146 on 13/04/2017.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

/**
 * Helper to manage scheduling the reminder alarm
 */
public class AlarmScheduler {

    /**
     * Schedule a reminder alarm at the specified time for the given task.
     *
     * @param context Local application or activity context
     * @param alarmTime Alarm start time
     * @param taskData task ID + task project ID
     */
    public static void scheduleAlarm(Context context, long alarmTime, ArrayList<String> taskData) {
        //Schedule the alarm. Will update an existing item for the same task.
        AlarmManager manager = AlarmManagerProvider.getAlarmManager(context);

        PendingIntent operation =
                ReminderAlarmService.getReminderPendingIntent(context, taskData);

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            manager.setExact(AlarmManager.RTC, alarmTime, operation);
        } else {
            manager.set(AlarmManager.RTC, alarmTime, operation);
        }
        Log.d ("setReminder", String.valueOf(alarmTime));
    }
}
