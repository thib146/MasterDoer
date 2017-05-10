package com.thibautmassard.android.masterdoer.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Contract;
import com.thibautmassard.android.masterdoer.data.DateFormatter;
import com.thibautmassard.android.masterdoer.data.Project;
import com.thibautmassard.android.masterdoer.data.ProjectAdapter;
import com.thibautmassard.android.masterdoer.data.Task;
import com.thibautmassard.android.masterdoer.data.TaskAdapter;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

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
    @BindView(R.id.task_error_message_logo) ImageView errorMessageLogo;
    @BindView(R.id.fab_add_task) FloatingActionButton fabAddTask;

    // Prepare the values passed through intents
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_NAME = "item_name";
    public static final String ARG_ITEM_POSITION = "item_position";
    public static final String ARG_ITEM_TODAY = "today";
    public static final String ARG_ITEM_WEEK = "week";
    public static final String ARG_PROJECT_LIST = "project_list";
    public static final String ARG_PROJECT_ID_LIST = "project_id_list";
    public static final String ARG_PROJECT_TASK_NUMBER_LIST= "project_task_number_list";
    public static final String ARG_PROJECT_TASK_DONE_LIST = "project_task_done_list";

    private long mMaxTaskId;

    private Cursor mTaskCursor;
    private Task mTask;

    private MergeCursor mergeCursor;

    private String mProjectId;
    private String mProjectName;
    private int mProjectPosition;
    private String mProjectPositionStr;
    private int mTaskStatus;
    private ArrayList<String> projectList = new ArrayList<String>();
    private ArrayList<String> projectIdList = new ArrayList<String>();
    public static ArrayList<String> projectTaskNumberList = new ArrayList<String>();
    public static ArrayList<String> projectTaskDoneList = new ArrayList<String>();
    private boolean projectListEmpty = true;

    public static boolean mTodayView;
    public static boolean mWeekView;

    private boolean noData = false;

    private TaskAdapter taskAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    public static final int ID_TASKS_LOADER = 158;

    private DatabaseReference mFirebaseDatabaseRef;

    /*
     * The columns of data that we are interested in displaying
     */
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

        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        ButterKnife.bind(this, view);

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            projectList = bundle.getStringArrayList(TaskListFragment.ARG_PROJECT_LIST);
            projectIdList = bundle.getStringArrayList(TaskListFragment.ARG_PROJECT_ID_LIST);
            projectTaskNumberList = bundle.getStringArrayList(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST);
            projectTaskDoneList = bundle.getStringArrayList(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST);
            if (!mTodayView && !mWeekView){
                mProjectId = bundle.getString(TaskListFragment.ARG_ITEM_ID);
                mProjectName = bundle.getString(TaskListFragment.ARG_ITEM_NAME);
                mProjectPosition = bundle.getInt(TaskListFragment.ARG_ITEM_POSITION);
            }
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            projectList = intentThatStartedThatActivity.getStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST);
            projectIdList = intentThatStartedThatActivity.getStringArrayListExtra(TaskListFragment.ARG_PROJECT_ID_LIST);
            projectTaskNumberList = intentThatStartedThatActivity.getStringArrayListExtra(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST);
            projectTaskDoneList = intentThatStartedThatActivity.getStringArrayListExtra(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST);
            if (!mTodayView && !mWeekView) {
                mProjectId = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_ID);
                mProjectName = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_NAME);
                mProjectPositionStr = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_POSITION);
                mProjectPosition = Integer.valueOf(mProjectPositionStr);
            }
        }

        if (projectList != null) {
            for (int i = 0; i < projectList.size(); i++) {
                if (projectList.get(i) != null) {
                    projectListEmpty = false;
                    break;
                }
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
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE && !projectListEmpty) {
            Bundle arguments = new Bundle();
            if (mTodayView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                arguments.putLong(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                arguments.putLong(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
                AddTaskFragment.mWeekView = true;
            } else {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, mProjectId);
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_NAME, mProjectName);
                arguments.putInt(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, mProjectPosition);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                arguments.putLong(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
            }

            AddTaskFragment fragment = new AddTaskFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
            // If we're in Portrait mode, create a new activity for each of the choices
        } else if (!getResources().getBoolean(R.bool.isTablet) && !projectListEmpty) {
            Intent detailIntent = new Intent(getActivity(), AddTaskActivity.class);
            if (mTodayView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                detailIntent.putExtra(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, "0");
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, "default");
                detailIntent.putExtra(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
                AddTaskFragment.mWeekView = true;
            } else {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, mProjectId);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_NAME, mProjectName);
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, mProjectPositionStr);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
                detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
                detailIntent.putExtra(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
            }
            startActivity(detailIntent);
        } else if (projectListEmpty) {
            Toast.makeText(getActivity(), R.string.add_task_no_project_today_week, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //getDataFromFirebase(user);

        // Initialize or restart the Loader accordingly to the activity state
//        if (savedInstanceState == null) {
//            getActivity().getSupportLoaderManager().initLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
//        } else {
//            getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
//        }
    }

    public void checkIfNoTaskData(final FirebaseUser user) {
        if (user != null) {
            String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                    "task_status", "task_priority", "task_reminder_date"};
            final MatrixCursor matrixCursor = new MatrixCursor(columns);

            final DatabaseReference checkIfNoDataRef = mFirebaseDatabaseRef.child("users").child(user.getUid());
            checkIfNoDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.hasChild("tasks")) {
                        noData = true;
                        swipeRefreshLayout.setRefreshing(false);

                        showNoTaskMessage();
                        mergeCursor = new MergeCursor(new Cursor[] { matrixCursor });
                        taskAdapter = new TaskAdapter(getActivity(), TaskListFragment.this,
                                TaskListFragment.this, TaskListFragment.this, TaskListFragment.this);
                        taskAdapter.setCursor(mergeCursor);
                        taskRecyclerView.setAdapter(taskAdapter);
                    } else {
                        noData = false;
                        if (mTodayView) {
                            getDataForTodayView(user);
                        } else if (mWeekView) {
                            getDataForWeekView(user);
                        } else {
                            getDataFromFirebase(user);
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

        }
    }

    public void getDataFromFirebase(final FirebaseUser user) {
        if (user != null) {
            String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                                                "task_status", "task_priority", "task_reminder_date"};
            final MatrixCursor matrixCursor = new MatrixCursor(columns);

            if (!noData) {
                final DatabaseReference taskRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks");
                taskRef.orderByChild("projectId").equalTo(mProjectId).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                        Task task = dataSnapshot.getValue(Task.class);

                        matrixCursor.addRow(new Object[]{task.id, task.projectId, task.name,
                                task.date, task.status, task.priority, task.reminderDate});
                        displayTasks(user, matrixCursor);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                displayDelayTasks(user, matrixCursor, 500);
            }
            final DatabaseReference maxTaskIdRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxTaskId");
            maxTaskIdRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMaxTaskId = (long) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void displayDelayTasks(final FirebaseUser user, final MatrixCursor matrixCursor, int time) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                displayTasks(user, matrixCursor);
            }
        }, time);
    }

    public void displayTasks(FirebaseUser user, MatrixCursor matrixTaskCursor) {
        swipeRefreshLayout.setRefreshing(false);

        mergeCursor = new MergeCursor(new Cursor[] { matrixTaskCursor });

        taskAdapter = new TaskAdapter(getActivity(), this, this, this, this);
        taskAdapter.setCursor(mergeCursor);
        taskRecyclerView.setAdapter(taskAdapter);

        if (mergeCursor.getCount() != 0) {
            showTaskDataView();
        } else { // If there are no projects yet
            showNoTaskMessage();
        }
    }

    public void getDataForTodayView(final FirebaseUser user) {
        if (user != null) {
            String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                    "task_status", "task_priority", "task_reminder_date"};
            final MatrixCursor matrixCursor = new MatrixCursor(columns);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            cal.set(Calendar.MILLISECOND,0);
            String minDate = String.valueOf(cal.getTimeInMillis());
            cal.set(Calendar.HOUR_OF_DAY,23);
            cal.set(Calendar.MINUTE,59);
            cal.set(Calendar.SECOND,59);
            cal.set(Calendar.MILLISECOND,0);
            String maxDate = String.valueOf(cal.getTimeInMillis());

            if (!noData) {
                final DatabaseReference taskRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks");
                taskRef.orderByChild("date").startAt(minDate).endAt(maxDate).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                        Task task = dataSnapshot.getValue(Task.class);

                        if (task.status.equals("0")) {
                            matrixCursor.addRow(new Object[]{task.id, task.projectId, task.name,
                                    task.date, task.status, task.priority, task.reminderDate});
                            displayTasks(user, matrixCursor);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                displayDelayTasks(user, matrixCursor, 500);
            }
            final DatabaseReference maxTaskIdRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxTaskId");
            maxTaskIdRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMaxTaskId = (long) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void getDataForWeekView(final FirebaseUser user) {
        if (user != null) {
            String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                    "task_status", "task_priority", "task_reminder_date"};
            final MatrixCursor matrixCursor = new MatrixCursor(columns);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            cal.set(Calendar.MILLISECOND,0);
            String minDate = String.valueOf(cal.getTimeInMillis());
            cal.set(Calendar.HOUR_OF_DAY,23);
            cal.set(Calendar.MINUTE,59);
            cal.set(Calendar.SECOND,59);
            cal.set(Calendar.MILLISECOND,0);
            String maxDate = String.valueOf(cal.getTimeInMillis() + 604800000);

            if (!noData) {
                final DatabaseReference taskRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks");
                taskRef.orderByChild("date").startAt(minDate).endAt(maxDate).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                        Task task = dataSnapshot.getValue(Task.class);

                        if (task.status.equals("0")) {
                            matrixCursor.addRow(new Object[]{task.id, task.projectId, task.name,
                                    task.date, task.status, task.priority, task.reminderDate});
                            displayTasks(user, matrixCursor);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
                displayDelayTasks(user, matrixCursor, 500);
            }
            final DatabaseReference maxTaskIdRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxTaskId");
            maxTaskIdRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMaxTaskId = (long) dataSnapshot.getValue();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
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
        //mTaskCursor.moveToPosition(position);
        mergeCursor.moveToPosition(position);

        //mTask = new Task(mTaskCursor);
        mTask = new Task(mergeCursor);

        switch(button) {
            case "edit": // Open the AddTaskFragment and fill it with the task data
                editTask(mTask.id, mTask.name, mTask.date, mTask.status, mTask.priority, mTask.reminderDate, mTask.projectId, mProjectPositionStr);
                break;
            case "delete": // Delete the task
                deleteTask(mTask.id, mTask.status, mTask.projectId, position);
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
    private void editTask(String taskId, String taskName, String taskDate, String taskStatus, String taskPriority, String taskReminderDate, String projectId,
                          String projectPosition) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape mode, open a new fragment with the task data
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_ID, taskId);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_NAME, taskName);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_DATE, taskDate);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_STATUS, taskStatus);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_PRIORITY, taskPriority);
            arguments.putString(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE, taskReminderDate);
            if (mTodayView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, "0");
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, "0");
                AddTaskFragment.mWeekView = true;
            } else {
                arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, projectPosition);
            }
            arguments.putString(AddTaskFragment.ARG_ITEM_PROJECT_ID, projectId);
            arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_LIST, projectList);
            arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
            arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            arguments.putStringArrayList(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
            arguments.putLong(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
            AddTaskFragment fragment = new AddTaskFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.add_task_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Activity with the task data
            Intent detailIntent = new Intent(getActivity(), AddTaskActivity.class);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_ID, taskId);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_NAME, taskName);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_DATE, taskDate);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_STATUS, taskStatus);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_PRIORITY, taskPriority);
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_TASK_REMINDER_DATE, taskReminderDate);
            if (mTodayView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, "0");
                AddTaskFragment.mTodayView = true;
            } else if (mWeekView) {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, "0");
                AddTaskFragment.mWeekView = true;
            } else {
                detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_POSITION, projectPosition);
            }
            detailIntent.putExtra(AddTaskFragment.ARG_ITEM_PROJECT_ID, projectId);
            detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_LIST, projectList);
            detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_ID_LIST, projectIdList);
            detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            detailIntent.putStringArrayListExtra(AddTaskFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
            detailIntent.putExtra(AddTaskFragment.ARG_MAX_TASK_ID, mMaxTaskId);
            startActivity(detailIntent);
        }
    }

    /**
     * DeleteTask method. Deletes the task using its ID
     * @param taskId the ID of the task
     */
    private void deleteTask(String taskId, String taskStatus, String taskProjectId, int pos) {
        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;

        // Delete Task on local storage
//        getActivity().getContentResolver().delete(
//                Contract.TaskEntry.CONTENT_URI,
//                Contract.TaskEntry._ID + " = ?",
//                mSelectionArgs);

//        int currTaskNumberInt = Integer.valueOf(projectTaskNumberList.get(mProjectPosition));
//        int newTaskNumberInt = currTaskNumberInt - 1;
//        String newTaskNumberStr = String.valueOf(newTaskNumberInt);
//
//        projectTaskNumberList.set(mProjectPosition, newTaskNumberStr);
//
//        int currTaskDoneInt = 0;
//        int newTaskDoneInt = 0;
//        String newTaskDoneStr = "";
//        if (taskStatus.equals("1")) {
//            currTaskDoneInt = Integer.valueOf(projectTaskDoneList.get(mProjectPosition));
//            newTaskDoneInt = currTaskDoneInt - 1;
//            newTaskDoneStr = String.valueOf(newTaskDoneInt);
//
//            projectTaskDoneList.set(mProjectPosition, newTaskDoneStr);
//        }

        // Delete Task on the Firebase database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks").child(taskId).removeValue();
            //mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(taskProjectId).child("taskNumber").setValue(newTaskNumberStr);
            //if (taskStatus.equals("1")) {
            //    mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(taskProjectId).child("taskDone").setValue(newTaskDoneStr);
            //}
        }

        getDataFromFirebase(user);
    }

    /**
     * Listener for the Task CheckBox. Changes the status (Done / Normal) of the Task
     * When such action happens, we update the status of the task with the ContentProvider
     * @param taskId the ID of the task
     * @param taskStatus the status of the Task
     */
    @Override
    public void passTaskStatus(String taskId, String taskProjectId, int position, int taskStatus) {
        mTaskStatus = taskStatus;

        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;

        // Create a ContentValues to use with the Update method
        ContentValues taskValue = new ContentValues();
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, taskStatus);

        ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
        taskValuesContent.add(taskValue);

        // Update the status of the task
//        getActivity().getContentResolver().update(
//                Contract.TaskEntry.CONTENT_URI,
//                taskValue,
//                Contract.TaskEntry._ID + " = ?",
//                mSelectionArgs
//                );

        String taskStatusStr = String.valueOf(taskStatus);

//        int currTaskDoneInt;
//        if (projectTaskDoneList != null) {
//            if (mProjectPosition < projectTaskDoneList.size()) {
//                currTaskDoneInt = Integer.valueOf(projectTaskDoneList.get(mProjectPosition));
//            } else {
//                currTaskDoneInt = 0;
//            }
//        } else {
//            currTaskDoneInt = 0;
//        }
//        int newTaskDoneInt;
//        if (taskStatus == 0) {
//            newTaskDoneInt = currTaskDoneInt - 1;
//        } else {
//            newTaskDoneInt = currTaskDoneInt + 1;
//        }
//        String newTaskDoneStr = String.valueOf(newTaskDoneInt);

        // Update our current list of done tasks
        //projectTaskDoneList.set(mProjectPosition, newTaskDoneStr);

        // Check the Task on the Firebase database + add a DONE task to the task's project
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks").child(taskId).child("status").setValue(taskStatusStr);
            //mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(taskProjectId).child("taskDone").setValue(newTaskDoneStr);
        }

        checkIfNoTaskData(user);
    }

    public static void updateProjectLists(ArrayList<String> projectTaskNumberList, ArrayList<String> projectTaskDoneList) {
        TaskListFragment.projectTaskNumberList = projectTaskNumberList;
        TaskListFragment.projectTaskDoneList = projectTaskDoneList;
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
        errorMessageLogo.setVisibility(View.INVISIBLE);
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
        if (mTodayView) {
            errorMessageLogo.setBackground(getResources().getDrawable(R.drawable.ic_today_grey_24dp));
            errorMessage.setText(R.string.no_task_today_message);
        } else if (mWeekView) {
            errorMessageLogo.setBackground(getResources().getDrawable(R.drawable.ic_date_range_grey_24dp));
            errorMessage.setText(R.string.no_task_week_message);
        } else {
            errorMessageLogo.setBackground(getResources().getDrawable(R.drawable.ic_check_box_outline_blank_black_24dp));
            errorMessage.setText(R.string.no_task_message);
        }
        errorMessageLogo.setVisibility(View.VISIBLE);
        errorMessage.setVisibility(View.VISIBLE);
    }

    // Restart the loader when the user refreshes
    @Override
    public void onRefresh() {
        //getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        checkIfNoTaskData(user);
//        if (mTodayView) {
//            getDataForTodayView(user);
//        } else if (mWeekView) {
//            getDataForWeekView(user);
//        } else {
//            //getDataFromFirebase(user);
//            checkIfNoTaskData(user);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        checkIfNoTaskData(user);
//        if (mTodayView) {
//            getDataForTodayView(user);
//        } else if (mWeekView) {
//            getDataForWeekView(user);
//        } else {
//            //getDataFromFirebase(user);
//            checkIfNoTaskData(user);
//        }
    }

    // Close all the cursors
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTaskCursor != null) {
            mTaskCursor.close();
        }
        if (mergeCursor != null) {
            mergeCursor.close();
        }
        projectTaskNumberList = null;
        projectTaskDoneList = null;
        mTodayView = false;
        mWeekView = false;
    }
}