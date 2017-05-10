package com.thibautmassard.android.masterdoer;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by thib146 on 01/05/2017.
 */

public class MasterDoer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}