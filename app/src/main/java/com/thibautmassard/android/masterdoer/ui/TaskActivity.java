package com.thibautmassard.android.masterdoer.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.thibautmassard.android.masterdoer.R;

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

        // Set the activity title with the Project Name
        if (mProjectName != null) {
            getSupportActionBar().setTitle(mProjectName);
        }

        // Get the device's orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're on a Tablet and we rotate the device from landscape to portrait, close this activity
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            finish();
        }
    }
}
