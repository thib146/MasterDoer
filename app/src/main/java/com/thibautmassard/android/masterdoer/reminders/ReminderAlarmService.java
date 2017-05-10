package com.thibautmassard.android.masterdoer.reminders;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Project;
import com.thibautmassard.android.masterdoer.data.Task;
import com.thibautmassard.android.masterdoer.ui.TaskActivity;
import com.thibautmassard.android.masterdoer.ui.TaskListFragment;
import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static com.thibautmassard.android.masterdoer.ui.ProjectListFragment.MAIN_PROJECTS_PROJECTION;
import static com.thibautmassard.android.masterdoer.ui.TaskListFragment.MAIN_TASKS_PROJECTION;

/**
 * Created by thib146 on 13/04/2017.
 */

public class ReminderAlarmService extends IntentService {
    private static final String TAG = ReminderAlarmService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 42;

    private static String EXT_TASK_ID = "task_id";
    private static String EXT_TASK_PROJECT_ID = "task_project_id";

    private boolean firstCall;
    private int oldProjectPosition;
    private int mTodayTaskNumber;
    private int mWeekTaskNumber;
    private long mMaxProjectId;

    private MergeCursor mergeAlarmProjectCursor;
    private MergeCursor mergeTaskCursorLocal;
    private MergeCursor mergeTaskCursorGlobal;

    private String mTaskId;
    private String mTaskName;
    private String mTaskProjectId;
    private String mProjectPosition;

    private ArrayList<String> mProjectList;
    private ArrayList<String> mProjectIdList;
    private ArrayList<String> mProjectTaskNumberList;
    private ArrayList<String> mProjectTaskDoneList;

    private DatabaseReference mFirebaseDatabaseRef;

    private boolean firstProjectCall;
    private boolean firstAlarmCall;

    //This is a deep link intent, and needs the task stack
    public static PendingIntent getReminderPendingIntent(Context context, ArrayList<String> data) {
        Intent action = new Intent(context, ReminderAlarmService.class);
        action.putExtra(EXT_TASK_ID, data.get(0));
        action.putExtra(EXT_TASK_PROJECT_ID, data.get(1));
        return PendingIntent.getService(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public ReminderAlarmService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mTaskId = intent.getStringExtra(EXT_TASK_ID);
        mTaskProjectId = intent.getStringExtra(EXT_TASK_PROJECT_ID);

        if (mergeAlarmProjectCursor != null) {
            mergeAlarmProjectCursor.close();
        }
        if (mergeTaskCursorLocal != null) {
            mergeTaskCursorLocal.close();
        }
        if (mergeTaskCursorGlobal != null) {
            mergeTaskCursorGlobal.close();
        }

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        firstProjectCall = true;
        getProjectsFromFirebase(user);
    }

    public void getProjectsFromFirebase(final FirebaseUser user) {
        if (user != null) {
            final String[] columns = new String[] {"_id", "project_name", "project_date", "project_color",
                    "project_task_number", "project_task_done"};
            final MatrixCursor matrixAlarmProjectCursor = new MatrixCursor(columns);
            mProjectTaskNumberList = new ArrayList<>();
            mProjectTaskDoneList = new ArrayList<>();
            firstCall = true;
            firstAlarmCall = true;
            oldProjectPosition = 0;
            mTodayTaskNumber = 0;
            mWeekTaskNumber = 0;

            final DatabaseReference projectRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("projects");
            projectRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                    Project project = dataSnapshot.getValue(Project.class);

                    matrixAlarmProjectCursor.addRow(new Object[]{project.id, project.name, project.date, project.color, project.taskNumber, project.taskDone});

                    //if (firstProjectCall) {
                        handleLoadedProjects(user, matrixAlarmProjectCursor);
                        //firstProjectCall = false;
                    //}
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

            final DatabaseReference maxProjectIdRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxProjectId");
            maxProjectIdRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mMaxProjectId = (long) dataSnapshot.getValue();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    public void handleLoadedProjects(FirebaseUser user, MatrixCursor matrixAlarmProjectCursor) {
        mergeAlarmProjectCursor = new MergeCursor(new Cursor[] { matrixAlarmProjectCursor });

        mProjectList = new ArrayList<>();
        mProjectIdList = new ArrayList<>();

        for (int pos = 0; pos < mergeAlarmProjectCursor.getCount(); pos++) {
            mergeAlarmProjectCursor.moveToPosition(pos);
            mProjectList.add(mergeAlarmProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
            mProjectIdList.add(mergeAlarmProjectCursor.getString(Contract.ProjectEntry.POSITION_ID));
        }

        mergeAlarmProjectCursor.moveToLast();
        String projectId = mergeAlarmProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
        int projectPosition = mergeAlarmProjectCursor.getPosition();

        getTasksFromFirebase(user, projectId, projectPosition, mergeAlarmProjectCursor);
        matrixAlarmProjectCursor.close();
    }

    public void getTasksFromFirebase(final FirebaseUser user, final String projectId,
                                     final int projectPosition, final MergeCursor mergeAlarmProjectCursor) {
        if (user != null) {
            final String[] columns = new String[] {"_id", "task_project_id", "task_name", "task_date",
                    "task_status", "task_priority", "task_reminder_date"};
            //final MatrixCursor matrixAlarmTaskCursor = new MatrixCursor(columns);

            final DatabaseReference taskNumberRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("tasks");
            taskNumberRef.orderByChild("projectId").equalTo(projectId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                    Task task = dataSnapshot.getValue(Task.class);

                    final MatrixCursor matrixAlarmTaskCursor = new MatrixCursor(columns);
                    matrixAlarmTaskCursor.addRow(new Object[]{task.id, task.projectId, task.name,
                            task.date, task.status, task.priority, task.reminderDate});

                    handleTasksNumbers(projectPosition, matrixAlarmTaskCursor);
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

    public void handleTasksNumbers(int projectPosition, MatrixCursor matrixAlarmTaskCursor) {
        if (!firstCall) {
            mergeTaskCursorGlobal = new MergeCursor(new Cursor[]{mergeTaskCursorGlobal, matrixAlarmTaskCursor});
        } else {
            mergeTaskCursorGlobal = new MergeCursor(new Cursor[]{matrixAlarmTaskCursor});
        }
        mergeTaskCursorLocal = new MergeCursor(new Cursor[]{matrixAlarmTaskCursor});

        if (firstCall && projectPosition == 0) {
            oldProjectPosition = projectPosition;
        }

        if (projectPosition > oldProjectPosition) {
            if (firstCall) { // case where there are no tasks on the first projects
                for (int i = 0; i < projectPosition - oldProjectPosition; i++) {
                    mProjectTaskNumberList.add(oldProjectPosition + i, "0");
                    mProjectTaskDoneList.add(oldProjectPosition + i, "0");
                }
            } else {
                for (int i = 1; i < projectPosition - oldProjectPosition; i++) {
                    mProjectTaskNumberList.add(oldProjectPosition + i, "0");
                    mProjectTaskDoneList.add(oldProjectPosition + i, "0");
                }
            }
            oldProjectPosition = projectPosition;
        }

        if (projectPosition >= mProjectTaskNumberList.size()) {
            mProjectTaskNumberList.add(projectPosition, String.valueOf(mergeTaskCursorLocal.getCount()));
        } else {
            mProjectTaskNumberList.set(projectPosition, String.valueOf(mergeTaskCursorLocal.getCount()));
        }

        int taskDoneCount = 0;
        for (int j=0; j < mergeTaskCursorLocal.getCount(); j++) {
            mergeTaskCursorLocal.moveToPosition(j);
            if (mergeTaskCursorLocal.getString(Contract.TaskEntry.POSITION_TASK_STATUS).equals("1")) {
                taskDoneCount++;
            }
        }
        mergeTaskCursorLocal.moveToFirst();

        if (projectPosition >= mProjectTaskDoneList.size()) {
            mProjectTaskDoneList.add(projectPosition, String.valueOf(taskDoneCount));
        } else {
            mProjectTaskDoneList.set(projectPosition, String.valueOf(taskDoneCount));
        }

        handleDelayedData(3000);

        firstCall = false;
        matrixAlarmTaskCursor.close();
        mergeTaskCursorGlobal.close();
    }

    public void handleDelayedData(int timeLapse) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (firstAlarmCall) {
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    //Display a notification to view the task details
                    //TODO : add fragment possibility for the Two-Pane Mode
                    Intent action = new Intent(ReminderAlarmService.this, TaskActivity.class); // Intent directing to the Detail Activity

                    // Get the task's project name and ID in order to
                    // launch the project's task list when clicking on the notification
                    //String[] projectData = getProjectData(uri);

                    // Get the task's name
                    for (int i = 0; i < mergeTaskCursorGlobal.getCount(); i++) {
                        mergeTaskCursorGlobal.moveToPosition(i);
                        String currentTaskId = mergeTaskCursorGlobal.getString(Contract.TaskEntry.POSITION_ID);
                        if (mTaskId.equals(currentTaskId)) {
                            mTaskName = mergeTaskCursorGlobal.getString(Contract.TaskEntry.POSITION_TASK_NAME);
                            mTaskProjectId = mergeTaskCursorGlobal.getString(Contract.TaskEntry.POSITION_TASK_PROJECT_ID);
                        }
                    }

                    // Get the project's position
                    for (int i = 0; i < mergeAlarmProjectCursor.getCount(); i++) {
                        mergeAlarmProjectCursor.moveToPosition(i);
                        String currentProjectId = mergeAlarmProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
                        if (mTaskProjectId.equals(currentProjectId)) {
                            mProjectPosition = String.valueOf(i);
                        }
                    }

                    String projectId = mProjectIdList.get(Integer.valueOf(mProjectPosition));
                    String projectName = mProjectList.get(Integer.valueOf(mProjectPosition));

                    action.putExtra(TaskListFragment.ARG_ITEM_ID, projectId);
                    action.putExtra(TaskListFragment.ARG_ITEM_NAME, projectName);
                    action.putExtra(TaskListFragment.ARG_ITEM_POSITION, mProjectPosition);
                    action.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_LIST, mProjectList);
                    action.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_ID_LIST, mProjectIdList);
                    action.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_TASK_NUMBER_LIST, mProjectTaskNumberList);
                    action.putStringArrayListExtra(TaskListFragment.ARG_PROJECT_TASK_DONE_LIST, mProjectTaskDoneList);

                    // Create the Pending Intent
                    PendingIntent operation = TaskStackBuilder.create(ReminderAlarmService.this)
                            .addNextIntentWithParentStack(action)
                            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Grab the task description
                    //String description = getTaskDescription(uri);

                    // Create the notification
                    Notification note = new NotificationCompat.Builder(ReminderAlarmService.this)
                            .setContentTitle(getString(R.string.reminder_title))
                            .setContentText(mTaskName)
                            .setSmallIcon(R.mipmap.icon)
                            .setContentIntent(operation)
                            .setAutoCancel(true)
                            .build();

                    // Set the notification
                    manager.notify(NOTIFICATION_ID, note);

                    mergeAlarmProjectCursor.close();
                    mergeTaskCursorLocal.close();
                    mergeTaskCursorGlobal.close();
                    firstAlarmCall = false;
                }
            }
        }, timeLapse);
    }
}