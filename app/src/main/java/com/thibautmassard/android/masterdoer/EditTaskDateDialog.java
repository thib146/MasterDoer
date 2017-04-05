package com.thibautmassard.android.masterdoer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 05/04/2017.
 */

public class EditTaskDateDialog extends DialogFragment {

    @BindView(R.id.edit_date_datepicker) DatePicker datePicker;

    public interface OnSetDueDate {
        void onSetDueDate(String taskDueDate);
    }
    OnSetDueDate setDueDate;

    private Activity mActivity;

    private String mTaskDueDate;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.edit_task_date_dialog, null);

        ButterKnife.bind(this, custom);

        builder.setView(custom);

        builder.setMessage(getString(R.string.edit_date_dialog_datepicker_title));
        builder.setPositiveButton(getString(R.string.edit_date_dialog_set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String day = String.valueOf(datePicker.getDayOfMonth());
                        String month = String.valueOf(datePicker.getMonth()+1);
                        String year = String.valueOf(datePicker.getYear());

                        mTaskDueDate = day + "/" + month + "/" + year;

                        setDueDate.onSetDueDate(mTaskDueDate);
                        //AddTaskFragment.mTaskDueDate = mTaskDueDate;
                        dismissAllowingStateLoss();
                    }
                });
        builder.setNegativeButton(getString(R.string.edit_date_dialog_cancel), null);

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
            mActivity = (Activity) context;
            this.setDueDate = (OnSetDueDate) mActivity;
        }
    }
}