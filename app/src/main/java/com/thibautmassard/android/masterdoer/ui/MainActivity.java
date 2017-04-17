package com.thibautmassard.android.masterdoer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;

import com.thibautmassard.android.masterdoer.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.today_item) LinearLayout todayItem;
    @BindView(R.id.week_item) LinearLayout weekItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

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
    }

    private void openTodayOrWeekView(String option) {
        int orientation = getResources().getConfiguration().orientation;
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (isTablet && orientation == ORIENTATION_LANDSCAPE) {
            Bundle arguments = new Bundle();

            if (option.equals("week")) {
                TaskListFragment.mWeekView = true;
                arguments.putString(TaskListFragment.ARG_ITEM_WEEK, option);
            } else {
                TaskListFragment.mTodayView = true;
                arguments.putString(TaskListFragment.ARG_ITEM_TODAY, option);
            }

            TaskListFragment fragment = new TaskListFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_list_fragment, fragment).commit();
        } else { // If we're in Portrait mode, open a new Detail Activity
            Intent detailIntent = new Intent(MainActivity.this, TaskActivity.class);

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

    // ** MENUS ** // Unused for now
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

}
