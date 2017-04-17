package com.thibautmassard.android.masterdoer.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;
import com.thibautmassard.android.masterdoer.data.ProjectAdapter;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 29/03/2017.
 */

public class ProjectListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        ProjectAdapter.ProjectAdapterOnClickHandler {

    @BindView(R.id.recycler_view) RecyclerView projectRecyclerView;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.error_message) TextView errorMessage;
    @BindView(R.id.fab_add_project) FloatingActionButton fabAddProject;
    @BindView(R.id.today_number_tasks) TextView todayNumberTasks;
    @BindView(R.id.week_number_tasks) TextView weekNumberTasks;

    private ProjectAdapter projectAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private Cursor mProjectCursor;
    private Cursor mTaskCursorToday;
    private Cursor mTaskCursorWeek;

    private int mTaskNumberToday; // Number of tasks for the Today indicator
    private int mTaskNumberWeek; // Number of tasks for the Weekly indicator

    // Array list used to send to a Task List via an intent, in order to get the project list when adding a new Task
    private ArrayList<String> projectList = new ArrayList<String>();

    private Activity mActivity;

    public static final int ID_PROJECTS_LOADER = 146;
    public static final int ID_TASKS_TODAY_LOADER = 148;
    public static final int ID_TASKS_WEEK_LOADER = 149;

    private String mProjectName;

    /*
     * The columns of data that we are interested in displaying within our Project List Fragment
     */
    public static final String[] MAIN_PROJECTS_PROJECTION = {
            Contract.ProjectEntry._ID,
            Contract.ProjectEntry.COLUMN_PROJECT_ID,
            Contract.ProjectEntry.COLUMN_PROJECT_NAME,
            Contract.ProjectEntry.COLUMN_PROJECT_DATE,
            Contract.ProjectEntry.COLUMN_PROJECT_COLOR
    };

    public static final String[] MAIN_TASKS_PROJECTION = {
            Contract.TaskEntry._ID,
            Contract.TaskEntry.COLUMN_TASK_ID,
            Contract.TaskEntry.COLUMN_TASK_PROJECT_ID,
            Contract.TaskEntry.COLUMN_TASK_NAME,
            Contract.TaskEntry.COLUMN_TASK_DATE,
            Contract.TaskEntry.COLUMN_TASK_STATUS,
            Contract.TaskEntry.COLUMN_TASK_PRIORITY,
            Contract.TaskEntry.COLUMN_TASK_REMINDER_DATE
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_project_list, container, false);
        ButterKnife.bind(this, view);

        // Set the Project Adapter and the RecyclerView
        projectAdapter = new ProjectAdapter(getActivity(), this, this, this);
        projectRecyclerView.setAdapter(projectAdapter);
        projectRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set the Swipe Refresher
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        // Add Project button logic
        fabAddProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewProjectButton();
            }
        });

        return view;
    }

    private void addNewProjectButton() {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in two-pane mode
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            AddTaskFragment fragment = new AddTaskFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.add_project_fragment, fragment).commit();
        } else { // If we're in Portrait mode
            Intent detailIntent = new Intent(getActivity(), AddProjectActivity.class);
            startActivity(detailIntent);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Load for the first time or Reload the project data if needed
        if (savedInstanceState == null) {
            getActivity().getSupportLoaderManager().initLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
            getActivity().getSupportLoaderManager().initLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
            getActivity().getSupportLoaderManager().initLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
            getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
            getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);
        }
    }

    /**
     * Method handling the click on a Project item
     * @param pos the adapter position of the project we clicked on
     * @param button the button we clicked on: the project name, the Edit button or the Delete button
     */
    @Override
    public void onClick(String pos, String button) {

        int position = Integer.valueOf(pos);
        mProjectCursor.moveToPosition(position);

        String projectId = String.valueOf(mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID));
        String projectName = String.valueOf(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
        String projectColor = mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_COLOR);
        String projectDate = mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_DATE);

        switch(button) {
            case "item":
                openProject(projectId, projectName, position);
                break;
            case "edit":
                editProject(projectId, projectName, projectColor, projectDate);
                break;
            case "delete":
                deleteProject(projectId);
                break;
        }
    }

    /**
     * OpenProject method. Displays the task list of the project selected in a Task List Fragment
     * @param projectId the ID of the project
     * @param projectName the name of the project
     * @param position the adapter position of the selected project
     */
    private void openProject(String projectId, String projectName, int position) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape Mode, open the project in a new fragment
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(TaskListFragment.ARG_ITEM_ID, projectId);
            arguments.putString(TaskListFragment.ARG_ITEM_NAME, projectName);
            arguments.putInt(TaskListFragment.ARG_ITEM_POSITION, position);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, projectList);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new activity
            Intent detailIntent = new Intent(getActivity(), TaskActivity.class);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_ID, projectId);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_NAME, projectName);
            String positionStr = String.valueOf(position);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_POSITION, positionStr);
            detailIntent.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST, projectList);
            startActivity(detailIntent);
        }
    }

    /**
     * EditProject method. Opens a AddProjectFragment with all the project data filled in
     * @param projectId the ID of the project
     * @param projectName the Name of the project
     * @param projectColor the bullet point color of the project
     * @param projectDate the creation date of the project
     */
    public void editProject(String projectId, String projectName, String projectColor, String projectDate) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape Mode, create a new fragment
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(AddProjectFragment.ARG_ITEM_ID, projectId);
            arguments.putString(AddProjectFragment.ARG_ITEM_NAME, projectName);
            arguments.putString(AddProjectFragment.ARG_ITEM_COLOR, projectColor);
            arguments.putString(AddProjectFragment.ARG_ITEM_DATE, projectDate);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.add_project_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Activity
            Intent detailIntent = new Intent(getActivity(), AddProjectActivity.class);
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_ID, projectId);
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_NAME, projectName);
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_COLOR, projectColor);
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_DATE, projectDate);
            startActivity(detailIntent);
        }
    }

    /**
     * DeleteProject method. Deletes the project using the ContentProvider
     * @param projectId the ID of the project
     */
    public void deleteProject(String projectId) {
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = projectId;
        getActivity().getContentResolver().delete(
                Contract.ProjectEntry.CONTENT_URI,
                Contract.ProjectEntry._ID + " = ?",
                mSelectionArgs);
    }

    /**
     * The main Loader. Divided in 3 options: loading the projects, loading the tasks for today or the week.
     * The 2 last options are used to get the numbers of tasks in the Today and Week indicators
     * @param id the ID given to the loader: Project, Today, Week
     * @param args the Loader arguments
     * @return the generated cursor
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Calendar clCurrent = Calendar.getInstance();
        String currentDate = DateFormatter.formatDate(clCurrent);

        switch (id) {
            // When load the projects
            case ID_PROJECTS_LOADER:
                /* URI for all rows of project data in our project table */
                Uri favoritesQueryUri = Contract.ProjectEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = Contract.ProjectEntry.COLUMN_PROJECT_DATE + " ASC";
                // Selection of all the data
                String selection = Contract.ProjectEntry.COLUMN_PROJECT_ID;

                return new CursorLoader(getActivity(),
                        favoritesQueryUri,
                        MAIN_PROJECTS_PROJECTION,
                        selection,
                        null,
                        sortOrder);

            // When we load the today's tasks
            case ID_TASKS_TODAY_LOADER:

                String[] mSelectionArgsToday = {""};
                mSelectionArgsToday[0] = currentDate;

                return new CursorLoader(getActivity(),
                        Contract.TaskEntry.CONTENT_URI,
                        MAIN_TASKS_PROJECTION,
                        Contract.TaskEntry.COLUMN_TASK_DATE + "=?",
                        mSelectionArgsToday,
                        null);

            // When we load the week's tasks
            case ID_TASKS_WEEK_LOADER:

                String[] mSelectionArgsWeek = {""};
                mSelectionArgsWeek[0] = currentDate;

                return new CursorLoader(getActivity(),
                        Contract.TaskEntry.CONTENT_URI,
                        MAIN_TASKS_PROJECTION,
                        Contract.TaskEntry.COLUMN_TASK_DATE + "=?",
                        mSelectionArgsWeek,
                        null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    /**
     * The post-loader method: show the project list, update the tasks numbers for Today and the Week
     * @param loader reference to the main loader
     * @param data the data we just loaded
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        switch (loader.getId()) {

            // Show the project list
            case ID_PROJECTS_LOADER:
                if (isAdded()) {
                    projectAdapter.setCursor(data);
                }
                mProjectCursor = data;

                for (int pos = 0; pos < mProjectCursor.getCount(); pos++) {
                    mProjectCursor.moveToPosition(pos);
                    projectList.add(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
                }

                if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

                if (data != null) {
                    if (data.getCount() != 0) {
                        showProjectDataView();
                    } else { // If there are no projects yet
                        showNoProjectMessage();
                    }
                }
                break;

            // Update the tasks number for the Today indicator
            case ID_TASKS_TODAY_LOADER:
                mTaskCursorToday = data;

                // Get all the Today tasks in an Array list and get the size of it
                ArrayList<String> taskListToday = new ArrayList<String>();
                for (int pos = 0; pos < mTaskCursorToday.getCount(); pos++) {
                    mTaskCursorToday.moveToPosition(pos);
                    String taskToday = mTaskCursorToday.getString(Contract.TaskEntry.POSITION_TASK_ID);
                    taskListToday.add(taskToday);
                }
                mTaskNumberToday = taskListToday.size();

                // Display the number in the appropriate TextView
                todayNumberTasks.setText(String.valueOf(mTaskNumberToday));
                break;

            // Update the tasks number for the Week indicator
            case ID_TASKS_WEEK_LOADER:
                mTaskCursorWeek = data;

                // Get all the Week tasks in an Array list and get the size of it
                ArrayList<String> taskListWeek = new ArrayList<String>();
                for (int pos = 0; pos < mTaskCursorWeek.getCount(); pos++) {
                    mTaskCursorWeek.moveToPosition(pos);
                    String taskWeek = mTaskCursorWeek.getString(Contract.TaskEntry.POSITION_TASK_ID);
                    taskListWeek.add(taskWeek);
                }
                mTaskNumberWeek = taskListWeek.size();

                // Display the number in the appropriate TextView
                weekNumberTasks.setText(String.valueOf(mTaskNumberWeek));
        }
    }

    /**
     * Reset method for the Loader
     * @param loader the loader we just used
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        if (loader.getId() == ID_PROJECTS_LOADER) {
            //Clear the Adapter
            if (isAdded()) {
                projectAdapter.setCursor(null);
            }
        }
    }

    /**
     * This method will make the View for the projects data visible and
     * hide the error message.
     */
    private void showProjectDataView() {
        /* Hide the error message */
        errorMessage.setVisibility(View.INVISIBLE);
        /* Make the movie data visible */
        projectRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the project list view
     */
    private void showErrorMessage() {

    }

    /**
     * This method will make the "No Projects" message visible and hide the movie
     * View.
     */
    private void showNoProjectMessage() {
        /* Hide the current data */
        projectRecyclerView.setVisibility(View.INVISIBLE);
        // Show the no project message
        errorMessage.setText(R.string.no_project_message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    /**
     * When the user pulls the swipe refresh feature
     */
    @Override
    public void onRefresh() {
        getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);
    }

    // Close all the cursors
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProjectCursor != null) {
            mProjectCursor.close();
        }
        if (mTaskCursorToday != null) {
            mTaskCursorToday.close();
        }
        if (mTaskCursorWeek != null) {
            mTaskCursorWeek.close();
        }
    }
}