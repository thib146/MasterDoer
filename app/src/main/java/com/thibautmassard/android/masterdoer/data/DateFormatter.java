package com.thibautmassard.android.masterdoer.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    public static String formatDate(String dateStr) {
        long date;
        if (dateStr.equals("")) {
            date = 0;
        } else {
            date = Long.valueOf(dateStr);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);

        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        return Integer.toString(currentDay) + "/" + Integer.toString(currentMonth) + "/" + Integer.toString(currentYear);
    }

    public static long dateToMillis(String dateStr) {
        Date date = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            date = sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static boolean isTaskDateToday(String taskDate, long currentDate) {
        if (taskDate.equals("")) {
            return false;
        }
        long taskDateLg = Long.valueOf(taskDate);
        long difference = taskDateLg - currentDate;
        return (difference < 86400000 && difference >= 0);
    }

    public static boolean isTaskDateInWeek(String taskDate, long currentDate) {
        if (taskDate.equals("")) {
            return false;
        }
        long taskDateLg = Long.valueOf(taskDate);
        long difference = taskDateLg - currentDate;
        return (difference < 604800000 && difference >= 0);
    }
}