package com.thibautmassard.android.masterdoer.data;

import android.database.Cursor;

/**
 * Created by thib146 on 26/04/2017.
 */

public class Project {

    public final String id;
    public final String name;
    public final String date;
    public final String color;
    public final String taskNumber;
    public final String taskDone;

    public Project() {
        this.id = "";
        this.name = "";
        this.date = "";
        this.color = "";
        this.taskNumber = "";
        this.taskDone = "";
    }

    // Classic constructor if needed
    public Project(String id, String name, String date, String color, String taskNumber, String taskDone) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.color = color;
        this.taskNumber = taskNumber;
        this.taskDone = taskDone;
    }

    // Constructor with a Cursor
    public Project(Cursor cursor) {
        this.id = cursor.getString(Contract.ProjectEntry.POSITION_ID);
        this.name = cursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME);
        this.date = cursor.getString(Contract.ProjectEntry.POSITION_PROJECT_DATE);
        this.color = cursor.getString(Contract.ProjectEntry.POSITION_PROJECT_COLOR);
        this.taskNumber = cursor.getString(Contract.ProjectEntry.POSITION_PROJECT_TASK_NUMBER);
        this.taskDone = cursor.getString(Contract.ProjectEntry.POSITION_PROJECT_TASK_DONE);
    }
}