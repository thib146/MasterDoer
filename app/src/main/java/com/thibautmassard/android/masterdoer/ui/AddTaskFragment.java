package com.thibautmassard.android.masterdoer.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;
import com.thibautmassard.android.masterdoer.reminders.AlarmScheduler;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 03/04/2017.
 */

public class AddTaskFragment extends Fragment implements
        EditTaskReminderDialog.OnSetReminderDate,
        EditTaskDateDialog.OnSetDueDate {

    @BindView(R.id.add_task_name_edit_text) EditText addTaskNameEditText;
    @BindView(R.id.add_task_date_edit_text) EditText addTaskDateEditText;
    @BindView(R.id.add_task_priority_picker) NumberPicker priorityPicker;
    @BindView(R.id.add_task_project_spinner) Spinner addTaskProjectSpinner;
    @BindView(R.id.add_task_reminder_edit_text) EditText addTaskReminderEditText;
    @BindView(R.id.add_task_button_cancel) Button addTaskButtonCancel;
    @BindView(R.id.add_task_button_add) Button addTaskButtonAdd;
    @BindView(R.id.add_task_priority_description) TextView taskPriorityDescription;

    private Activity mActivity;

    private Cursor mProjectCursor;

    // Prepare the values passed through intents
    public static final String ARG_ITEM_PROJECT_ID = "item_project_id";
    public static final String ARG_ITEM_PROJECT_NAME = "item_project_name";
    public static final String ARG_ITEM_PROJECT_POSITION = "item_project_position";
    
    public static final String ARG_ITEM_TASK_ID = "item_task_id";
    public static final String ARG_ITEM_TASK_NAME = "item_task_name";
    public static final String ARG_ITEM_TASK_DATE = "item_task_date";
    public static final String ARG_ITEM_TASK_PRIORITY = "item_task_priority";
    public static final String ARG_ITEM_TASK_REMINDER_DATE = "item_task_reminder_date";
    
    public static final String ARG_PROJECT_LIST = "project_list";

    public static boolean mTodayView;
    public static boolean mWeekView;

    public static final int ID_PROJECTS_LOADER = 146;

    private String mProjectName;
    private String mProjectId;
    private int mProjectPosition;
    private int mSpinnerItemPosition;

    private int mReminderDay;
    private int mReminderMonth;
    private int mReminderYear;
    private int mReminderHour;
    private int mReminderMinute;

    public static String mTaskId;
    public static String mTaskName;
    public static String mTaskDueDate;
    public static String mTaskPriority;
    public static String mTaskReminderDate;

    private ArrayList<String> mProjectList = new ArrayList<String>();
    private ArrayList<String> mProjectIdList = new ArrayList<String>();

    boolean mEditMode = false;

    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of tasks.
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

        View view = inflater.inflate(R.layout.fragment_add_task, container, false);
        ButterKnife.bind(this, view);

        // Get the complete project List with a query
        mProjectCursor = getProjectList();

        // Set the project list data (names + ids) in the String arrays
        mProjectList = setProjectListData(mProjectCursor, mProjectList);
        mProjectIdList = setProjectIdListData(mProjectCursor, mProjectIdList);

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        // Get the data from the intent
        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            // TODO: what if there's no project yet, but we're trying to create a task in the today view??
            // TODO: -> tell the user to create a project
            if (mTodayView) { // If we're in the today view, set the project as default
                mProjectId = mProjectIdList.get(0);
                mProjectName = mProjectList.get(0);
                mProjectPosition = 0;
            } else if (mWeekView) { // If we're in the week view, set the first project as default
                mProjectId = mProjectIdList.get(0);
                mProjectName = mProjectList.get(0);
                mProjectPosition = 0;
            } else { // If we're inside a project, get the project info
                mProjectId = bundle.getString(AddTaskFragment.ARG_ITEM_PROJECT_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = bundle.getString(AddTaskFragment.ARG_ITEM_PROJECT_NAME);
                mProjectPosition = bundle.getInt(AddTaskFragment.ARG_ITEM_PROJECT_POSITION);

                // If we're editing a task get the relevant information
                mTaskId = bundle.getString(AddTaskFragment.ARG_ITEM_TASK_ID);
                mTaskName = bundle.getString(AddTaskFragment.ARG_ITEM_TASK_NAME);
                mTaskDueDate = bundle.getString(AddTaskFragment.ARG_ITEM_TASK_DATE);
                mTaskPriority = bundle.getString(AddTaskFragment.ARG_ITEM_TASK_PRIORITY);
                mTaskReminderDate = bundle.getString(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE);
            }
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            if (mTodayView) { // If we're in the today view, set the first project as default
                mProjectId = mProjectIdList.get(0);
                mProjectName = mProjectList.get(0);
                mProjectPosition = 0;
            } else if (mWeekView) { // If we're in the week view, set the first project as default
                mProjectId = mProjectIdList.get(0);
                mProjectName = mProjectList.get(0);
                mProjectPosition = 0;
            } else { // If we're inside a project, get the project info
                mProjectId = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME);
                String projectPositionStr = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION);
                mProjectPosition = Integer.valueOf(projectPositionStr);

                // If we're editing a task get the relevant information
                mTaskId = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_ID);
                mTaskName = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_NAME);
                mTaskDueDate = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_DATE);
                mTaskPriority = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_PRIORITY);
                mTaskReminderDate = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE);
            }
        }

        // Test if we're in the edit mode (if we have a task ID)
        if (mTaskId != null) {
            mEditMode = true;
        }

        // If we're in Edit Mode, fill the activity with the info we have
        if (mEditMode) {
            addTaskNameEditText.setText(mTaskName);
            addTaskDateEditText.setText(mTaskDueDate);
            addTaskReminderEditText.setText(mTaskReminderDate);
            addTaskButtonAdd.setText(getResources().getString(R.string.add_task_update_button));

            priorityPicker.setValue(Integer.valueOf(mTaskPriority));
        } else {
            priorityPicker.setValue(0);
        }

        // Add a Due Date button logic : open a date picking dialog
        addTaskDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new EditTaskDateDialog().show(getActivity().getSupportFragmentManager(), "EditTaskDateDialogFragment");
            }
        });

        // Add a Reminder Date button logic: open a reminder date picking dialog
        addTaskReminderEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTaskReminderDialog dialogFragment = new EditTaskReminderDialog();
                Bundle args = new Bundle();
                //args.putLong("taskDate", mTask.dueDateMillis); // TODO: Add the Task due date selected by default
                dialogFragment.setArguments(args);
                dialogFragment.show(getActivity().getSupportFragmentManager(), "EditTaskReminderDialogFragment");
            }
        });

        priorityPicker.setMinValue(0);
        priorityPicker.setMaxValue(2);

        // Set the priority picker description to the default value
        taskPriorityDescription.setText(R.string.add_task_priority_desc_none);

        // Priority picker description logic
        priorityPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                switch(newVal) {
                    case 0:
                        taskPriorityDescription.setText(R.string.add_task_priority_desc_none);
                        break;
                    case 1:
                        taskPriorityDescription.setText(R.string.add_task_priority_desc_important);
                        break;
                    case 2:
                        taskPriorityDescription.setText(R.string.add_task_priority_desc_very_important);
                        break;
                }
            }
        });

        // Set the project spinner with the Project List we got from the intent, with the help of a local adapter
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, mProjectList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addTaskProjectSpinner.setAdapter(dataAdapter);
        addTaskProjectSpinner.setSelection(mProjectPosition); // Set the spinner selection on our current project

        // Set the Cancel button logic
        addTaskButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAddTaskFragment();
            }
        });

        // Set the Add Button Logic
        addTaskButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = addTaskNameEditText.getText().toString();

                if (!taskName.isEmpty()) {
                    addOrUpdateTask(taskName);
                } else {
                    Toast.makeText(getContext(), R.string.add_task_empty_name, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private Cursor getProjectList() {
        return getActivity().getContentResolver().query(
                Contract.ProjectEntry.CONTENT_URI,
                MAIN_PROJECTS_PROJECTION,
                Contract.ProjectEntry.COLUMN_PROJECT_ID,
                null,
                null);
    }

    private ArrayList<String> setProjectListData(Cursor projectCursor, ArrayList<String> projectList) {
        for (int pos = 0; pos < mProjectCursor.getCount(); pos++) {
            projectCursor.moveToPosition(pos);
            projectList.add(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
        }
        return projectList;
    }

    private ArrayList<String> setProjectIdListData(Cursor projectCursor, ArrayList<String> projectIdList) {
        for (int pos = 0; pos < mProjectCursor.getCount(); pos++) {
            projectCursor.moveToPosition(pos);
            projectIdList.add(mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID));
        }
        return projectIdList;
    }

    @Override
    public void onSetDueDate(String taskDueDate) {
        addTaskDateEditText.setText(taskDueDate);
    }

    private void addOrUpdateTask(String taskName) {
        String taskProjectName = addTaskProjectSpinner.getSelectedItem().toString();
        String taskDueDate = addTaskDateEditText.getText().toString();
        String taskReminderDate = addTaskReminderEditText.getText().toString();
        mSpinnerItemPosition = addTaskProjectSpinner.getSelectedItemPosition();
        int taskProjectId;

        if (taskProjectName.equals(mProjectName)) {
            taskProjectId = Integer.valueOf(mProjectId);
        } else {
            taskProjectId = Integer.valueOf(mProjectIdList.get(mSpinnerItemPosition));
        }

        int taskPriority = priorityPicker.getValue();

        Calendar clCurrent = Calendar.getInstance();
        String currentDate = DateFormatter.formatDate(clCurrent);

        int i = 2;

        // Create a ContentValues to store the new task data
        ContentValues taskValue = new ContentValues();
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_ID, i);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_PROJECT_ID, taskProjectId);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, "0");
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_PRIORITY, taskPriority);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_NAME, taskName);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_DATE, taskDueDate);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_REMINDER_DATE, taskReminderDate);

        ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
        taskValuesContent.add(taskValue);
        Uri taskUri = null;

        // ** INSERT / UPDATE ** //
        if (mEditMode) { // If we're editing a task, update it
            String[] mSelectionArgs = {""};
            mSelectionArgs[0] = mTaskId;

            getActivity().getContentResolver().update(
                    Contract.TaskEntry.CONTENT_URI,
                    taskValue,
                    Contract.TaskEntry._ID + "=?",
                    mSelectionArgs);
        } else { // If we're creating a new task, insert a new item
            taskUri = getActivity().getContentResolver().insert(
                    Contract.TaskEntry.CONTENT_URI,
                    taskValue);
        }

        // Set the reminder alarm
        addReminderAlarmNotification(taskUri);

        // Close the fragment when we're done
        closeAddTaskFragment();
    }

    /**
     * Listener activated whenever a reminder date is set
     * @param reminderDate the complete reminder date to display in the edit text
     * @param reminderDay the reminder day
     * @param reminderMonth the reminder month
     * @param reminderYear the reminder year
     * @param reminderHour the reminder hour
     * @param reminderMinute the reminder minute
     */
    @Override
    public void onSetReminderDate(String reminderDate, String reminderDay, String reminderMonth,
                                  String reminderYear, String reminderHour, String reminderMinute) {
        addTaskReminderEditText.setText(reminderDate);

        mReminderDay = Integer.valueOf(reminderDay);
        mReminderMonth = Integer.valueOf(reminderMonth);
        mReminderYear = Integer.valueOf(reminderYear);
        mReminderHour = Integer.valueOf(reminderHour);
        mReminderMinute = Integer.valueOf(reminderMinute);
    }

    /**
     * Add a new alarm reminder to the device, using the uri of the task we just created/updated
     * @param taskUri the uri of the task we just created
     */
    private void addReminderAlarmNotification(Uri taskUri) {
        if (mReminderYear != 0 && taskUri != null) {
            //TODO: add Reminder function for Edit Mode
            //if (mReminderYear != 0 && (taskUri != null || mEditMode)) {

            // Create a Date variable containing the reminder date data
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, mReminderYear);
            c.set(Calendar.MONTH, mReminderMonth);
            c.set(Calendar.DAY_OF_MONTH, mReminderDay);
            c.set(Calendar.HOUR_OF_DAY, mReminderHour);
            c.set(Calendar.MINUTE, mReminderMinute);
            c.set(Calendar.SECOND, 0);

            long time = c.getTimeInMillis();

            // Use the AlarmScheduler class provided to set the reminder
            AlarmScheduler.scheduleAlarm(getActivity(), time, taskUri);
        }
    }

    /**
     * Close the fragment properly once we're done
     */
    private void closeAddTaskFragment() {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Landscape Mode, display the task list fragment again
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(TaskListFragment.ARG_ITEM_ID, mProjectId);
            arguments.putString(TaskListFragment.ARG_ITEM_NAME, mProjectName);
            arguments.putInt(TaskListFragment.ARG_ITEM_POSITION, mProjectPosition);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, mProjectList);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in portrait mode, close the activity
            getActivity().finish();
        }
    }

    /**
     * This method will make the View for the add task data visible and
     * hide the error message.
     */
    private void showAddTaskView() {

    }

    /**
     * This method will make the error message visible and hide the add task view
     */
    private void showErrorMessage() {

    }
}