package com.thibautmassard.android.masterdoer.data;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thibautmassard.android.masterdoer.R;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 29/03/2017.
 */

public class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private Cursor mProjectCursor;
    private Project mProject;

    private DatabaseReference mFirebaseDatabaseRef;

    // Set up the click handlers
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandler;
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerDelete;
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerEdit;
    private final ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerAddProject;

    private static int VIEW_TYPE_FOOTER = 146;
    private static int VIEW_TYPE_CELL = 147;

    private ArrayList<String> projectTaskNumberList;
    private ArrayList<String> projectTaskDoneList;

    // This object helps save/restore the swiped/unswiped state of each view
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    public static final String[] MAIN_TASKS_PROJECTION = {
            Contract.TaskEntry._ID,
            Contract.TaskEntry.COLUMN_TASK_PROJECT_ID,
            Contract.TaskEntry.COLUMN_TASK_NAME,
            Contract.TaskEntry.COLUMN_TASK_DATE,
            Contract.TaskEntry.COLUMN_TASK_STATUS,
            Contract.TaskEntry.COLUMN_TASK_PRIORITY,
            Contract.TaskEntry.COLUMN_TASK_REMINDER_DATE
    };

    // Constructor
    public ProjectAdapter (Context context, ProjectAdapter.ProjectAdapterOnClickHandler clickHandler,
                           ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerDelete,
                           ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerEdit,
                           ProjectAdapter.ProjectAdapterOnClickHandler clickHandlerAddProject,
                           ArrayList<String> projectTaskNumberList, ArrayList<String> projectTaskDoneList) {
        this.context = context;
        this.clickHandler = clickHandler;
        this.clickHandlerDelete = clickHandlerDelete;
        this.clickHandlerEdit = clickHandlerEdit;
        this.clickHandlerAddProject = clickHandlerAddProject;
        this.projectTaskNumberList = projectTaskNumberList;
        this.projectTaskDoneList = projectTaskDoneList;
        viewBinderHelper.setOpenOnlyOne(true); // Forces only one item to be swiped at a time
    }

    public void setCursor(Cursor cursor) {
        mProjectCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == mProjectCursor.getCount()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_CELL;
    }

    public void setTasksNumbers(ArrayList<String> projectTaskNumberList, ArrayList<String> projectTaskDoneList) {
        this.projectTaskNumberList = projectTaskNumberList;
        this.projectTaskDoneList = projectTaskDoneList;
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mProjectCursor != null) {
            count = mProjectCursor.getCount()+1;
        }
        return count;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        View item;
        if (viewType == VIEW_TYPE_CELL) {
            item = LayoutInflater.from(context).inflate(R.layout.project_swipe_list_item, parent, false);
            return new ProjectAdapter.ProjectViewHolder(item);
        } else {
            item = LayoutInflater.from(context).inflate(R.layout.add_project_item, parent, false);
            return new ProjectAdapter.FooterViewHolder(item);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof ProjectViewHolder) {
            ProjectViewHolder projectHolder = (ProjectViewHolder) holder;

            mProjectCursor.moveToPosition(position);
            mProject = new Project(mProjectCursor);

            // Save/restore the swiped/unswiped state.
            viewBinderHelper.bind(projectHolder.projectSwipeRevealLayout, mProject.id);

            // Set the Project Name
            projectHolder.projectName.setText(mProject.name);

            // Set the project creation date
            projectHolder.projectCreationDate.setText(DateFormatter.formatDate(mProject.date));

            // Set the Project's bullet point color
            setProjectColor(mProject.color, projectHolder);

            // Get the numbers of tasks of this project (stored in the pos 0), and the number of DONE tasks (stored in the pos 1)
            //int[] taskNumbers = getProjectTaskNumbers(mProject.id);

            // Get the number of tasks (total + done) from Firebase database
            //String taskNumber = mProject.taskNumber;
            //String taskDone = mProject.taskDone;

            String taskNumber = "0";
            String taskDone = "0";
            if (position < projectTaskNumberList.size()) {
                taskNumber = projectTaskNumberList.get(position);
            }
            if (position < projectTaskDoneList.size()) {
                taskDone = projectTaskDoneList.get(position);
            }

            //String taskNumber = projectTaskNumberList.get(0);
            //String taskDone = projectTaskDoneList.get(0);

            float percentage;

            //if (taskNumbers[0]!=0) {
            if (!taskNumber.equals("0")) {
                //percentage = taskNumbers[1] * 100 / taskNumbers[0];
                percentage = Integer.valueOf(taskDone) * 100 / Integer.valueOf(taskNumber);
            } else {
                percentage = 0;
            }

            String percentageStrValue = String.format("%d", (long) percentage); // Used to color the number's background
            String percentageStr = String.format("%d", (long) percentage) + "%"; // Used to display

            // Set the number of tasks remaining
            //String tasksCount = taskNumbers[1] + "/" + taskNumbers[0];
            String tasksCount = taskDone + "/" + taskNumber;
            projectHolder.projectTasksRemaining.setText(tasksCount);

            // Set the percentage completed
            projectHolder.projectPercentageCompleted.setText(percentageStr);

            // Set the percentage background color
            int percentageInt = Integer.parseInt(percentageStrValue);
            setProjectPercentageColor(percentageInt, projectHolder);

            projectHolder.bind(mProject.id);
        } else {
            FooterViewHolder footerHolder = (FooterViewHolder) holder;

            footerHolder.addProjectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickHandlerAddProject.onClick("0", "add_project");
                }
            });
        }
    }

    private int[] getProjectTaskNumbers(String projectId) {
        int taskNumber, taskNumberDone = 0;

        String[] mSelectionArgs = {""};
        mSelectionArgs[0] = projectId;

        // Get all the tasks of this project in order to get the number and display it in the indicator
        Cursor taskCursor = context.getContentResolver().query(
                Contract.TaskEntry.CONTENT_URI,
                MAIN_TASKS_PROJECTION,
                Contract.TaskEntry.COLUMN_TASK_PROJECT_ID + "=?",
                mSelectionArgs,
                null);

        ArrayList<String> taskStatusList = new ArrayList<String>();

        // Add all the tasks to get their number + count all the DONE tasks
        for (int pos = 0; pos < taskCursor.getCount(); pos++) {
            taskCursor.moveToPosition(pos);
            String taskStatus = taskCursor.getString(Contract.TaskEntry.POSITION_TASK_STATUS);
            taskStatusList.add(taskStatus);
            if (taskStatus.equals("1")) {
                taskNumberDone++;
            }
        }

        taskNumber = taskStatusList.size();

        int[] numbers = {taskNumber, taskNumberDone};

        taskCursor.close();
        return numbers;
    }

    private void setProjectColor(String projectColor, ProjectAdapter.ProjectViewHolder holder) {
        switch (projectColor) {
            case "green":
                holder.projectBulletPoint.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_fiber_manual_record_black_24dp, null));
                break;
            case "blue":
                holder.projectBulletPoint.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_fiber_manual_record_blue_24dp, null));
                break;
            case "yellow":
                holder.projectBulletPoint.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_fiber_manual_record_yellow_24dp, null));
                break;
            case "red":
                holder.projectBulletPoint.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_fiber_manual_record_red_24dp, null));
                break;

        }
    }

    private void setProjectPercentageColor(int percentage, ProjectAdapter.ProjectViewHolder holder) {
        if (percentage < 30) {
            holder.projectPercentageCompleted.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPercentage0_30, null));
            holder.projectPercentageCompleted.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.white, null));
        } else if (percentage > 30 && percentage < 60) {
            holder.projectPercentageCompleted.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPercentage30_60, null));
            holder.projectPercentageCompleted.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.white, null));
        } else if (percentage > 60 && percentage < 80) {
            holder.projectPercentageCompleted.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPercentage60_80, null));
            holder.projectPercentageCompleted.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.black, null));
        } else if (percentage > 80 && percentage < 99) {
            holder.projectPercentageCompleted.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPercentage80_99, null));
            holder.projectPercentageCompleted.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.black, null));
        } else if (percentage == 100) {
            holder.projectPercentageCompleted.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPercentage100, null));
            holder.projectPercentageCompleted.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.white, null));
        }
    }

    public interface ProjectAdapterOnClickHandler {
        void onClick(String projectId, String button);
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.project_bullet_point) ImageView projectBulletPoint;
        @BindView(R.id.project_name) TextView projectName;
        @BindView(R.id.project_creation_date) TextView projectCreationDate;
        @BindView(R.id.project_tasks_remaining) TextView projectTasksRemaining;
        @BindView(R.id.project_percentage_completed) TextView projectPercentageCompleted;
        @BindView(R.id.project_swipe_layout) SwipeRevealLayout projectSwipeRevealLayout;
        @BindView(R.id.project_surface_view_start) LinearLayout projectSurfaceViewStart;
        @BindView(R.id.project_edit_button) ImageView projectEditButton;
        @BindView(R.id.project_delete_button) ImageView projectDeleteButton;

        ProjectViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        /**
         * Handling the different possible clicks (item, edit, delete) on a project item
         * @param projectId project ID
         */
        public void bind (final String projectId) {
            projectSurfaceViewStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                clickHandler.onClick(String.valueOf(pos), "item");
                viewBinderHelper.closeLayout(projectId);
            }
            });

            projectEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    clickHandlerEdit.onClick(String.valueOf(pos), "edit");
                    viewBinderHelper.closeLayout(projectId);
                }
            });

            projectDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    clickHandlerDelete.onClick(String.valueOf(pos), "delete");
                    viewBinderHelper.closeLayout(projectId);
                }
            });
        }

        @Override
        public void onClick(View v) {
        }
    }

    class FooterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.add_project_row) LinearLayout addProjectButton;

        FooterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        }
    }
}
