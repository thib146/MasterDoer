package com.thibautmassard.android.masterdoer.reminders;

/**
 * Created by thib146 on 13/04/2017.
 */

import android.app.AlarmManager;
import android.content.Context;

/**
 * Interface to provide access to an {@link AlarmManager} instance that can be configured
 * during automated unit tests.
 *
 */
public class AlarmManagerProvider {
    private static final String TAG = AlarmManagerProvider.class.getSimpleName();
    private static AlarmManager sAlarmManager;
    public static synchronized void injectAlarmManager(AlarmManager alarmManager) {
        if (sAlarmManager != null) {
            throw new IllegalStateException("Alarm Manager Already Set");
        }
        sAlarmManager = alarmManager;
    }
    /*package*/ static synchronized AlarmManager getAlarmManager(Context context) {
        if (sAlarmManager == null) {
            sAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        return sAlarmManager;
    }
}