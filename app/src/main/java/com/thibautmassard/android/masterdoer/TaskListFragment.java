package com.thibautmassard.android.masterdoer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

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

    // Key used to get the movie ID through the fragment creation
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_NAME = "item_name";

    private String mProjectId;
    private String mProjectName;
    private int mTaskStatus;

    private TaskAdapter taskAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private Activity mActivity;

    public static final int ID_TASKS_LOADER = 158;

    public interface OnProjectNamePass {
        public void onProjectName(String projectName);
    }

    OnProjectNamePass projectNamePass;

    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of
     * favorite movies data.
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

    private Cursor mCursor;

    @Override
    public void onClick(String taskId) {
        Timber.d("Task clicked: %s", taskId);
    }

    @Override
    public void passTaskStatus(String taskId, int taskStatus) {
        mTaskStatus = taskStatus;
        //((TaskActivity) mActivity).updateTaskStatus(taskId, taskStatus);

        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = taskId;

        ContentValues taskValue = new ContentValues();
        taskValue.put(Contract.TaskEntry.COLUMN_TASK_STATUS, taskStatus);

        ArrayList<ContentValues> taskValuesContent = new ArrayList<ContentValues>();
        taskValuesContent.add(taskValue);

        getActivity().getContentResolver().update(
                Contract.TaskEntry.CONTENT_URI,
                taskValue,
                Contract.TaskEntry._ID + " = ?",
                mSelectionArgs
                );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_task_list, container, false);
        ButterKnife.bind(this, view);

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        if (bundle != null) { // If the fragment was created in Landscape Mode, get the movie id with the Fragment's arguments
            mProjectId = bundle.getString(TaskListFragment.ARG_ITEM_ID);
            TaskActivity.mProjectId = mProjectId;
            mProjectName = bundle.getString(TaskListFragment.ARG_ITEM_NAME);
        } else { // If the fragment was created in Portrait mode (intent), get the movie id with the Intent's extra
            mProjectId = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_ID);
            TaskActivity.mProjectId = mProjectId;
            mProjectName = intentThatStartedThatActivity.getStringExtra(TaskListFragment.ARG_ITEM_NAME);
        }

        TaskActivity.mProjectName = mProjectName;

        taskAdapter = new TaskAdapter(getActivity(), this);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String taskId = taskAdapter.getIdAtPosition(viewHolder.getAdapterPosition());
                ((TaskActivity) mActivity).removeTask(taskId);

                // Send the symbol String to the main activity for removal
                //StockRemoved.onRemoveStock(symbol);
            }
        }).attachToRecyclerView(taskRecyclerView);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the choice is Favorites, load the data from the database
        if (savedInstanceState == null) {
            // Initialize the cursor loader for the Favorites list
            getActivity().getSupportLoaderManager().initLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context; // Store the main activity in mActivity
            //this.projectNamePass = (OnProjectNamePass) mActivity;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case ID_TASKS_LOADER:
                /* URI for all rows of project data in our project table */
                Uri tasksQueryUri = Contract.TaskEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = Contract.TaskEntry.COLUMN_TASK_DATE + " ASC";
                // Selection of all the data
                String selection = Contract.TaskEntry.COLUMN_TASK_PROJECT_ID;

                String[] mSelectionArgs = {""};
                mSelectionArgs[0] = mProjectId;

                return new CursorLoader(getActivity(),
                        tasksQueryUri,
                        MAIN_TASKS_PROJECTION,
                        selection + "=?",
                        mSelectionArgs,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (isAdded()) {
            taskAdapter.setCursor(data);
        }
        mCursor = data;

        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

        if (data != null) {
            if (data.getCount() != 0) {
                showTaskDataView();
            } else { // If there are no projects yet
                showNoTaskMessage();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        //Clear the Adapter
        if (isAdded()) {
            taskAdapter.setCursor(null);
        }
    }

    /**
     * This method will make the View for the movie data visible and
     * hide the error message.
     */
    private void showTaskDataView() {
        /* Hide the error message */
        errorMessage.setVisibility(View.INVISIBLE);
        /* Make the movie data visible */
        taskRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the tasks
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

    /**
     * This method will make the "No Tasks" message visible and hide the tasks
     * View.
     */
    private void showNoTaskMessage() {
        /* Hide the current data */
        taskRecyclerView.setVisibility(View.INVISIBLE);
        errorMessage.setText(R.string.no_task_message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRefresh() {
        getActivity().getSupportLoaderManager().restartLoader(ID_TASKS_LOADER, null, TaskListFragment.this);
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
    }
}
