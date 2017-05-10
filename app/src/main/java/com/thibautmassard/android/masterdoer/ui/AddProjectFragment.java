package com.thibautmassard.android.masterdoer.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;
import com.thibautmassard.android.masterdoer.data.Project;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 05/04/2017.
 */

public class AddProjectFragment extends Fragment {

    // Get all the visual elements
    @BindView(R.id.add_project_name_edit_text) EditText addProjectNameEditText;
    @BindView(R.id.add_project_date_edit_text) EditText addProjectDateEditText;
    @BindView(R.id.project_color_green) ImageView projectColorGreen;
    @BindView(R.id.project_color_blue) ImageView projectColorBlue;
    @BindView(R.id.project_color_yellow) ImageView projectColorYellow;
    @BindView(R.id.project_color_red) ImageView projectColorRed;
    @BindView(R.id.add_project_button_cancel) Button addProjectButtonCancel;
    @BindView(R.id.add_project_button_add) Button addProjectButtonAdd;

    // Create the colors variables
    private boolean colorGreenSelected = false, colorBlueSelected = false, colorYellowSelected = false, colorRedSelected = false;
    private boolean[] colors = {colorGreenSelected, colorBlueSelected, colorYellowSelected, colorRedSelected};

    // Prepare the values passed through intents
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_NAME = "item_name";
    public static final String ARG_ITEM_COLOR = "item_color";
    public static final String ARG_ITEM_DATE = "item_date";
    public static final String ARG_ITEM_TASK_NUMBER = "item_task_number";
    public static final String ARG_ITEM_TASK_DONE = "item_task_done";

    public static final String ARG_MAX_PROJECT_ID = "max_project_id";

    private long projectDateMillis;

    private long mMaxProjectId;

    // Project object
    private Project mProject;

    private DatabaseReference mFirebaseDatabaseRef;

    // Global boolean to know if we're editing a project or creating a new one
    boolean mEditMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_add_project, container, false);
        ButterKnife.bind(this, view);

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        // Get the data from the intent
        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            mMaxProjectId = bundle.getLong(AddProjectFragment.ARG_MAX_PROJECT_ID);
            mProject = new Project(bundle.getString(AddProjectFragment.ARG_ITEM_ID),
                                    bundle.getString(AddProjectFragment.ARG_ITEM_NAME),
                                    bundle.getString(AddProjectFragment.ARG_ITEM_DATE),
                                    bundle.getString(AddProjectFragment.ARG_ITEM_COLOR),
                                    bundle.getString(AddProjectFragment.ARG_ITEM_TASK_NUMBER),
                                    bundle.getString(AddProjectFragment.ARG_ITEM_TASK_DONE));
        } else if (intentThatStartedThatActivity != null) { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            mMaxProjectId = intentThatStartedThatActivity.getLongExtra(AddProjectFragment.ARG_MAX_PROJECT_ID, 0);
            mProject = new Project(intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_ID),
                                    intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_NAME),
                                    intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_DATE),
                                    intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_COLOR),
                                    intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_TASK_NUMBER),
                                    intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_TASK_DONE));
        }

        // Check if we're editing the project or not
        if (mProject.id != null) {
            mEditMode = true;
        }

        // Get the current date and format it
        Calendar clCurrent = Calendar.getInstance();
        final String currentDate = DateFormatter.formatDate(clCurrent);

        if (mEditMode) { // If we're editing a project
            addProjectNameEditText.setText(mProject.name);
            addProjectDateEditText.setText(DateFormatter.formatDate(mProject.date));
            addProjectButtonAdd.setText(getActivity().getResources().getString(R.string.add_project_update_button));

            int colorIndex = selectSavedColor(mProject.color);
            colors = selectColor(colorIndex, colors);
        } else { // If we're creating a new project
            addProjectDateEditText.setText(currentDate);

            colors[0] = true;
            projectColorGreen.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_crop_square_black_24dp));
        }

        // Green color selector
        projectColorGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!colorGreenSelected) {
                    highlightColor(projectColorGreen, projectColorBlue, projectColorYellow, projectColorRed);
                    colors = selectColor(0, colors);
                }
            }
        });

        // Blue color selector
        projectColorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!colorBlueSelected) {
                    highlightColor(projectColorBlue, projectColorGreen, projectColorYellow, projectColorRed);
                    colors = selectColor(1, colors);
                }
            }
        });

        // Yellow color selector
        projectColorYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!colorYellowSelected) {
                    highlightColor(projectColorYellow, projectColorBlue, projectColorGreen, projectColorRed);
                    colors = selectColor(2, colors);
                }
            }
        });

        // Red color selector
        projectColorRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!colorRedSelected) {
                    highlightColor(projectColorRed, projectColorBlue, projectColorYellow, projectColorGreen);
                    colors = selectColor(3, colors);
                }
            }
        });

        // Cancel button
        addProjectButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the device's current orientation
                int orientation = getResources().getConfiguration().orientation;

                // If we're in Landscape Mode, display the project list fragment again
                if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
                    Bundle arguments = new Bundle();
                    TaskListFragment fragment = new TaskListFragment();
                    fragment.setArguments(arguments);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.project_list_fragment, fragment).commit();
                } else {
                    getActivity().finish();
                }
            }
        });

        // Add/Update button
        addProjectButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String projectName = addProjectNameEditText.getText().toString();

                if (!projectName.isEmpty()) { // If the project's name isn't empty, add or update the project
                    addOrUpdateProject(projectName, colors);
                } else { // If the project's name is empty, inform the user with a Toast
                    Toast.makeText(getActivity(), R.string.add_project_empty_name, Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    // Highlight the selected color by adding a square around the colored bullet
    private void highlightColor(ImageView selectedColor, ImageView otherColor1, ImageView otherColor2, ImageView otherColor3) {
        selectedColor.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_black_24dp, null));
        otherColor1.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_transparent_24dp, null));
        otherColor2.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_transparent_24dp, null));
        otherColor3.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_transparent_24dp, null));
    }

    // Update the colors array when a new color is selected
    private boolean[] selectColor(int colorIndex, boolean[] colors) {
        for (int j = 0; j < colors.length; j++) { // Set all the array values to false
            colors[j] = false;
        }
        colors[colorIndex] = true; // Set the value selected to true
        return colors;
    }

    // Select the color saved when we edit an already existing project
    private int selectSavedColor(String color) {
        int colorIndex = 0;
        switch(color) {
            case "green":
                colorIndex = 0;
                projectColorGreen.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_black_24dp, null));
                break;
            case "blue":
                colorIndex = 1;
                projectColorBlue.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_black_24dp, null));
                break;
            case "yellow":
                colorIndex = 2;
                projectColorYellow.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_black_24dp, null));
                break;
            case "red":
                colorIndex = 3;
                projectColorRed.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_crop_square_black_24dp, null));
                break;
        }

        return colorIndex;
    }

    /**
     * Add or Update a project. This function adds the data and closes the activity
     * @param projectName the name of the project we're adding or updating
     * @param colors the color array with the color selected
     */
    private void addOrUpdateProject(String projectName, boolean[] colors) {

        String color = "green";

        if (colors[0]) {
            color = "green";
        } else if (colors[1]) {
            color = "blue";
        } else if (colors[2]) {
            color = "yellow";
        } else if (colors[3]) {
            color = "red";
        }

        String projectDate = addProjectDateEditText.getText().toString();
        long projectDateMillis = DateFormatter.dateToMillis(projectDate);
        projectDate = String.valueOf(projectDateMillis);

        String taskNumber;
        String taskDone;
        if (mProject.taskNumber != null) {
            taskNumber = mProject.taskNumber;
            taskDone = mProject.taskDone;
        } else {
            taskNumber = "0";
            taskDone = "0";
        }

        // Create a ContentValues to insert or update the project
        ContentValues projectValue = new ContentValues();
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_NAME, projectName);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_DATE, projectDate);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_COLOR, color);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_TASK_NUMBER, taskNumber);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_TASK_DONE, taskDone);

        //ArrayList<ContentValues> projectValuesContent = new ArrayList<ContentValues>();
        //projectValuesContent.add(projectValue);

        Uri projectUri = null;

//        if (mEditMode) { // If we're editing a project, update it
//            String[] mSelectionArgs = {""};
//            mSelectionArgs[0] = mProject.id; // The project we're updating
//
//            getActivity().getContentResolver().update(
//                    Contract.ProjectEntry.CONTENT_URI,
//                    projectValue,
//                    Contract.ProjectEntry._ID + "=?",
//                    mSelectionArgs);
//        } else { // If we're creating a new project, insert a new item
//            projectUri = getActivity().getContentResolver().insert(
//                    Contract.ProjectEntry.CONTENT_URI,
//                    projectValue);
//        }

        String projectId;
        if (mEditMode) {
            projectId = mProject.id;
        } else {
            projectId = String.valueOf(mMaxProjectId + 1);
        }

        Project project = new Project(projectId, projectName, projectDate, color, taskNumber, taskDone);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(projectId).setValue(project);
            if (!mEditMode) {
                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxProjectId").setValue(mMaxProjectId + 1);
            }
        }

        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Landscape Mode, refresh the Detail Fragment with the new data
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.project_list_fragment, fragment).commit();
        } else {
            getActivity().finish();
        }
    }

    /**
     * This method will make the View for the project data visible and
     * hide the error message.
     */
    private void showAddProjectView() {

    }

    /**
     * This method will make the error messages visible and hide the project data
     */
    private void showErrorMessage() {
    }
}
