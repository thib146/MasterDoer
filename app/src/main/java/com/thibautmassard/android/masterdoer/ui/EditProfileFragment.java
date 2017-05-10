package com.thibautmassard.android.masterdoer.ui;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.data.Task;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by thib146 on 06/05/2017.
 */

public class EditProfileFragment extends Fragment {

    @BindView(R.id.edit_profile_user_name) EditText mUserNameEditText;
    @BindView(R.id.edit_profile_user_email) EditText mUserEmailEditText;
    @BindView(R.id.edit_profile_picture) ImageView mUserPictureImageView;
    @BindView(R.id.edit_profile_button_cancel) Button mButtonCancel;
    @BindView(R.id.edit_profile_button_update) Button mButtonUpdate;

    public static final String ARG_USER_NAME = "user_name";
    public static final String ARG_USER_EMAIL= "user_email";
    public static final String ARG_USER_PICTURE_URL = "user_picture_url";

    private DatabaseReference mFirebaseDatabaseRef;

    private String mUserName;
    private String mUserEmail;
    private String mUserPictureUrl;

    public ProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        ButterKnife.bind(this, view);

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Get the intent that started the activity
        Intent intentThatStartedThatActivity = getActivity().getIntent();
        Bundle bundle = getArguments();

        // Get the data from the intent
        if (bundle != null) { // If the fragment was created in Landscape Mode, get the project id with the Fragment's arguments
            mUserName = bundle.getString(EditProfileFragment.ARG_USER_NAME);
            mUserEmail = bundle.getString(EditProfileFragment.ARG_USER_EMAIL);
            mUserPictureUrl = bundle.getString(EditProfileFragment.ARG_USER_PICTURE_URL);
        } else { // If the fragment was created in Portrait mode (intent), get the project id with the Intent's extra
            mUserName = intentThatStartedThatActivity.getStringExtra(EditProfileFragment.ARG_USER_NAME);
            mUserEmail = intentThatStartedThatActivity.getStringExtra(EditProfileFragment.ARG_USER_EMAIL);
            mUserPictureUrl = intentThatStartedThatActivity.getStringExtra(EditProfileFragment.ARG_USER_PICTURE_URL);
        }

        mUserNameEditText.setText(mUserName);
        mUserEmailEditText.setText(mUserEmail);

        mButtonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUserName = mUserNameEditText.getText().toString();
                String newUserEmail = mUserEmailEditText.getText().toString();

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user!=null) {
                    if (!newUserName.equals("") && !newUserName.equals(mUserName)) {
                        mFirebaseDatabaseRef.child("users").child(user.getUid()).child("userName").setValue(newUserName);
                        getActivity().finish();
                    }
                    if (!newUserEmail.equals("") && !newUserEmail.equals(mUserEmail)) {
                        showProgressDialog();
                        user.updateEmail(newUserEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                                hideProgressDialog();
                                Toast.makeText(getActivity(), "Email changed", Toast.LENGTH_SHORT).show();
                                getActivity().finish();
                            }
                        });
                    }

                }
                //TODO: add fragment management
            }
        });

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return view;
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }
}
