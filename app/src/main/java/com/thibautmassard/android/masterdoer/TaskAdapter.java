package com.thibautmassard.android.masterdoer;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.thibautmassard.android.masterdoer.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 31/03/2017.
 */

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private final Context context;
    private Cursor mCursor;
    private final TaskAdapter.TaskAdapterOnClickHandler clickHandler;

    private boolean taskStatusBl;

    public int TaskAdapterPosition;

    TaskAdapter (Context context, TaskAdapter.TaskAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;
    }

    void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    String getIdAtPosition(int position) {
        mCursor.moveToPosition(position);
        String id = mCursor.getString(Contract.TaskEntry.POSITION_ID);
        //return mCursor.getString(Contract.TaskEntry.POSITION_Task_ID);
        return mCursor.getString(Contract.TaskEntry.POSITION_ID);
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
    public TaskAdapter.TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.task_list_item, parent, false);

        return new TaskAdapter.TaskViewHolder(item);
    }

    @Override
    public void onBindViewHolder(TaskAdapter.TaskViewHolder holder, final int position) {

        mCursor.moveToPosition(position);

        //holder.TaskBulletPoint.setColorFilter(mCursor.getInt(Contract.TaskEntry.POSITION_TASK_PRIORITY));

        holder.taskName.setText(mCursor.getString(Contract.TaskEntry.POSITION_TASK_NAME));

        holder.taskDueDate.setText(mCursor.getString(Contract.TaskEntry.POSITION_TASK_DATE));

        String taskStatus = mCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
        if (taskStatus.equals("0")) {
            taskStatusBl = false;
        } else if (taskStatus.equals("1")){
            taskStatusBl = true;
        } else {
            taskStatusBl = false;
        }

        holder.taskCheckBox.setChecked(taskStatusBl);

        holder.taskCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCursor.moveToPosition(position);
                int taskIdColumn = mCursor.getColumnIndex(Contract.TaskEntry._ID);
                String taskId = mCursor.getString(taskIdColumn);
                int taskStatus = mCursor.getInt(Contract.TaskEntry.POSITION_TASK_STATUS);
                if (taskStatus == 0) {
                    taskStatus = 1;
                } else {
                    taskStatus = 0;
                }
                clickHandler.onClick(mCursor.getString(taskIdColumn));
                clickHandler.passTaskStatus(taskId, taskStatus);
            }
        });

    }

    interface TaskAdapterOnClickHandler {
        void onClick(String taskId);
        void passTaskStatus(String taskId, int taskStatus);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.task_check_box) CheckBox taskCheckBox;
        @BindView(R.id.task_name) TextView taskName;
        @BindView(R.id.task_date) TextView taskDueDate;

        TaskViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            TaskAdapterPosition = getAdapterPosition();
            mCursor.moveToPosition(TaskAdapterPosition);
            int taskIdColumn = mCursor.getColumnIndex(Contract.TaskEntry.COLUMN_TASK_ID);
            clickHandler.onClick(mCursor.getString(taskIdColumn));
        }
    }
}
