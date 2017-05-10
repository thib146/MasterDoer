package com.thibautmassard.android.masterdoer.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by thib146 on 29/03/2017.
 */

public final class Contract {

    static final String AUTHORITY = "com.thibautmassard.android.masterdoer";
    static final String PATH_PROJECT = "project";
    static final String PATH_PROJECT_WITH_ID = "project/*";
    static final String PATH_TASK = "task";
    static final String PATH_TASK_WITH_ID = "task/*";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final class ProjectEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PROJECT).build();

        public static final String TABLE_NAME = "projects";
        public static final String COLUMN_PROJECT_NAME = "project_name";
        public static final String COLUMN_PROJECT_DATE = "project_date";
        public static final String COLUMN_PROJECT_COLOR = "project_color";
        public static final String COLUMN_PROJECT_TASK_NUMBER = "project_task_number";
        public static final String COLUMN_PROJECT_TASK_DONE = "project_task_done";
        public static final int POSITION_ID = 0;
        public static final int POSITION_PROJECT_NAME = 1;
        public static final int POSITION_PROJECT_DATE = 2;
        public static final int POSITION_PROJECT_COLOR = 3;
        public static final int POSITION_PROJECT_TASK_NUMBER = 4;
        public static final int POSITION_PROJECT_TASK_DONE = 5;

        public static final String[] COLUMNS =
                {_ID, COLUMN_PROJECT_NAME, COLUMN_PROJECT_DATE, COLUMN_PROJECT_COLOR, COLUMN_PROJECT_TASK_NUMBER, COLUMN_PROJECT_TASK_DONE};

        public static Uri buildProjectUriWithId(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(id))
                    .build();
        }
    }

    public static final class TaskEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TASK).build();

        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TASK_PROJECT_ID = "task_project_id";
        public static final String COLUMN_TASK_NAME = "task_name";
        public static final String COLUMN_TASK_DATE = "task_date";
        public static final String COLUMN_TASK_STATUS = "task_status";
        public static final String COLUMN_TASK_PRIORITY = "task_priority";
        public static final String COLUMN_TASK_REMINDER_DATE = "task_reminder_date";
        public static final int POSITION_ID = 0;
        public static final int POSITION_TASK_PROJECT_ID = 1;
        public static final int POSITION_TASK_NAME = 2;
        public static final int POSITION_TASK_DATE = 3;
        public static final int POSITION_TASK_STATUS = 4;
        public static final int POSITION_TASK_PRIORITY = 5;
        public static final int POSITION_TASK_REMINDER_DATE = 6;

        public static final String[] COLUMNS =
                {_ID, COLUMN_TASK_PROJECT_ID, COLUMN_TASK_NAME, COLUMN_TASK_DATE,
                        COLUMN_TASK_STATUS, COLUMN_TASK_PRIORITY, COLUMN_TASK_REMINDER_DATE};

        public static Uri buildTaskUriWithId(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(id))
                    .build();
        }
    }
}