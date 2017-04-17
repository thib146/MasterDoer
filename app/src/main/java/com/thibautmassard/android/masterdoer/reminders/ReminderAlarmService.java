package com.thibautmassard.android.masterdoer.reminders;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.ui.TaskActivity;
import com.thibautmassard.android.masterdoer.ui.TaskListFragment;
import com.thibautmassard.android.masterdoer.data.Contract;

import static com.thibautmassard.android.masterdoer.ui.ProjectListFragment.MAIN_PROJECTS_PROJECTION;
import static com.thibautmassard.android.masterdoer.ui.TaskListFragment.MAIN_TASKS_PROJECTION;

/**
 * Created by thib146 on 13/04/2017.
 */

public class ReminderAlarmService extends IntentService {
    private static final String TAG = ReminderAlarmService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 42;

    //This is a deep link intent, and needs the task stack
    public static PendingIntent getReminderPendingIntent(Context context, Uri uri) {
        Intent action = new Intent(context, ReminderAlarmService.class);
        action.setData(uri);
        return PendingIntent.getService(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public ReminderAlarmService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Uri uri = intent.getData();

        //Display a notification to view the task details
        //TODO : add fragment possibility for the Two-Pane Mode
        Intent action = new Intent(this, TaskActivity.class); // Intent directing to the Detail Activity

        // Get the task's project name and ID in order to
        // launch the project's task list when clicking on the notification
        String[] projectData = getProjectData(uri);

        action.putExtra(TaskListFragment.ARG_ITEM_ID, projectData[0]);
        action.putExtra(TaskListFragment.ARG_ITEM_NAME, projectData[1]);
        action.putExtra(TaskListFragment.ARG_ITEM_POSITION, "1");

        // Create the Pending Intent
        PendingIntent operation = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(action)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Grab the task description
        String description = getTaskDescription(uri);

        // Create the notification
        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.reminder_title))
                .setContentText(description)
                .setSmallIcon(R.mipmap.icon)
                .setContentIntent(operation)
                .setAutoCancel(true)
                .build();

        // Set the notification
        manager.notify(NOTIFICATION_ID, note);
    }

    private String[] getProjectData(Uri uri) {

        String projectId = "", projectName = "";

        // Get the task we want a reminder of
        Cursor taskCursor = getContentResolver().query(
                uri,
                MAIN_TASKS_PROJECTION,
                null,
                null,
                null);

        // Get the task's project ID
        if (taskCursor != null && taskCursor.moveToFirst()) {
            projectId = taskCursor.getString(Contract.TaskEntry.POSITION_TASK_PROJECT_ID);
            taskCursor.close();
        }

        // Get the task's project Name
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = projectId;
        Cursor projectCursor = getContentResolver().query(
                Contract.ProjectEntry.CONTENT_URI,
                MAIN_PROJECTS_PROJECTION,
                Contract.ProjectEntry._ID + "=?",
                mSelectionArgs,
                null);

        if (projectCursor != null && projectCursor.moveToFirst()) {
            projectName = projectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME);
            projectCursor.close();
        }

        // Return the data acquired
        String[] projectData = {projectId, projectName};
        return projectData;
    }

    private String getTaskDescription(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        String description = "";
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndex(Contract.TaskEntry.COLUMN_TASK_NAME);
                description = cursor.getString(colIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return description;
    }
}