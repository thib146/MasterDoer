package com.thibautmassard.android.masterdoer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.support.v4.app.Fragment;
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
import android.widget.TimePicker;

import com.thibautmassard.android.masterdoer.R;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 13/04/2017.
 */

public class EditTaskReminderDialog extends DialogFragment {

    @BindView(R.id.edit_reminder_datepicker) DatePicker datePicker;
    @BindView(R.id.edit_reminder_timepicker) TimePicker timePicker;

    // Listener used when the user sets the reminder date and clicks on the "Set" button
    public interface OnSetReminderDate {
        void onSetReminderDate(String reminderDate, String reminderDay,
                               String reminderMonth, String reminderYear,
                               String reminderHour, String reminderMinute);
    }
    OnSetReminderDate setReminderDate;

    private String mTaskReminderDate;
    private String mTaskReminderTime;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View custom = inflater.inflate(R.layout.edit_task_reminder_dialog, null);

        ButterKnife.bind(this, custom);

        builder.setView(custom);

        // TODO: remove the keyboard
        //datePicker.setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);
        //timePicker.setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);

        // Set the minimum authorized date on the datepicker as Today
        Calendar c = Calendar.getInstance();
        datePicker.setMinDate(c.getTimeInMillis());

        builder.setMessage(getString(R.string.edit_reminder_dialog_datepicker_title));
        builder.setPositiveButton(getString(R.string.edit_reminder_dialog_set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String day = String.valueOf(datePicker.getDayOfMonth());
                        String month = String.valueOf(datePicker.getMonth()+1);
                        String year = String.valueOf(datePicker.getYear());

                        // Get the hour and minute of the timepicker
                        String hour, minutes;
                        if (Build.VERSION.SDK_INT >= 23 ) {
                            hour = String.valueOf(timePicker.getHour());
                            minutes = String.valueOf(timePicker.getMinute());
                        } else {
                            hour = String.valueOf(timePicker.getCurrentHour());
                            minutes = String.valueOf(timePicker.getCurrentMinute());
                        }

                        mTaskReminderTime = hour + ":" + minutes;

                        mTaskReminderDate = day + "/" + month + "/" + year + " " + mTaskReminderTime;

                        setReminderDate.onSetReminderDate(mTaskReminderDate, day, String.valueOf(datePicker.getMonth()), year, hour, minutes);
                        dismissAllowingStateLoss();
                    }
                });
        builder.setNegativeButton(getString(R.string.edit_reminder_dialog_cancel), null);

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

        // Get the parent fragment to use the OnSetReminderDate listener
        Fragment mainFragment = getFragmentManager().findFragmentById(R.id.add_task_fragment);
        this.setReminderDate = (OnSetReminderDate) mainFragment;
    }
}