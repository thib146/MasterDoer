package com.thibautmassard.android.masterdoer.ui;

import android.content.ContentValues;
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
import com.thibautmassard.android.masterdoer.data.TaskAdapter;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 31/03/2017.
 */

public class TaskListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        TaskAdapter.TaskAdapterOnClickHandler {

    @BindView(R.id.task_recycler_view) RecyclerView taskRecyclerView;
    @BindView(R.id.task_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.task_error_message) TextView errorMessage;
    @BindView(R.id.fab_add_task) FloatingActionButton fabAddTask;

    // Prepare the values passed through intents
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_NAME = "item_name";
    public static final String ARG_ITEM_POSITION = "item_position";
    public static final String ARG_ITEM_TODAY = "today";
    public static final String ARG_ITEM_WEEK = "week";
    public static final String ARG_PROJECT_LIST= "project_list";

    private Cursor mTaskCursor;

    private String mProjectId;
    private String mProjectName;
    private int mProjectPosition;
    private String mProjectPositionStr;
    private int mTaskStatus;
    private ArrayList<String> projectList = new ArrayList<String>();

    public static boolean mTodayView;
    public static boolean mWeekView;

    private TaskAdapter taskAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    public static final int ID_TASKS_LOADER = 158;

    /*
     * The columns of data that we are interested in displaying
     */
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

        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        ButterKnife.bind(this, view);

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            if (!mTodayView && !mWeekView){
                mProjectId = bundle.getString(TaskListFragment.ARG_ITEM_ID);
                mProjectName = bundle.getString(TaskListFragment.ARG_ITEM_NAME);
                mProjectPosition = bundle.getInt(TaskListFragment.ARG_ITEM_POSITION);
                projectList = bundle.getStringArrayList(TaskListFragment.ARG_PROJECT_LIST);
            }
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            if (!mTodayView && !mWeekView) {
                mProjectId = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_ID);
                mProjectName = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_NAME);
                mProjectPositionStr = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_POSITION);
                mProjectPosition = Integer.valueOf(mProjectPositionStr);
                projectList = intentThatStartedThatActivity.getStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST);
            }
        }

        // If we're displaying Today or This Week's tasks, change the project name accordingly
        if (mTodayView) {
            mProjectName = "Today";
        } else if (mWeekView) {
            mProjectName = "This week";
        }

        // Pass the project name and ID to the Activity
        TaskActivity.mProjectName = mProjectName;
        TaskActivity.mProjectId = mProjectId;

        // Set up the TaskAdapter and the RecyclerView
        taskAdapter = new TaskAdapter(getActivity(), this, this, this, this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set up the SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        // Add new Task Button logic
        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTaskButton();
            }
        });

        return view;
    }

    private void addNewTaskButton() {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in two-pane mode, create a new Fragment for each of the choices
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            if (mTodayView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                AddTaskFragment.mWeekView = true;
            } else {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, mProjectId);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, mProjectName);
                arguments.putInt(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, mProjectPosition);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
            }

            AddTaskFragment fragment = new AddTaskFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, create a new activity for each of the choices
            Intent detailIntent = new Intent(getActivity(), AddTaskActivity.class);
            if (mTodayView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                AddTaskFragment.mWeekView = true;
            } else {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, mProjectId);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, mProjectName);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, mProjectPositionStr);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
            }
            startActivity(detailIntent);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize or restart the Loader accordingly to the activity state
        if (savedInstanceState == null) {
            getActivity().getSupportLoaderManager().initLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
        }
    }

    /**
     * Task item OnClick method. Handles the Edit and Delete buttons
     * @param pos adapter position of the Task selected
     * @param button button clicked on
     */
    @Override
    public void onClick(String pos, String button) {
        int position = Integer.valueOf(pos);
        mTaskCursor.moveToPosition(position);

        // Get the task data
        String taskId = mTaskCursor.getString(Contract.TaskEntry.POSITION_ID);
        String taskName = String.valueOf(mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_NAME));
        String taskDate = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_DATE);
        String taskPriority = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_PRIORITY);
        String taskReminderDate = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_REMINDER_DATE);

        switch(button) {
            case "edit": // Open the AddTaskFragment and fill it with the task data
                editTask(taskId, taskName, taskDate, taskPriority, taskReminderDate, mProjectPositionStr);
                break;
            case "delete": // Delete the task
                deleteTask(taskId);
                break;
        }
    }

    /**
     * EditTask method. Opens the AddTaskFragment and fills in all the task values that we have
     * @param taskId the ID of the task
     * @param taskName the name of the tsk
     * @param taskDate the Due Date of the task
     * @param taskPriority the Priority of the task (0, 1 or 2)
     * @param taskReminderDate the Reminder Date of the task
     * @param projectPosition the adapter position of the Project (to select the right project by default
     */
    private void editTask(String taskId, String taskName, String taskDate, String taskPriority, String taskReminderDate,
                          String projectPosition) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape mode, open a new fragment with the task data
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_ID, taskId);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_NAME, taskName);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_DATE, taskDate);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_PRIORITY, taskPriority);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE, taskReminderDate);
            arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, projectPosition);
            AddTaskFragment fragment = new AddTaskFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.add_task_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Activity with the task data
            Intent detailIntent = new Intent(getActivity(), AddTaskActivity.class);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_ID, taskId);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_NAME, taskName);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_DATE, taskDate);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_PRIORITY, taskPriority);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE, taskReminderDate);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, projectPosition);
            startActivity(detailIntent);
        }
    }

    /**
     * DeleteTask method. Deletes the task using its ID
     * @param taskId the ID of the task
     */
    private void deleteTask(String taskId) {
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;

        getActivity().getContentResolver().delete(
                Contract.TaskEntry.CONTENT_URI,
                Contract.TaskEntry._ID + " = ?",
                mSelectionArgs);
    }

    /**
     * Listener for the Task CheckBox. Changes the status (Done / Normal) of the Task
     * When such action happens, we update the status of the task with the ContentProvider
     * @param taskId the ID of the task
     * @param taskStatus the status of the Task
     */
    @Override
    public void passTaskStatus(String taskId, int taskStatus) {
        mTaskStatus = taskStatus;

        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;

        // Create a ContentValues to use with the Update method
        ContentValues taskValue = new ContentValues();
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, taskStatus);

        ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
        taskValuesContent.add(taskValue);

        // Update the status of the task
        getActivity().getContentResolver().update(
                Contract.TaskEntry.CONTENT_URI,
                taskValue,
                Contract.TaskEntry._ID + " = ?",
                mSelectionArgs
                );
    }

    /**
     * The main Loader. Depending on the Task list (Project, Today, Week), the Cursor will be different
     * @param id the ID given to the loader
     * @param args the Loader arguments
     * @return the generated cursor
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case ID_TASKS_LOADER:
                /* URI for all rows of project data in our project table */
                Uri tasksQueryUri = Contract.TaskEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = Contract.TaskEntry.COLUMN_TASK_PRIORITY + " DESC, " + Contract.TaskEntry.COLUMN_TASK_DATE + " ASC";
                // Selection of all the data
                String selection = Contract.TaskEntry.COLUMN_TASK_PROJECT_ID;
                String selectionToday = Contract.TaskEntry.COLUMN_TASK_DATE;
                //TODO: selection for the week

                String[] mSelectionArgs = {""};
                mSelectionArgs[0] = mProjectId;

                Calendar clCurrent = Calendar.getInstance();
                String currentDate = DateFormatter.formatDate(clCurrent);

                String[] mSelectionArgsToday = {""};
                mSelectionArgsToday[0] = currentDate;

                if (mTodayView) {
                    return new CursorLoader(getActivity(),
                            tasksQueryUri,
                            MAIN_TASKS_PROJECTION,
                            selectionToday + "=?",
                            mSelectionArgsToday,
                            sortOrder);
                    //TODO : add the week filter
                } else if (mWeekView) {
                    return new CursorLoader(getActivity(),
                            tasksQueryUri,
                            MAIN_TASKS_PROJECTION,
                            selectionToday + "=?",
                            mSelectionArgsToday,
                            sortOrder);
                } else {
                    return new CursorLoader(getActivity(),
                            tasksQueryUri,
                            MAIN_TASKS_PROJECTION,
                            selection + "=?",
                            mSelectionArgs,
                            sortOrder);
                }

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    /**
     * The post-loader method: show the task list
     * @param loader reference to the main loader
     * @param data the data we just loaded
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (isAdded()) {
            taskAdapter.setCursor(data);
        }
        mTaskCursor = data;

        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

        if (data != null) {
            if (data.getCount() != 0) {
                showTaskDataView();
            } else { // If there are no tasks yet
                showNoTaskMessage();
            }
        }
    }

    /**
     * Reset method for the Loader
     * @param loader the loader we just used
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        //Clear the Adapter
        if (isAdded()) {
            taskAdapter.setCursor(null);
        }
    }

    /**
     * This method will make the View for the task data visible
     */
    private void showTaskDataView() {
        /* Hide the error message */
        errorMessage.setVisibility(View.INVISIBLE);
        /* Make the task data visible */
        taskRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible
     */
    private void showErrorMessage() {

    }

    /**
     * This method will make the "No Tasks" message visible and hide the tasks list
     */
    private void showNoTaskMessage() {
        /* Hide the current data */
        taskRecyclerView.setVisibility(View.INVISIBLE);
        errorMessage.setText(R.string.no_task_message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    // Restart the loader when the user refreshes
    @Override
    public void onRefresh() {
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
    }

    // Close all the cursors
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTaskCursor != null) {
            mTaskCursor.close();
        }
    }
}
