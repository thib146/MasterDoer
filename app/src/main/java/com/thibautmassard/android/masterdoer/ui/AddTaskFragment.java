package com.thibautmassard.android.masterdoer.ui;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;
import com.thibautmassard.android.masterdoer.data.Project;
import com.thibautmassard.android.masterdoer.data.Task;
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

    private Cursor mProjectCursor;

    // Prepare the values passed through intents
    public static final String ARG_ITEM_PROJECT_ID = "item_project_id";
    public static final String ARG_ITEM_PROJECT_NAME = "item_project_name";
    public static final String ARG_ITEM_PROJECT_POSITION = "item_project_position";
    
    public static final String ARG_ITEM_TASK_ID = "item_task_id";
    public static final String ARG_ITEM_TASK_NAME = "item_task_name";
    public static final String ARG_ITEM_TASK_DATE = "item_task_date";
    public static final String ARG_ITEM_TASK_STATUS = "item_task_status";
    public static final String ARG_ITEM_TASK_PRIORITY = "item_task_priority";
    public static final String ARG_ITEM_TASK_REMINDER_DATE = "item_task_reminder_date";
    
    public static final String ARG_PROJECT_LIST = "project_list";
    public static final String ARG_PROJECT_ID_LIST = "project_id_list";
    public static final String ARG_PROJECT_TASK_NUMBER_LIST = "project_task_number_list";
    public static final String ARG_PROJECT_TASK_DONE_LIST = "project_task_done_list";

    public static final String ARG_MAX_TASK_ID = "max_task_id";

    private long mMaxTaskId;

    public static boolean mTodayView;
    public static boolean mWeekView;

    private DatabaseReference mFirebaseDatabaseRef;

    public static final int ID_PROJECTS_LOADER = 146;

    private String mProjectName;
    private String mProjectId;
    private String mProjectTaskNumber = "0";
    private String mProjectTaskDone = "0";
    private int mProjectPosition;
    private int mSpinnerItemPosition;

    private int mReminderDay;
    private int mReminderMonth;
    private int mReminderYear;
    private int mReminderHour;
    private int mReminderMinute;

    public static Task mTask;

    private ArrayList<String> mProjectList = new ArrayList<String>();
    private ArrayList<String> mProjectIdList = new ArrayList<String>();
    private ArrayList<String> mProjectTaskNumberList = new ArrayList<String>();
    private ArrayList<String> mProjectTaskDoneList = new ArrayList<String>();

    boolean mEditMode = false;

    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of tasks.
     */
    public static final String[] MAIN_PROJECTS_PROJECTION = {
            Contract.ProjectEntry._ID,
            Contract.ProjectEntry.COLUMN_PROJECT_NAME,
            Contract.ProjectEntry.COLUMN_PROJECT_DATE,
            Contract.ProjectEntry.COLUMN_PROJECT_COLOR,
            Contract.ProjectEntry.COLUMN_PROJECT_TASK_NUMBER,
            Contract.ProjectEntry.COLUMN_PROJECT_TASK_DONE
    };

    public static final String[] MAIN_TASKS_PROJECTION = {
            Contract.TaskEntry._ID,
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

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

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
            if (mTodayView || mWeekView) { // If we're in the today/week view, set the project as default
                mProjectTaskNumberList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST);
                mProjectTaskDoneList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST);
                mMaxTaskId = bundle.getLong(AddTaskFragment.ARG_MAX_TASK_ID);
                mProjectList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_LIST);
                mProjectIdList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST);
                mProjectId = mProjectIdList.get(0);
                mProjectName = mProjectList.get(0);
                mProjectTaskNumber = mProjectTaskNumberList.get(0);
                if (mProjectTaskDoneList != null) {
                    mProjectTaskDone = mProjectTaskDoneList.get(0);
                }
                mProjectPosition = 0;
            } else { // If we're inside a project, get the project info
                mProjectId = bundle.getString(AddTaskFragment.ARG_ITEM_PROJECT_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = bundle.getString(AddTaskFragment.ARG_ITEM_PROJECT_NAME);
                mProjectPosition = bundle.getInt(AddTaskFragment.ARG_ITEM_PROJECT_POSITION);
                mMaxTaskId = bundle.getLong(AddTaskFragment.ARG_MAX_TASK_ID);
                mProjectList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_LIST);
                mProjectIdList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST);

                mProjectTaskNumberList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST);
                mProjectTaskDoneList = bundle.getStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST);
                mProjectTaskNumber = mProjectTaskNumberList.get(mProjectPosition);
                if (mProjectTaskDoneList != null) {
                    mProjectTaskDone = mProjectTaskDoneList.get(mProjectPosition);
                }

                // If we're editing a task get the relevant information
                mTask = new Task(bundle.getString(AddTaskFragment.ARG_ITEM_TASK_ID),
                                mProjectId,
                                bundle.getString(AddTaskFragment.ARG_ITEM_TASK_NAME),
                                bundle.getString(AddTaskFragment.ARG_ITEM_TASK_DATE),
                                bundle.getString(AddTaskFragment.ARG_ITEM_TASK_STATUS),
                                bundle.getString(AddTaskFragment.ARG_ITEM_TASK_PRIORITY),
                                bundle.getString(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE));
            }
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            if (mTodayView || mWeekView) { // If we're in the today/week view, set the first project as default
                mProjectTaskNumberList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST);
                mProjectTaskDoneList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST);
                mMaxTaskId = intentThatStartedThatActivity.getLongExtra(AddTaskFragment.ARG_MAX_TASK_ID, 0);
                String projectPositionStr = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION);
                if (projectPositionStr != null) {
                    mProjectPosition = Integer.valueOf(projectPositionStr);
                } else {
                    mProjectPosition = 0;
                }
                mProjectList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST);
                mProjectIdList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST);
                mProjectId = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID);
                for (int i=0; i < mProjectIdList.size(); i++) {
                    if (mProjectId.equals(mProjectIdList.get(i))) {
                        mProjectPosition = i;
                    }
                }
                mProjectName = mProjectList.get(mProjectPosition);
                //mProjectTaskNumber = mProjectTaskNumberList.get(0);
                if (mProjectTaskDoneList != null) {
                    //mProjectTaskDone = mProjectTaskDoneList.get(0);
                }
                //mProjectPosition = 0;
            } else { // If we're inside a project, get the project info
                mProjectId = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID);
                TaskActivity.mProjectId = mProjectId;
                mProjectName = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME);
                String projectPositionStr = intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION);
                mProjectPosition = Integer.valueOf(projectPositionStr);
                mMaxTaskId = intentThatStartedThatActivity.getLongExtra(AddTaskFragment.ARG_MAX_TASK_ID, 0);
                mProjectList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST);
                mProjectIdList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST);

                mProjectTaskNumberList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST);
                mProjectTaskDoneList = intentThatStartedThatActivity.getStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST);
                //mProjectTaskNumber = mProjectTaskNumberList.get(mProjectPosition);
                if (mProjectTaskDoneList != null) {
                    //mProjectTaskDone = mProjectTaskDoneList.get(mProjectPosition);
                }
            }

            // If we're editing a task get the relevant information
            mTask = new Task(intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_ID),
                            mProjectId,
                            intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_NAME),
                            intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_DATE),
                            intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_STATUS),
                            intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_PRIORITY),
                            intentThatStartedThatActivity.getStringExtra(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE));
        }

        // Test if we're in the edit mode (if we have a task ID)
        if (mTask.id != null) {
            mEditMode = true;
        }

        priorityPicker.setMinValue(0);
        priorityPicker.setMaxValue(2);

        // If we're in Edit Mode, fill the activity with the info we have
        if (mEditMode) {
            addTaskNameEditText.setText(mTask.name);
            if (mTask.date.equals("")) {
                addTaskDateEditText.setText("");
            } else {
                addTaskDateEditText.setText(DateFormatter.formatDate(mTask.date));
            }
            addTaskReminderEditText.setText(mTask.reminderDate);
            addTaskButtonAdd.setText(getResources().getString(R.string.add_task_update_button));

            priorityPicker.setValue(Integer.valueOf(mTask.priority));
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
                Contract.ProjectEntry._ID,
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
        if (!taskDueDate.equals("")) {
            long taskDateMillis = DateFormatter.dateToMillis(taskDueDate);
            taskDueDate = String.valueOf(taskDateMillis);
        }
        String taskReminderDate = addTaskReminderEditText.getText().toString();
        mSpinnerItemPosition = addTaskProjectSpinner.getSelectedItemPosition();
        int taskProjectId;
        String taskStatus;

        if (mEditMode) {
            taskStatus = mTask.status;
        } else {
            taskStatus = "0";
        }

        if (taskProjectName.equals(mProjectName)) {
            taskProjectId = Integer.valueOf(mProjectId);
        } else {
            taskProjectId = Integer.valueOf(mProjectIdList.get(mSpinnerItemPosition));
        }

        int taskPriority = priorityPicker.getValue();

        Calendar clCurrent = Calendar.getInstance();
        String currentDate = DateFormatter.formatDate(clCurrent);

        // Create a ContentValues to store the new task data
        ContentValues taskValue = new ContentValues();
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_PROJECT_ID, taskProjectId);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, taskStatus);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_PRIORITY, taskPriority);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_NAME, taskName);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_DATE, taskDueDate);
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_REMINDER_DATE, taskReminderDate);

        ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
        taskValuesContent.add(taskValue);
        Uri taskUri = null;

        // ** INSERT / UPDATE ** //
//        if (mEditMode) { // If we're editing a task, update it
//            String[] mSelectionArgs = {""};
//            mSelectionArgs[0] = mTask.id;
//
//            getActivity().getContentResolver().update(
//                    Contract.TaskEntry.CONTENT_URI,
//                    taskValue,
//                    Contract.TaskEntry._ID + "=?",
//                    mSelectionArgs);
//        } else { // If we're creating a new task, insert a new item
//            taskUri = getActivity().getContentResolver().insert(
//                    Contract.TaskEntry.CONTENT_URI,
//                    taskValue);
//        }

        String taskId;
        if (mEditMode) {
            taskId = mTask.id;
        } else {
            taskId = String.valueOf(mMaxTaskId + 1);
        }

        String taskPriorityStr = String.valueOf(taskPriority);
        String taskProjectIdStr = String.valueOf(taskProjectId);

        Task task = new Task(taskId, taskProjectIdStr, taskName, taskDueDate, taskStatus, taskPriorityStr, taskReminderDate);

//        int currTaskNumberInt = Integer.valueOf(mProjectTaskNumberList.get(mSpinnerItemPosition));
//        int newTaskNumberInt = currTaskNumberInt + 1;
//        String newTaskNumberStr = String.valueOf(newTaskNumberInt);
//        mProjectTaskNumberList.set(mSpinnerItemPosition, newTaskNumberStr); // update the number of Tasks to return to the Task List
//        String projectIdStr = mProjectIdList.get(mSpinnerItemPosition);
//
//        int prevTaskNumberInt = Integer.valueOf(mProjectTaskNumber);
//        int newPrevTaskNumberInt = prevTaskNumberInt - 1;
//        String newPrevTaskNumberStr = String.valueOf(newPrevTaskNumberInt);
//
//        String prevProjectId = mProjectIdList.get(mProjectPosition);
//
//        int currTaskDoneInt = 0;
//        int prevTaskDoneInt = 0;
//        int newTaskDoneInt = 0;
//        int newPrevTaskDoneInt = 0;
//        String newTaskDoneStr = "";
//        String newPrevTaskDoneStr = "";
//        if (taskStatus.equals("1")) {
//            currTaskDoneInt = Integer.valueOf(mProjectTaskDoneList.get(mSpinnerItemPosition));
//            prevTaskDoneInt = Integer.valueOf(mProjectTaskDoneList.get(mProjectPosition));
//            newTaskDoneInt = currTaskDoneInt + 1;
//            newPrevTaskDoneInt = prevTaskDoneInt - 1;
//            newTaskDoneStr = String.valueOf(newTaskDoneInt);
//            newPrevTaskDoneStr = String.valueOf(newPrevTaskDoneInt);
//
//            mProjectTaskDoneList.set(mSpinnerItemPosition, newTaskDoneStr);
//            mProjectTaskDoneList.set(mProjectPosition, newPrevTaskDoneStr);
//        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks").child(taskId).setValue(task);
            //mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(projectIdStr).child("taskNumber").setValue(newTaskNumberStr);
//            if (mSpinnerItemPosition != mProjectPosition) {
//                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(prevProjectId).child("taskNumber").setValue(newPrevTaskNumberStr);
//                if (mTask.status.equals("1")) {
//                    mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(projectIdStr).child("taskDone").setValue(newTaskDoneStr);
//                    mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(prevProjectId).child("taskDone").setValue(newPrevTaskDoneStr);
//                }
//            }
            if (!mEditMode) {
                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxTaskId").setValue(mMaxTaskId + 1);
            }
        }

        // Set the reminder alarm
        addReminderAlarmNotification(taskId, taskProjectIdStr);

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
     * @param taskId the ID of the task we just created
     * @param taskProjectId the task's project ID
     */
    private void addReminderAlarmNotification(String taskId, String taskProjectId) {
        if (mReminderYear != 0 && taskId != null) {
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

            ArrayList<String> taskData = new ArrayList<>();
            taskData.add(0, taskId);
            taskData.add(1, taskProjectId);

            // Use the AlarmScheduler class provided to set the reminder
            AlarmScheduler.scheduleAlarm(getActivity(), time, taskData);
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
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, mProjectTaskNumberList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, mProjectTaskDoneList);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in portrait mode, close the activity
            TaskListFragment.updateProjectLists(mProjectTaskNumberList, mProjectTaskDoneList);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProjectTaskNumberList = null;
        mProjectTaskDoneList = null;
    }
}