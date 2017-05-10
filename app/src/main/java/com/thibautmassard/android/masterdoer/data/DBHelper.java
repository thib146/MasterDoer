package com.thibautmassard.android.masterdoer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.thibautmassard.android.masterdoer.data.Contract.ProjectEntry;
import com.thibautmassard.android.masterdoer.data.Contract.TaskEntry;

/**
 * Created by thib146 on 29/03/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "masterdoer.db";

    private static final int DATABASE_VERSION = 12;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_PROJECTS_TABLE =
                "CREATE TABLE " + ProjectEntry.TABLE_NAME + " (" +
                        ProjectEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        ProjectEntry.COLUMN_PROJECT_NAME + " STRING NOT NULL," +
                        ProjectEntry.COLUMN_PROJECT_DATE + " DATETIME NOT NULL," +
                        ProjectEntry.COLUMN_PROJECT_COLOR + " STRING," +
                        ProjectEntry.COLUMN_PROJECT_TASK_NUMBER + " STRING," +
                        ProjectEntry.COLUMN_PROJECT_TASK_DONE + " STRING);";

        sqLiteDatabase.execSQL(SQL_CREATE_PROJECTS_TABLE);

        final String SQL_CREATE_TASKS_TABLE =
                "CREATE TABLE " + TaskEntry.TABLE_NAME + " (" +
                        TaskEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        TaskEntry.COLUMN_TASK_PROJECT_ID + " INTEGER NOT NULL," +
                        TaskEntry.COLUMN_TASK_NAME + " STRING NOT NULL," +
                        TaskEntry.COLUMN_TASK_DATE + " DATETIME," +
                        TaskEntry.COLUMN_TASK_STATUS + " STRING NOT NULL," +
                        TaskEntry.COLUMN_TASK_PRIORITY + " INTEGER NOT NULL," +
                        TaskEntry.COLUMN_TASK_REMINDER_DATE + " STRING);";

        sqLiteDatabase.execSQL(SQL_CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ProjectEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TaskEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}