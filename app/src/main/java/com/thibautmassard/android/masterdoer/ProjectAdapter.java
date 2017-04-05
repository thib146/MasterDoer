package com.thibautmassard.android.masterdoer;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thibautmassard.android.masterdoer.data.Contract;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 29/03/2017.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final Context context;
    private Cursor mProjectCursor;
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandler;

    private int mTaskNumber;
    private int mTaskNumberDone;

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

    public int projectAdapterPosition;

    ProjectAdapter (Context context, ProjectAdapter.ProjectAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;
    }

    void setCursor(Cursor cursor) {
        mProjectCursor = cursor;
        notifyDataSetChanged();
    }

    String getIdAtPosition(int position) {
        mProjectCursor.moveToPosition(position);
        String id = mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
        //return mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_ID);
        return mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mProjectCursor != null) {
            count = mProjectCursor.getCount();
        }
        return count;
    }

    @Override
    public ProjectAdapter.ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.project_list_item, parent, false);

        return new ProjectAdapter.ProjectViewHolder(item);
    }

    @Override
    public void onBindViewHolder(ProjectAdapter.ProjectViewHolder holder, int position) {

        mProjectCursor.moveToPosition(position);

        String[] mSelectionArgs = {""};
        String projectId = mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);
        mSelectionArgs[0] = mProjectCursor.getString(Contract.ProjectEntry.POSITION_ID);

        Cursor mTaskCursor = context.getContentResolver().query(
                Contract.TaskEntry.CONTENT_URI,
                MAIN_TASKS_PROJECTION,
                Contract.TaskEntry.COLUMN_TASK_PROJECT_ID + "=?",
                mSelectionArgs,
                null);

        ArrayList<String> taskStatusList = new ArrayList<String>();

        for (int pos = 0; pos < mTaskCursor.getCount(); pos++) {
            mTaskCursor.moveToPosition(pos);
            String taskStatus = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
            taskStatusList.add(taskStatus);
            if (taskStatus.equals("1")) {
                mTaskNumberDone++;
            }
        }

        mTaskNumber = taskStatusList.size();

        float percentage = mTaskNumberDone*100/mTaskNumber;
        String percentageStr = String.format("%d", (long) percentage) + "%";

        //holder.projectBulletPoint.setColorFilter(mProjectCursor.getInt(Contract.ProjectEntry.POSITION_PROJECT_COLOR));

        holder.projectName.setText(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));

        holder.projectCreationDate.setText(mProjectCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_DATE));

        String tasksCount = mTaskNumberDone + "/" + mTaskNumber;

        holder.projectTasksRemaining.setText(tasksCount);

        holder.projectPercentageCompleted.setText(percentageStr);

        mTaskNumber = 0;
        mTaskNumberDone = 0;

    }

    interface ProjectAdapterOnClickHandler {
        void onClick(String projectId);
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.project_bullet_point) ImageView projectBulletPoint;
        @BindView(R.id.project_name) TextView projectName;
        @BindView(R.id.project_creation_date) TextView projectCreationDate;
        @BindView(R.id.project_tasks_remaining) TextView projectTasksRemaining;
        @BindView(R.id.project_percentage_completed) TextView projectPercentageCompleted;

        ProjectViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            projectAdapterPosition = getAdapterPosition();
            mProjectCursor.moveToPosition(projectAdapterPosition);
            int projectIdColumn = mProjectCursor.getColumnIndex(Contract.ProjectEntry.COLUMN_PROJECT_ID);
            clickHandler.onClick(mProjectCursor.getString(projectIdColumn));
        }
    }
}
