package com.thibautmassard.android.masterdoer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 01/04/2017.
 */

public class AddTaskDialog extends DialogFragment {

    @BindView(R.id.dialog_task) EditText dialogTask;

    private Activity mActivity;
    private TaskListFragment mMainFragment;

    private String mTaskName;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //mMainFragment = (TaskListFragment) getSupportParentFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.add_task_dialog, null);

        ButterKnife.bind(this, custom);

        dialogTask.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mTaskName = dialogTask.getText().toString();
                //new checkIfTaskIsValid().execute(mProject);
                addTask();
                return true;
            }
        });
        builder.setView(custom);

        builder.setMessage(getString(R.string.task_dialog_title));
        builder.setPositiveButton(getString(R.string.task_dialog_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mTaskName = dialogTask.getText().toString();
                        //new checkIfProjectIsValid().execute(mProject);
                        addTask();
                    }
                });
        builder.setNegativeButton(getString(R.string.task_dialog_cancel), null);

        Dialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context; // Store the main activity in mActivity for the Add Task function to work properly
            //mMainFragment = getFragmentManager().findFragmentById(R.id.task_list_fragment);
        }
    }

    // Add a stock to the main Stock List activity
    private void addTask() {
        //TaskListFragment mainFragment = (TaskListFragment) getSupportFragmentManager().findFragmentById(R.id.task_list_fragment);
        ((TaskActivity) mActivity).addTask(dialogTask.getText().toString());
        //mMainFragment.addTask(dialogTask.getText().toString());
        //mainFragment.addTask(dialogTask.getText().toString());
        dismissAllowingStateLoss();
    }

}
