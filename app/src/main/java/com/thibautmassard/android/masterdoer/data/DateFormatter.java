package com.thibautmassard.android.masterdoer.data;

import java.util.Calendar;

/**
 * Created by thib146 on 16/04/2017.
 */

public class DateFormatter {

    // Set the date in a user readable format
    public static String formatDate(Calendar calendar) {
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        return Integer.toString(currentDay) + "/" + Integer.toString(currentMonth) + "/" + Integer.toString(currentYear);
    }
}
