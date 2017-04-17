package com.thibautmassard.android.masterdoer.ui;

import android.content.ContentValues;
import android.content.Intent;
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

import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 05/04/2017.
 */

public class AddProjectFragment extends Fragment {

    // Get all the elements
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

    // Project data
    private String mProjectName;
    private String mProjectId;
    private String mProjectColor;
    private String mProjectDate;

    // Global boolean to know if we're editing a project or creating a new one
    boolean mEditMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_add_project, container, false);
        ButterKnife.bind(this, view);

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        // Get the data from the intent
        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            mProjectId = bundle.getString(AddProjectFragment.ARG_ITEM_ID);
            mProjectName = bundle.getString(AddProjectFragment.ARG_ITEM_NAME);
            mProjectColor = bundle.getString(AddProjectFragment.ARG_ITEM_COLOR);
            mProjectDate = bundle.getString(AddProjectFragment.ARG_ITEM_DATE);
        } else if (intentThatStartedThatActivity != null) { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            mProjectId = intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_ID);
            mProjectName = intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_NAME);
            mProjectColor = intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_COLOR);
            mProjectDate = intentThatStartedThatActivity.getStringExtra(AddProjectFragment.ARG_ITEM_DATE);
        }

        // Check if we're editing the project or not
        if (mProjectId != null) {
            mEditMode = true;
        }

        // Get the current date and format it
        Calendar clCurrent = Calendar.getInstance();
        final String currentDate = DateFormatter.formatDate(clCurrent);

        if (mEditMode) { // If we're editing a project
            addProjectNameEditText.setText(mProjectName);
            addProjectDateEditText.setText(mProjectDate);
            addProjectButtonAdd.setText(getActivity().getResources().getString(R.string.add_project_update_button));

            int colorIndex = selectSavedColor(mProjectColor);
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

        int i = 1;
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

        // Create a ContentValues to insert or update the project
        ContentValues projectValue = new ContentValues();
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_ID, i);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_NAME, projectName);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_DATE, projectDate);
        projectValue.put(Contract.ProjectEntry.COLUMN_PROJECT_COLOR, color);

        ArrayList<ContentValues> projectValuesContent = new ArrayList<ContentValues>();
        projectValuesContent.add(projectValue);

        if (mEditMode) { // If we're editing a project, update it
            String[] mSelectionArgs = {""};
            mSelectionArgs[0] = mProjectId; // The project we're updating

            getActivity().getContentResolver().update(
                    Contract.ProjectEntry.CONTENT_URI,
                    projectValue,
                    Contract.ProjectEntry._ID + "=?",
                    mSelectionArgs);
        } else { // If we're creating a new project, insert a new item
            getActivity().getContentResolver().bulkInsert(
                    Contract.ProjectEntry.CONTENT_URI,
                    projectValuesContent.toArray(new ContentValues[1]));
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
