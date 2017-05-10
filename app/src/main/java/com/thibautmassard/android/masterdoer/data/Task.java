package com.thibautmassard.android.masterdoer.data;

import android.database.Cursor;

/**
 * Created by thib146 on 26/04/2017.
 */

public class Task {

    public static final long NO_ID = -1;

    public final String id;
    public final String projectId;
    public final String name;
    public final String date;
    public final String status;
    public final String priority;
    public final String reminderDate;

    public Task() {
        this.id = "";
        this.projectId = "";
        this.name = "";
        this.date = "";
        this.status = "";
        this.priority = "";
        this.reminderDate = "";
    }

    // Classic constructor if needed
    public Task(String id, String projectId, String name, String date, String status, String priority, String reminderDate) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.date = date;
        this.status = status;
        this.priority = priority;
        this.reminderDate = reminderDate;
    }

    // Constructor with a Cursor
    public Task(Cursor cursor) {
        this.id = cursor.getString(Contract.TaskEntry.POSITION_ID);
        this.projectId = cursor.getString(Contract.TaskEntry.POSITION_TASK_PROJECT_ID);
        this.name = cursor.getString(Contract.TaskEntry.POSITION_TASK_NAME);
        this.date = cursor.getString(Contract.TaskEntry.POSITION_TASK_DATE);
        this.status = cursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
        this.priority = cursor.getString(Contract.TaskEntry.POSITION_TASK_PRIORITY);
        this.reminderDate = cursor.getString(Contract.TaskEntry.POSITION_TASK_REMINDER_DATE);
    }
}
