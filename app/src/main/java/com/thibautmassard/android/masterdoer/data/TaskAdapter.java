package com.thibautmassard.android.masterdoer.data;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.thibautmassard.android.masterdoer.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 31/03/2017.
 */

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private final Context context;
    private Cursor mTaskCursor;
    
    // Click handlers
    private final TaskAdapter.TaskAdapterOnClickHandler clickHandler;
    private final TaskAdapter.TaskAdapterOnClickHandler clickHandlerCheck;
    private final TaskAdapter.TaskAdapterOnClickHandler clickHandlerEdit;
    private final TaskAdapter.TaskAdapterOnClickHandler clickHandlerDelete;

    private boolean taskStatusBl;

    public int TaskAdapterPosition;

    // This object helps save/restore the swiped/unswiped state of each view
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    // Task Adapter Constructor
    public TaskAdapter (Context context, TaskAdapter.TaskAdapterOnClickHandler clickHandler,
                        TaskAdapter.TaskAdapterOnClickHandler clickHandlerCheck,
                        TaskAdapter.TaskAdapterOnClickHandler clickHandlerEdit,
                        TaskAdapter.TaskAdapterOnClickHandler clickHandlerDelete) {
        this.context = context;
        this.clickHandler = clickHandler;
        this.clickHandlerCheck = clickHandlerCheck;
        this.clickHandlerEdit = clickHandlerEdit;
        this.clickHandlerDelete = clickHandlerDelete;
        viewBinderHelper.setOpenOnlyOne(true);
    }

    public void setCursor(Cursor cursor) {
        mTaskCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mTaskCursor != null) {
            count = mTaskCursor.getCount();
        }
        return count;
    }

    @Override
    public TaskAdapter.TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.task_swipe_list_item, parent, false);

        return new TaskAdapter.TaskViewHolder(item);
    }

    @Override
    public void onBindViewHolder(TaskAdapter.TaskViewHolder holder, final int position) {

        mTaskCursor.moveToPosition(position);

        String taskId = mTaskCursor.getString(Contract.TaskEntry.POSITION_ID);

        // Save/restore the swiped/unswiped state.
        viewBinderHelper.bind(holder.taskSwipeRevealLayout, taskId);

        // Set the Task priority color
        String taskPriority = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_PRIORITY);
        int priorityColor = setTaskPriorityColor(taskPriority);
        holder.taskPriorityBackColor.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), priorityColor, null));

        // Set the Task's name
        holder.taskName.setText(mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_NAME));

        // Set the Task's Due Date
        holder.taskDueDate.setText(mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_DATE));

        // Set the Task's Status
        String taskStatus = mTaskCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
        switch(taskStatus) {
            case "0":
                taskStatusBl = false;
                break;
            case "1":
                taskStatusBl = true;
                break;
            default:
                taskStatusBl = false;
                break;
        }
        holder.taskCheckBox.setChecked(taskStatusBl);

        holder.bind(taskId);
    }

    private int setTaskPriorityColor(String taskPriority) {
        int priorityColor = 0;
        switch (taskPriority) {
            case "0":
                priorityColor = R.color.colorTaskPriority0;
                break;
            case "1":
                priorityColor = R.color.colorTaskPriority1;
                break;
            case "2":
                priorityColor = R.color.colorTaskPriority2;
                break;
        }
        return priorityColor;
    }

    public interface TaskAdapterOnClickHandler {
        void onClick(String taskId, String button);
        void passTaskStatus(String taskId, int taskStatus);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.task_check_box) CheckBox taskCheckBox;
        @BindView(R.id.task_name) TextView taskName;
        @BindView(R.id.task_date) TextView taskDueDate;
        @BindView(R.id.task_priority_color) ImageView taskPriorityBackColor;
        @BindView(R.id.task_swipe_layout) SwipeRevealLayout taskSwipeRevealLayout;
        @BindView(R.id.task_surface_view_start) LinearLayout taskSurfaceViewStart;
        @BindView(R.id.task_edit_button) ImageView taskEditButton;
        @BindView(R.id.task_delete_button) ImageView taskDeleteButton;

        TaskViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        /**
         * Handles the different clicks (checkbox, edit, delete) on a Task item
         * @param taskId the Task ID we selected
         */
        public void bind (final String taskId) {
            taskCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTaskCursor.moveToPosition(getAdapterPosition());

                    String taskId = mTaskCursor.getString(mTaskCursor.getColumnIndex(Contract.TaskEntry._ID));
                    int taskStatus = mTaskCursor.getInt(Contract.TaskEntry.POSITION_TASK_STATUS);

                    // Change the task status after click and pass the status
                    taskStatus = taskStatus == 0 ? 1 : 0;
                    clickHandler.passTaskStatus(taskId, taskStatus);
                }
            });
            taskEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    clickHandlerEdit.onClick(String.valueOf(pos), "edit");
                    viewBinderHelper.closeLayout(taskId); // Unswipe the item once we've clicked on it
                }
            });

            taskDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    clickHandlerDelete.onClick(String.valueOf(pos), "delete");
                    viewBinderHelper.closeLayout(taskId); // Unswipe the item once we've clicked on it
                }
            });
        }

        @Override
        public void onClick(View v) {
            TaskAdapterPosition = getAdapterPosition();
            mTaskCursor.moveToPosition(TaskAdapterPosition);
            int taskIdColumn = mTaskCursor.getColumnIndex(Contract.TaskEntry.COLUMN_TASK_ID);
            clickHandler.onClick(mTaskCursor.getString(taskIdColumn), "item");
        }
    }
}
