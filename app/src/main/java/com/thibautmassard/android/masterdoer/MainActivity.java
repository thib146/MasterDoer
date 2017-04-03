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
import android.widget.Toast;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    // Handle the "Add Project" button
    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddProjectDialog().show(this.getFragmentManager(), "ProjectDialogFragment");
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
