package com.thibautmassard.android.masterdoer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thibautmassard.android.masterdoer.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

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
    private ProjectAdapter projectAdapter;
    private int mPosition = RecyclerView.NO_POSITION;

    private Activity mActivity;

    public static final int ID_PROJECTS_LOADER = 146;

    private String mProjectName;

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
    public void onClick(String projectId) {
        int position = projectAdapter.projectAdapterPosition;
        mCursor.moveToPosition(position);
        mProjectName = String.valueOf(mCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));
        projectId = String.valueOf(mCursor.getString(Contract.ProjectEntry.POSITION_ID));

        Timber.d("Project clicked: %s", projectId);

        // Get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        // If we're in Landscape Mode, refresh the Detail Fragment with the new data
        if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();
            arguments.putString(TaskListFragment.ARG_ITEM_ID, projectId);
            arguments.putString(TaskListFragment.ARG_ITEM_NAME, mProjectName);
            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Detail Activity
            Intent detailIntent = new Intent(getActivity(), TaskActivity.class);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_ID, projectId);
            detailIntent.putExtra(TaskListFragment.ARG_ITEM_NAME, mProjectName);
            startActivity(detailIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_project_list, container, false);
        ButterKnife.bind(this, view);

        projectAdapter = new ProjectAdapter(getActivity(), this);
        projectRecyclerView.setAdapter(projectAdapter);
        projectRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String projectId = projectAdapter.getIdAtPosition(viewHolder.getAdapterPosition());
                ((MainActivity) mActivity).removeProject(projectId);

                // Send the symbol String to the main activity for removal
                //StockRemoved.onRemoveStock(symbol);
            }
        }).attachToRecyclerView(projectRecyclerView);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the choice is Favorites, load the data from the database
        if (savedInstanceState == null) {
            // Initialize the cursor loader for the Favorites list
            getActivity().getSupportLoaderManager().initLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context; // Store the main activity in mActivity
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
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

//                return new CursorLoader(getActivity(),
//                        Contract.TaskEntry.CONTENT_URI,
//                        MAIN_TASKS_PROJECTION,
//                        Contract.TaskEntry.COLUMN_TASK_ID,
//                        null,
//                        null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (isAdded()) {
            projectAdapter.setCursor(data);
        }
        mCursor = data;

        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

        if (data != null) {
            if (data.getCount() != 0) {
                showProjectDataView();
            } else { // If there are no projects yet
                showNoProjectMessage();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        //Clear the Adapter
        if (isAdded()) {
            projectAdapter.setCursor(null);
        }
    }

    /**
     * This method will make the View for the movie data visible and
     * hide the error message.
     */
    private void showProjectDataView() {
        /* Hide the error message */
        errorMessage.setVisibility(View.INVISIBLE);
        /* Make the movie data visible */
        projectRecyclerView.setVisibility(View.VISIBLE);
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

    /**
     * This method will make the "No Favorites" message visible and hide the movie
     * View.
     */
    private void showNoProjectMessage() {
        /* Hide the current data */
        projectRecyclerView.setVisibility(View.INVISIBLE);
        errorMessage.setText(R.string.no_project_message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRefresh() {
        getActivity().getSupportLoaderManager().restartLoader(ID_PROJECTS_LOADER, null, ProjectListFragment.this);
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
