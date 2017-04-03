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

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 29/03/2017.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final Context context;
    private Cursor mCursor;
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandler;

    public int projectAdapterPosition;

    ProjectAdapter (Context context, ProjectAdapter.ProjectAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;
    }

    void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    String getIdAtPosition(int position) {
        mCursor.moveToPosition(position);
        String id = mCursor.getString(Contract.ProjectEntry.POSITION_ID);
        //return mCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_ID);
        return mCursor.getString(Contract.ProjectEntry.POSITION_ID);
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mCursor != null) {
            count = mCursor.getCount();
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

        mCursor.moveToPosition(position);

        //holder.projectBulletPoint.setColorFilter(mCursor.getInt(Contract.ProjectEntry.POSITION_PROJECT_COLOR));

        holder.projectName.setText(mCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_NAME));

        holder.projectCreationDate.setText(mCursor.getString(Contract.ProjectEntry.POSITION_PROJECT_DATE));

        // TODO: add tasks remaining numbers
        //holder.projectTasksRemaining.setText();

        // TODO: add percentage completed
        //holder.projectPercentageCompleted.setText();

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
            mCursor.moveToPosition(projectAdapterPosition);
            int projectIdColumn = mCursor.getColumnIndex(Contract.ProjectEntry.COLUMN_PROJECT_ID);
            clickHandler.onClick(mCursor.getString(projectIdColumn));
        }
    }
}
