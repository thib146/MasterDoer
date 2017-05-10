package com.thibautmassard.android.masterdoer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.thibautmassard.android.masterdoer.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

/**
 * Created by thib146 on 06/05/2017.
 */

public class ProfileFragment extends Fragment {

    @BindView(R.id.profile_user_name) TextView mUserNameTextView;
    @BindView(R.id.profile_user_email) TextView mUserEmailTextView;
    @BindView(R.id.profile_picture) ImageView mUserPictureImageView;
    @BindView(R.id.profile_edit_button) TextView mEditButton;

    private DatabaseReference mFirebaseDatabaseRef;

    private String mUserName;
    private String mUserEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, view);

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the device's current orientation
                int orientation = getResources().getConfiguration().orientation;

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // If we're in Landscape Mode, display the task list fragment again
                if (getResources().getBoolean(R.bool.isTablet) && orientation == ORIENTATION_LANDSCAPE) {
                    Bundle arguments = new Bundle();
                    arguments.putString(EditProfileFragment.ARG_USER_NAME, mUserName);
                    arguments.putString(EditProfileFragment.ARG_USER_EMAIL, mUserEmail);
                    //TODO add picture url
                    EditProfileFragment fragment = new EditProfileFragment();
                    fragment.setArguments(arguments);
                    getActivity().getFragmentManager().beginTransaction()
                            .replace(R.id.edit_profile_fragment, fragment).commit();
                } else { // If we're in portrait mode, close the activity
                    Intent editProfileIntent = new Intent(getActivity(), EditProfileActivity.class);
                    editProfileIntent.putExtra(EditProfileFragment.ARG_USER_NAME, mUserName);
                    editProfileIntent.putExtra(EditProfileFragment.ARG_USER_EMAIL, mUserEmail);
                    //TODO add picture url
                    startActivity(editProfileIntent);
                }
            }
        });

        return view;
    }

    public void getUserData(final FirebaseUser user) {
        if (user != null) {
            final DatabaseReference userNameRef = mFirebaseDatabaseRef.child("users").child(user.getUid()).child("userName");
            userNameRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mUserName = (String) dataSnapshot.getValue();
                    displayUserData(user);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
            //TODO Load user picture
        }
    }

    public void displayUserData(FirebaseUser user) {
        mUserEmail = user.getEmail();

        mUserNameTextView.setText(mUserName);
        mUserEmailTextView.setText(mUserEmail);

        //TODO display user picture
    }

    @Override
    public void onResume() {
        super.onResume();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        getUserData(user);
    }
}
