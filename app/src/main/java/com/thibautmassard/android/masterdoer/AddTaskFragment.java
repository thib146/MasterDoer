package com.thibautmassard.android.masterdoer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 03/04/2017.
 */

public class AddTaskFragment extends Fragment {

    @BindView(R.id.add_task_name_edit_text) EditText addTaskNameEditText;
    @BindView(R.id.add_task_date_edit_text) EditText addTaskDateEditText;
    @BindView(R.id.add_task_priority_picker) NumberPicker priorityPicker;
    @BindView(R.id.add_task_project_spinner) Spinner addTaskProjectSpinner;
    @BindView(R.id.add_task_reminder_edit_text) EditText addTaskReminderEditText;
    @BindView(R.id.add_task_button_cancel) Button addTaskButtonCancel;
    @BindView(R.id.add_task_button_add) Button addTaskButtonAdd;

    private Activity mActivity;

    private Cursor projectCursor;

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_NAME = "item_name";
    public static final String ARG_ITEM_POSITION = "item_position";
    public static final String ARG_PROJECT_LIST = "project_list";

    public static boolean mTodayView;
    public static boolean mWeekView;

    public static final int ID_PROJECTS_LOADER = 146;

    private String mProjectName;
    private String mProjectId;
    private int mProjectPosition;
    private int mSpinnerItemPosition;

    public static String mTaskDueDate;

    private ProjectAdapter projectAdapter;

    private ArrayList<String> projectList = new ArrayList<String>();
    private ArrayList<String> projectIdList = new ArrayList<String>();

    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of
     * favorite movies data.
     */
    public static final String[] MAIN_PROJECTS_PROJECTION = {
            Contract.ProjectEntry._ID,
            Contract.ProjectEntry.COLUMN_PROJECT_ID,
            Contract.ProjectEntry.COLUMN_PROJECT_NAME,
            Contract.ProjectEntry.COLUMN_PROJECT_DATE,
            Contract.ProjectEntry.COLUMN_PROJECT_COLOR
    };

    private Cursor mCursor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_add_task, container, false);
        ButterKnife.bind(this, view);

        projectCursor = getActivity().getContentResolver().query(
                Contract.ProjectEntry.CONTENT_URI,
                MAIN_PROJECTS_PROJECTION,
                Contract.ProjectEntry.COLUMN_PROJECT_ID,
                null,
                null);

        for (int pos = 0; pos < projectCursor.getCount(); pos++) {
            projectCursor.moveToPosition(pos);
            projectIdList.add(projectCursor.getString(Contract.ProjectEntry.POSITION_ID));
            projectList.add(projectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
        }

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            if (mTodayView) {
                mProjectId = projectIdList.get(0);
                mProjectName = projectList.get(0);
                mProjectPosition = 0;
            } else if (mWeekView) {
                mProjectId = projectIdList.get(0);
                mProjectName = projectList.get(0);
                mProjectPosition = 0;
            } else {
                mProjectId = bundle.getString(AddTaskFragment.ARG_ITEM_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = bundle.getString(AddTaskFragment.ARG_ITEM_NAME);
                mProjectPosition = bundle.getInt(AddTaskFragment.ARG_ITEM_POSITION);
                //projectList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_LIST);
            }
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            if (mTodayView) {
                mProjectId = projectIdList.get(0);
                mProjectName = projectList.get(0);
                mProjectPosition = 0;
            } else if (mWeekView) {
                mProjectId = projectIdList.get(0);
                mProjectName = projectList.get(0);
                mProjectPosition = 0;
            } else {
                mProjectId = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_NAME);
                String projectPositionStr = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_POSITION);
                mProjectPosition = Integer.valueOf(projectPositionStr);
                //projectList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST);
            }
        }

        //projectAdapter = new ProjectAdapter(getActivity(), this);

        addTaskDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new EditTaskDateDialog().show(getActivity().getSupportFragmentManager(), "EditTaskDateDialogFragment");
            }
        });

        priorityPicker.setMinValue(0);
        priorityPicker.setMaxValue(2);
        priorityPicker.setValue(0);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, projectList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addTaskProjectSpinner.setAdapter(dataAdapter);
        addTaskProjectSpinner.setSelection(mProjectPosition);

        addTaskButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the device's current orientation
                int orientation = getResources().getConfiguration().orientation;

                // If we're in Landscape Mode, refresh the Detail Fragment with the new data
                if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
                    Bundle arguments = new Bundle();
                    arguments.putString(TaskListFragment.ARG_ITEM_ID, mProjectId);
                    arguments.putString(TaskListFragment.ARG_ITEM_NAME, mProjectName);
                    arguments.putInt(TaskListFragment.ARG_ITEM_POSITION, mProjectPosition);
                    arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, projectList);
                    TaskListFragment fragment = new TaskListFragment();
                    fragment.setArguments(arguments);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.task_list_fragment, fragment).commit();
                } else {
                    getActivity().finish();
                }
            }
        });

        addTaskButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = addTaskNameEditText.getText().toString();
                String taskProjectName = addTaskProjectSpinner.getSelectedItem().toString();
                String taskDueDate = addTaskDateEditText.getText().toString();
                mSpinnerItemPosition = addTaskProjectSpinner.getSelectedItemPosition();
                int taskProjectId;

                if (taskProjectName.equals(mProjectName)) {
                    taskProjectId = Integer.valueOf(mProjectId);
                } else {
                    taskProjectId = Integer.valueOf(projectIdList.get(mSpinnerItemPosition));
                }

                int taskPriority = priorityPicker.getValue();

                if (taskName.equals("") || taskName.isEmpty()) {
                    Toast.makeText(getContext(), R.string.add_task_empty_name, Toast.LENGTH_SHORT).show();
                } else {
                    ContentValues taskValue = new ContentValues();

                    Calendar clCurrent = Calendar.getInstance();
                    int currentDay = clCurrent.get(Calendar.DAY_OF_MONTH);
                    int currentMonth = clCurrent.get(Calendar.MONTH)+1;
                    int currentYear = clCurrent.get(Calendar.YEAR);
                    String currentDate = Integer.toString(currentDay)
                            + "/" + Integer.toString(currentMonth) + "/" + Integer.toString(currentYear);

                    int i = 2;

                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_ID, i);
                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_PROJECT_ID, taskProjectId);
                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, "0");
                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_PRIORITY, taskPriority);
                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_NAME, taskName);

                    taskValue.put(Contract.TaskEntry.COLUMN_TASK_DATE, taskDueDate);

                    ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
                    taskValuesContent.add(taskValue);

                    getActivity().getContentResolver().bulkInsert(
                            Contract.TaskEntry.CONTENT_URI,
                            taskValuesContent.toArray(new ContentValues[1]));

                    // Get the device's current orientation
                    int orientation = getResources().getConfiguration().orientation;

                    // If we're in Landscape Mode
                    if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
                        Bundle arguments = new Bundle();
                        arguments.putString(TaskListFragment.ARG_ITEM_ID, mProjectId);
                        arguments.putString(TaskListFragment.ARG_ITEM_NAME, mProjectName);
                        arguments.putInt(TaskListFragment.ARG_ITEM_POSITION, mProjectPosition);
                        arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, projectList);
                        TaskListFragment fragment = new TaskListFragment();
                        fragment.setArguments(arguments);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.task_list_fragment, fragment).commit();
                    } else {
                        getActivity().finish();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context; // Store the main activity in mActivity
        }
    }

    /**
     * This method will make the View for the movie data visible and
     * hide the error message.
     */
    private void showAddTaskView() {
        /* Hide the error message */
        //errorMessage.setVisibility(View.INVISIBLE);
        /* Make the movie data visible */
        //projectRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the movie
     * View.
     */
    private void showErrorMessage() {
//        /* Hide the current data */
//        mRecyclerView.setVisibility(View.INVISIBLE);
//        /* Chose which error message to display */
//        if (!mConnected) { // If the internet connexion is lost
//            mErrorMessageDisplay.setText(R.string.error_message_internet);
//        } else if (!NetworkUtils.isApiKeyOn()) { // If the API Key is empty
//            mErrorMessageDisplay.setText(R.string.error_message_api_key);
//        } else { // For any other problem
//            mErrorMessageDisplay.setText(R.string.error_message_common);
//        }
//        /* Show the error view */
//        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            mCursor.close();
        }
        mTodayView = false;
        mWeekView = false;
    }
}