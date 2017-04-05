package com.thibautmassard.android.masterdoer;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.today_item) LinearLayout todayItem;
    @BindView(R.id.week_item) LinearLayout weekItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        todayItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String today = "today";
                int orientation = getResources().getConfiguration().orientation;
                boolean isTablet = getResources().getBoolean(R.bool.isTablet);

                if (isTablet && orientation == ORIENTATION_LANDSCAPE) {
                    Bundle arguments = new Bundle();
                    arguments.putString(TaskListFragment.ARG_ITEM_TODAY, today);
                    TaskListFragment fragment = new TaskListFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.task_list_fragment, fragment).commit();
                    TaskListFragment.mTodayView = true;
                } else { // If we're in Portrait mode, open a new Detail Activity
                    Intent detailIntent = new Intent(MainActivity.this, TaskActivity.class);
                    detailIntent.putExtra(TaskListFragment.ARG_ITEM_TODAY, today);
                    startActivity(detailIntent);
                    TaskListFragment.mTodayView = true;
                }
            }
        });

        weekItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String week = "week";
                int orientation = getResources().getConfiguration().orientation;
                boolean isTablet = getResources().getBoolean(R.bool.isTablet);

                if (isTablet && orientation == ORIENTATION_LANDSCAPE) {
                    Bundle arguments = new Bundle();
                    arguments.putString(TaskListFragment.ARG_ITEM_WEEK, week);
                    TaskListFragment fragment = new TaskListFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.task_list_fragment, fragment).commit();
                    TaskListFragment.mWeekView = true;
                } else { // If we're in Portrait mode, open a new Detail Activity
                    Intent detailIntent = new Intent(MainActivity.this, TaskActivity.class);
                    detailIntent.putExtra(TaskListFragment.ARG_ITEM_WEEK, week);
                    startActivity(detailIntent);
                    TaskListFragment.mWeekView = true;
                }
            }
        });

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

    // Add a project
    void addProject(String projectName) {
        if (projectName != null && !projectName.isEmpty()) {

            ContentValues projectValue = new ContentValues();

            Calendar clCurrent = Calendar.getInstance();
            int currentDay = clCurrent.get(Calendar.DAY_OF_MONTH);
            int currentMonth = clCurrent.get(Calendar.MONTH)+1;
            int currentYear = clCurrent.get(Calendar.YEAR);
            String currentDate = Integer.toString(currentDay)
                    + "/" + Integer.toString(currentMonth) + "/" + Integer.toString(currentYear);

            int i = 1;
            String color = "test";

            projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_ID, i);
            projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_NAME, projectName);
            projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_DATE, currentDate);
            projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_COLOR, color);

            ArrayList<ContentValues> projectValuesContent = new ArrayList<ContentValues>();
            projectValuesContent.add(projectValue);

            getContentResolver().bulkInsert(
                    Contract.ProjectEntry.CONTENT_URI,
                    projectValuesContent.toArray(new ContentValues[1]));
        }
    }

    public void removeProject(String projectId) {
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = projectId;
        getContentResolver().delete(
                Contract.ProjectEntry.CONTENT_URI,
                Contract.ProjectEntry._ID + " = ?",
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
