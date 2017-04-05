package com.thibautmassard.android.masterdoer;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Calendar;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 31/03/2017.
 */

public class TaskActivity extends AppCompatActivity {

    public static String mProjectName;
    public static String mProjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        getSupportActionBar().setTitle(mProjectName);

        // Get the device's orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're on a Tablet and we rotate the device from landscape to portrait, close this activity
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            finish();
        }

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add_project);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    // Add a task
    void addTask(String taskName) {
        if (taskName != null && !taskName.isEmpty()) {

            ContentValues taskValue = new ContentValues();

            Calendar clCurrent = Calendar.getInstance();
            int currentDay = clCurrent.get(Calendar.DAY_OF_MONTH);
            int currentMonth = clCurrent.get(Calendar.MONTH)+1;
            int currentYear = clCurrent.get(Calendar.YEAR);
            String currentDate = Integer.toString(currentDay)
                    + "/" + Integer.toString(currentMonth) + "/" + Integer.toString(currentYear);

            int i = 2;

            taskValue.put(Contract.TaskEntry.COLUMN_TASK_ID, i);
            taskValue.put(Contract.TaskEntry.COLUMN_TASK_PROJECT_ID, mProjectId);
            taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, "0");
            taskValue.put(Contract.TaskEntry.COLUMN_TASK_PRIORITY, 0);
            taskValue.put(Contract.TaskEntry.COLUMN_TASK_NAME, taskName);

            taskValue.put(Contract.TaskEntry.COLUMN_TASK_DATE, currentDate);

            //projectValue.put(Contract.TaskEntry.COLUMN_TASK_PRIORITY, color);

            ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
            taskValuesContent.add(taskValue);

            getContentResolver().bulkInsert(
                    Contract.TaskEntry.CONTENT_URI,
                    taskValuesContent.toArray(new ContentValues[1]));
        }
    }

    public void updateTaskStatus(String taskId, int taskStatus) {

    }

    public void removeTask(String taskId) {
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;
        getContentResolver().delete(
                Contract.TaskEntry.CONTENT_URI,
                Contract.TaskEntry._ID + " = ?",
                mSelectionArgs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
