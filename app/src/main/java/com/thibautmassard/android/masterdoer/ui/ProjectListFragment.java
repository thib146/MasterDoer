package com.thibautmassard.android.masterdoer.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.animation.Animator;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
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
import com.willowtreeapps.spruce.Spruce;
import com.willowtreeapps.spruce.animation.DefaultAnimations;
import com.willowtreeapps.spruce.sort.DefaultSort;
import com.willowtreeapps.spruce.sort.LinearSort;
import com.willowtreeapps.spruce.sort.RadialSort;

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
    @BindView(R.id.project_error_message_logo) ImageView errorMessageLogo;
    //@BindView(R.id.fab_add_project) FloatingActionButton fabAddProject;
    @BindView(R.id.today_number_tasks) TextView todayNumberTasks;
    @BindView(R.id.week_number_tasks) TextView weekNumberTasks;
    @BindView(R.id.projects_loader) LottieAnimationView projectsLoader;
    @BindView(R.id.today_item) LinearLayout todayItem;
    @BindView(R.id.week_item) LinearLayout weekItem;

    private ProjectAdapter projectAdapter;
    private int mPosition = RecyclerView.NO_POSITION;
    private Animator spruceAnimator;

    private Cursor mProjectCursor;
    private Cursor mTaskCursorToday;
    private Cursor mTaskCursorWeek;

    private MergeCursor mergeProjectCursor;
    //private MergeCursor[] mergeTaskCursor;
    private MergeCursor mergeTaskCursor;

    private long mMaxProjectId;
    private String mUserName;

    private boolean noData = false;

    private Project mProject;

    private int prevCount;
    private boolean firstCall;

    private int oldProjectPosition;

    private int mTodayTaskNumber;
    private int mWeekTaskNumber;

    private String currentProjectId;

    private DatabaseReference mFirebaseDatabaseRef;

    private int mTaskNumberToday; // Number of tasks for the Today indicator
    private int mTaskNumberWeek; // Number of tasks for the Weekly indicator

    // Array list used to send to a Task List via an intent, in order to get the project list when adding a new Task
    private ArrayList<String> projectList = new ArrayList<String>();
    private ArrayList<String> projectIdList = new ArrayList<String>();
    public ArrayList<String> projectTaskNumberList = new ArrayList<String>();
    public ArrayList<String> projectTaskDoneList = new ArrayList<String>();
    public ArrayList<String> projectsHasTasksList = new ArrayList<String>();

    private Activity mActivity;

    public static final int ID_PROJECTS_LOADER = 146;
    public static final int ID_TASKS_TODAY_LOADER = 148;
    public static final int ID_TASKS_WEEK_LOADER = 149;

    private String mProjectName;

    private boolean firstStart = true;

    public interface OnUserNamePass {
        void onUserNamePass(String userName);
    }

    OnUserNamePass userNamePass;

    /*
     * The columns of data that we are interested in displaying within our Project List Fragment
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

        View view = inflater.inflate(R.layout.fragment_project_list, container, false);
        ButterKnife.bind(this, view);

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Set the Project Adapter and the RecyclerView
        //projectAdapter = new ProjectAdapter(getActivity(), this, this, this);
        //projectRecyclerView.setAdapter(projectAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity()) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                // Animate in the visible children
                if (firstStart && projectAdapter.getItemCount()!=0) {
                    spruceAnimator = new Spruce.SpruceBuilder(projectRecyclerView)
                            .sortWith(new LinearSort(100, false, LinearSort.Direction.TOP_TO_BOTTOM))
                            .animateWith(ObjectAnimator.ofFloat(projectRecyclerView, "translationY",
                                    projectRecyclerView.getHeight(), 0f).setDuration(800))
                            .start();
                    firstStart = false;
                }

            }
        };

        //LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

        projectRecyclerView.setLayoutManager(linearLayoutManager);

        // Set the Swipe Refresher
        swipeRefreshLayout.setOnRefreshListener(this);
        //swipeRefreshLayout.setRefreshing(true);

        // Add Project button logic
//        fabAddProject.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                addNewProjectButton();
//            }
//        });

        //addProjectButton.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        addNewProjectButton();
        //    }
        //});

        // Today view button logic
        todayItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTodayOrWeekView("today");
            }
        });

        // This week view button logic
        weekItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTodayOrWeekView("week");
            }
        });

        projectsLoader.setVisibility(View.VISIBLE);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.userNamePass = (OnUserNamePass) context;
    }

    public void addNewProjectButton() {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in two-pane mode
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            AddTaskFragment fragment = new AddTaskFragment();
            arguments.putLong(AddProjectFragment.ARG_MAX_PROJECT_ID, mMaxProjectId);
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.add_project_fragment, fragment).commit();
        } else { // If we're in Portrait mode
            Intent detailIntent = new Intent(getActivity(), AddProjectActivity.class);
            detailIntent.putExtra(AddProjectFragment.ARG_MAX_PROJECT_ID, mMaxProjectId);
            startActivity(detailIntent);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void getProjectsFromFirebase(final FirebaseUser user) {
        if (user != null) {
            final String[] columns = new String[] {"_id", "project_name", "project_date", "project_color",
                    "project_task_number", "project_task_done"};
            final MatrixCursor matrixProjectCursor = new MatrixCursor(columns);
            firstCall = true;
            oldProjectPosition = 0;
            mTodayTaskNumber = 0;
            mWeekTaskNumber = 0;

            final DatabaseReference checkIfNoDataRef = mFirebaseDatabaseRef.child("users").child(user.getUid());
            checkIfNoDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.hasChild("projects")) {
                        noData = true;
                        swipeRefreshLayout.setRefreshing(false);
                        projectsLoader.setVisibility(View.INVISIBLE);

                        todayNumberTasks.setText(String.valueOf(mTodayTaskNumber));
                        weekNumberTasks.setText(String.valueOf(mWeekTaskNumber));

                        showNoProjectMessage();
                        mergeProjectCursor = new MergeCursor(new Cursor[] { matrixProjectCursor });
                        projectAdapter = new ProjectAdapter(getActivity(), ProjectListFragment.this,
                                ProjectListFragment.this, ProjectListFragment.this, ProjectListFragment.this,
                                projectTaskNumberList, projectTaskDoneList);
                        projectAdapter.setCursor(mergeProjectCursor);
                        projectRecyclerView.setAdapter(projectAdapter);
                    } else {
                        noData = false;
                        showProjectDataView();
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

            if (!noData) {
                final DatabaseReference projectRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects");
                projectRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                        Project project = dataSnapshot.getValue(Project.class);

                        matrixProjectCursor.addRow(new Object[]{project.id, project.name, project.date, project.color, project.taskNumber, project.taskDone});
                        displayLoadedProjects(user, matrixProjectCursor);
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
            }

            final DatabaseReference maxProjectIdRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxProjectId");
            maxProjectIdRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMaxProjectId = (long) dataSnapshot.getValue();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
            final DatabaseReference userNameRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("userName");
            userNameRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mUserName = (String) dataSnapshot.getValue();
                    userNamePass.onUserNamePass(mUserName);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    public void displayLoadedProjects(FirebaseUser user, MatrixCursor matrixProjectCursor) {
        mergeProjectCursor = new MergeCursor(new Cursor[] { matrixProjectCursor });

        projectList = new ArrayList<>();
        projectIdList = new ArrayList<>();

        for (int pos = 0; pos < mergeProjectCursor.getCount(); pos++) {
            mergeProjectCursor.moveToPosition(pos);
            projectList.add(mergeProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
            projectIdList.add(mergeProjectCursor.getString(Contract.ProjectEntry.POSITION_ID));
        }

        mergeProjectCursor.moveToLast();
        String projectId = mergeProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
        int projectPosition = mergeProjectCursor.getPosition();

        getTasksFromFirebase(user, projectId, projectPosition, mergeProjectCursor, false);
        currentProjectId = projectId;
    }

    public void getTasksFromFirebase(final FirebaseUser user, final String projectId,
                                     final int projectPosition, final MergeCursor mergeProjectCursor,
                                     final boolean deleteTasks) {
        if (user != null) {
            final String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                    "task_status", "task_priority", "task_reminder_date"};
            final MatrixCursor matrixTaskCursor = new MatrixCursor(columns);

            if (!deleteTasks) {
                displayDelayedData(mergeProjectCursor, 1000);
            }

            final DatabaseReference taskNumberRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks");
            taskNumberRef.orderByChild("projectId").equalTo(projectId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                    Task task = dataSnapshot.getValue(Task.class);

                    matrixTaskCursor.addRow(new Object[]{task.id, task.projectId, task.name,
                            task.date, task.status, task.priority, task.reminderDate});

                    if (!deleteTasks) {
                        displayTasksNumbers(projectPosition, matrixTaskCursor, mergeProjectCursor);
                    } else {
                        deleteSelectedTasks(user, matrixTaskCursor);
                    }
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    public void displayTasksNumbers(int projectPosition, MatrixCursor matrixTaskCursor,
                                    MergeCursor mergeProjectCursor) {
        swipeRefreshLayout.setRefreshing(false);

        mergeTaskCursor = new MergeCursor(new Cursor[]{matrixTaskCursor});

        if (firstCall && projectPosition == 0) {
            oldProjectPosition = projectPosition;
        }

        if (projectPosition > oldProjectPosition) {
            if (firstCall) { // case where there are no tasks on the first projects
                for (int i = 0; i < projectPosition - oldProjectPosition; i++) {
                    projectTaskNumberList.add(oldProjectPosition + i, "0");
                    projectTaskDoneList.add(oldProjectPosition + i, "0");
                }
            } else {
                for (int i = 1; i < projectPosition - oldProjectPosition; i++) {
                    projectTaskNumberList.add(oldProjectPosition + i, "0");
                    projectTaskDoneList.add(oldProjectPosition + i, "0");
                }
            }
            oldProjectPosition = projectPosition;
        }

        if (projectPosition >= projectTaskNumberList.size()) {
            projectTaskNumberList.add(projectPosition, String.valueOf(mergeTaskCursor.getCount()));
        } else {
            projectTaskNumberList.set(projectPosition, String.valueOf(mergeTaskCursor.getCount()));
        }

        int taskDoneCount = 0;
        for (int j=0; j < mergeTaskCursor.getCount(); j++) {
            mergeTaskCursor.moveToPosition(j);
            if (mergeTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS).equals("1")) {
                taskDoneCount++;
            }
        }

        if (projectPosition >= projectTaskDoneList.size()) {
            projectTaskDoneList.add(projectPosition, String.valueOf(taskDoneCount));
        } else {
            projectTaskDoneList.set(projectPosition, String.valueOf(taskDoneCount));
        }

        // If the task's date is today or during the week, add 1 to the number of today's/week's tasks
        String taskDate = mergeTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_DATE);
        String taskStatus = mergeTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        long currentDateInMillis = calendar.getTimeInMillis();
        if (DateFormatter.isTaskDateToday(taskDate, currentDateInMillis) && taskStatus.equals("0")) {
            mTodayTaskNumber += 1;
        }
        if (DateFormatter.isTaskDateInWeek(taskDate, currentDateInMillis) && taskStatus.equals("0")) {
            mWeekTaskNumber += 1;
        }

        displayDelayedData(mergeProjectCursor, 500);

        firstCall = false;
    }

    public void displayDelayedData(final MergeCursor mergeProjectCursor, int timeLapse) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
                projectAdapter = new ProjectAdapter(getActivity(), ProjectListFragment.this,
                        ProjectListFragment.this, ProjectListFragment.this, ProjectListFragment.this, projectTaskNumberList, projectTaskDoneList);
                projectAdapter.setCursor(mergeProjectCursor);
                projectRecyclerView.setAdapter(projectAdapter);

                projectsLoader.setVisibility(View.INVISIBLE);

                todayNumberTasks.setText(String.valueOf(mTodayTaskNumber));
                weekNumberTasks.setText(String.valueOf(mWeekTaskNumber));
            }
        }, timeLapse);
    }

    private void openTodayOrWeekView(String option) {
        int orientation = getResources().getConfiguration().orientation;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (isTablet && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();

            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, projectList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_ID_LIST, projectIdList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);

            if (option.equals("week")) {
                TaskListFragment.mWeekView = true;
                arguments.putString(TaskListFragment.ARG_ITEM_WEEK, option);
            } else {
                TaskListFragment.mTodayView = true;
                arguments.putString(TaskListFragment.ARG_ITEM_TODAY, option);
            }

            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Detail Activity
            Intent detailIntent = new Intent(getActivity(), TaskActivity.class);

            detailIntent.putExtra(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            detailIntent.putExtra(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
            detailIntent.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST, projectList);
            detailIntent.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_ID_LIST, projectIdList);

            if (option.equals("week")) {
                TaskListFragment.mWeekView = true;
                detailIntent.putExtra(TaskListFragment.ARG_ITEM_WEEK, option);
            } else {
                TaskListFragment.mTodayView = true;
                detailIntent.putExtra(TaskListFragment.ARG_ITEM_TODAY, option);
            }
            startActivity(detailIntent);
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
        if (mergeProjectCursor.getCount() > 0) {
            //mProjectCursor.moveToPosition(position);
            mergeProjectCursor.moveToPosition(position);

            //mProject = new Project(mProjectCursor);
            mProject = new Project(mergeProjectCursor);
        }

        switch(button) {
            case "item":
                if (projectList.size()>0) {
                    openProject(mProject.id, mProject.name, mProject.taskNumber, mProject.taskDone, position);
                }
                break;
            case "edit":
                editProject(mProject.id, mProject.name, mProject.color, mProject.date, mProject.taskNumber, mProject.taskDone);
                break;
            case "delete":
                deleteProject(mProject.id);
                break;
            case "add_project":
                addNewProjectButton();
                break;
        }
    }

    /**
     * OpenProject method. Displays the task list of the project selected in a Task List Fragment
     * @param projectId the ID of the project
     * @param projectName the name of the project
     * @param position the adapter position of the selected project
     */
    private void openProject(String projectId, String projectName, String projectTaskNumber, String projectTaskDone, int position) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape Mode, open the project in a new fragment
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(TaskListFragment.ARG_ITEM_ID, projectId);
            arguments.putString(TaskListFragment.ARG_ITEM_NAME, projectName);
            arguments.putInt(TaskListFragment.ARG_ITEM_POSITION, position);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_LIST, projectList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_ID_LIST, projectIdList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            arguments.putStringArrayList(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new activity
            Intent detailIntent = new Intent(getActivity(), TaskActivity.class);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_ID, projectId);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_NAME, projectName);
            detailIntent.putExtra(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, projectTaskNumberList);
            detailIntent.putExtra(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, projectTaskDoneList);
            String positionStr = String.valueOf(position);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_POSITION, positionStr);
            detailIntent.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST, projectList);
            detailIntent.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_ID_LIST, projectIdList);
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
    public void editProject(String projectId, String projectName, String projectColor, String projectDate, String projectTaskNumber, String projectTaskDone) {
        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Tablet Landscape Mode, create a new fragment
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(AddProjectFragment.ARG_ITEM_ID, projectId);
            arguments.putString(AddProjectFragment.ARG_ITEM_NAME, projectName);
            arguments.putString(AddProjectFragment.ARG_ITEM_COLOR, projectColor);
            arguments.putString(AddProjectFragment.ARG_ITEM_DATE, projectDate);
            arguments.putString(AddProjectFragment.ARG_ITEM_TASK_NUMBER, projectTaskNumber);
            arguments.putString(AddProjectFragment.ARG_ITEM_TASK_DONE, projectTaskDone);
            arguments.putLong(AddProjectFragment.ARG_MAX_PROJECT_ID, mMaxProjectId);
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
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_TASK_NUMBER, projectTaskNumber);
            detailIntent.putExtra(AddProjectFragment.ARG_ITEM_TASK_DONE, projectTaskDone);
            detailIntent.putExtra(AddProjectFragment.ARG_MAX_PROJECT_ID, mMaxProjectId);
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

//        // Delete Project on local storage
//        getActivity().getContentResolver().delete(
//                Contract.ProjectEntry.CONTENT_URI,
//                Contract.ProjectEntry._ID + " = ?",
//                mSelectionArgs);

        // Delete Project on the Firebase database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects").child(projectId).removeValue();
            getTasksFromFirebase(user, projectId, 0, null, true);
        }

        // Delete all the Project's tasks on the Firebase database
        // Get all the Project's tasks
//        Cursor taskCursor = getContext().getContentResolver().query(
//                Contract.TaskEntry.CONTENT_URI,
//                MAIN_TASKS_PROJECTION,
//                Contract.TaskEntry.COLUMN_TASK_PROJECT_ID + " = ?",
//                mSelectionArgs,
//                null);
//        // Delete the tasks one by one on Firebase
//        if (user != null && taskCursor != null) {
//            for (int pos = 0; pos < taskCursor.getCount(); pos++) {
//                taskCursor.moveToPosition(pos);
//                String taskId = taskCursor.getString(Contract.TaskEntry.POSITION_ID);
//                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks").child(taskId).removeValue();
//            }
//            taskCursor.close();
//        }
//
//        // Delete all the project's tasks on local storage
//        getActivity().getContentResolver().delete(
//                Contract.TaskEntry.CONTENT_URI,
//                Contract.TaskEntry.COLUMN_TASK_PROJECT_ID + " = ?",
//                mSelectionArgs);

        getProjectsFromFirebase(user);
    }

    public void deleteSelectedTasks(FirebaseUser user, MatrixCursor matrixTaskCursor) {

        MergeCursor mergeCursor = new MergeCursor(new Cursor[]{matrixTaskCursor});

        for (int i=0; i < mergeCursor.getCount(); i++) {
            mergeCursor.moveToPosition(i);
            String taskId = mergeCursor.getString(Contract.TaskEntry.POSITION_ID);
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks").child(taskId).removeValue();
        }
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
                String selection = Contract.ProjectEntry._ID;

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
                    projectIdList.add(mergeProjectCursor.getString(Contract.ProjectEntry.POSITION_ID));
                    projectTaskNumberList.add(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_TASK_NUMBER));
                    projectTaskDoneList.add(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_TASK_DONE));
                }

                if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

                projectsLoader.setVisibility(View.INVISIBLE);

                if (data != null) {
                    if (data.getCount() != 0) {;
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
                    String taskToday = mTaskCursorToday.getString(Contract.TaskEntry.POSITION_ID);
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
                    String taskWeek = mTaskCursorWeek.getString(Contract.TaskEntry.POSITION_ID);
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
        errorMessageLogo.setVisibility(View.INVISIBLE);
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
        //projectRecyclerView.setVisibility(View.INVISIBLE);
        // Show the no project message
        errorMessageLogo.setBackground(getResources().getDrawable(R.drawable.ic_brightness_high_black_24dp));
        errorMessage.setText(R.string.no_project_message);
        errorMessage.setVisibility(View.VISIBLE);
        errorMessageLogo.setVisibility(View.VISIBLE);
    }

    /**
     * When the user pulls the swipe refresh feature
     */
    @Override
    public void onRefresh() {
        //getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        //getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
        //getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);

        if (mergeProjectCursor != null) {
            mergeProjectCursor.close();
        }
        if (mergeTaskCursor != null) {
            mergeTaskCursor.close();
        }

        //projectList = new ArrayList<String>();
        //projectIdList = new ArrayList<String>();
        projectTaskNumberList = new ArrayList<String>();
        projectTaskDoneList = new ArrayList<String>();
        projectsHasTasksList = new ArrayList<String>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        getProjectsFromFirebase(user);
    }

    @Override
    public void onResume() {
        super.onResume();
        //getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        //getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_TODAY_LOADER, null, ProjectListFragment.this);
        //getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_WEEK_LOADER, null, ProjectListFragment.this);

        if (mergeProjectCursor != null) {
            mergeProjectCursor.close();
        }
        if (mergeTaskCursor != null) {
            mergeTaskCursor.close();
        }

        //projectList = new ArrayList<String>();
        //projectIdList = new ArrayList<String>();
        projectTaskNumberList = new ArrayList<String>();
        projectTaskDoneList = new ArrayList<String>();
        projectsHasTasksList = new ArrayList<String>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        getProjectsFromFirebase(user);
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
        if (mergeProjectCursor != null) {
            mergeProjectCursor.close();
        }
        if (mergeTaskCursor != null) {
            mergeTaskCursor.close();
        }
    }
}