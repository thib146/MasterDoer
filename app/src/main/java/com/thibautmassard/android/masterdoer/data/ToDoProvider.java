package com.thibautmassard.android.masterdoer.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by thib146 on 29/03/2017.
 */

public class ToDoProvider extends ContentProvider {

    public static final int CODE_PROJECTS = 100;
    public static final int CODE_PROJECTS_WITH_ID = 101;
    public static final int CODE_TASKS = 102;
    public static final int CODE_TASKS_WITH_ID = 103;

    /*
     * The URI Matcher used by this content provider.
     */
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private DBHelper mOpenHelper;

    public static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = Contract.AUTHORITY;

        /* This URI is content://com.thibautmassard.android.masterdoer/projects/ */
        matcher.addURI(authority, Contract.PATH_PROJECT, CODE_PROJECTS);

        /* This URI is something like content://com.thibautmassard.android.masterdoer/projects/1932839084 */
        matcher.addURI(authority, Contract.PATH_PROJECT_WITH_ID, CODE_PROJECTS_WITH_ID);

        /* This URI is content://com.thibautmassard.android.masterdoer/tasks/ */
        matcher.addURI(authority, Contract.PATH_TASK, CODE_TASKS);

        /* This URI is something like content://com.thibautmassard.android.masterdoer/tasks/9432859384 */
        matcher.addURI(authority, Contract.PATH_TASK_WITH_ID, CODE_TASKS_WITH_ID);

        return matcher;
    }

    /**
     * Initialization of the content provider on startup.
     *
     * @return true if the provider was successfully loaded, false otherwise
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DBHelper(getContext());
        return true;
    }

    /**
     * Handles requests to insert a set of new rows.
     *
     * @param uri    The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     *               This must not be {@code null}.
     *
     * @return The number of values that were inserted.
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {

            case CODE_PROJECTS:
                db.beginTransaction();
                int rowsInsertedProj = 0;
                try {
                    for (ContentValues value : values) {
                        long projectId = value.getAsLong(Contract.ProjectEntry.COLUMN_PROJECT_ID);

                        long _id = db.insert(Contract.ProjectEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            rowsInsertedProj++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                if (rowsInsertedProj > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return rowsInsertedProj;

            case CODE_TASKS:
                db.beginTransaction();
                int rowsInsertedTask = 0;
                try {
                    for (ContentValues value : values) {
                        long taskId = value.getAsLong(Contract.TaskEntry.COLUMN_TASK_ID);

                        long _id = db.insert(Contract.TaskEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            rowsInsertedTask++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                if (rowsInsertedTask > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return rowsInsertedTask;

            default:
                return super.bulkInsert(uri, values);
        }
    }

    /**
     * Handles query requests from clients.
     *
     * @param uri           The URI to query
     * @param projection    The list of columns to put into the cursor. If null, all columns are
     *                      included.
     * @param selection     A selection criteria to apply when filtering rows. If null, then all
     *                      rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *                      the values from selectionArgs, in order that they appear in the
     *                      selection.
     * @param sortOrder     How the rows in the cursor should be sorted.
     * @return A Cursor containing the results of the query.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor;

        switch (sUriMatcher.match(uri)) {

            case CODE_PROJECTS: {
                cursor = mOpenHelper.getReadableDatabase().query(
                        Contract.ProjectEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }

            case CODE_PROJECTS_WITH_ID: {

                cursor = mOpenHelper.getReadableDatabase().query(
                        Contract.ProjectEntry.TABLE_NAME,
                        projection,
                        Contract.ProjectEntry.COLUMN_PROJECT_ID + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }

            case CODE_TASKS: {
                cursor = mOpenHelper.getReadableDatabase().query(
                        Contract.TaskEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }

            case CODE_TASKS_WITH_ID: {

                cursor = mOpenHelper.getReadableDatabase().query(
                        Contract.TaskEntry.TABLE_NAME,
                        projection,
                        Contract.TaskEntry._ID + " = " + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(selection) ? "AND (" +selection + ')' : ""),
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Deletes data at a given URI with optional arguments for more fine tuned deletions.
     *
     * @param uri           The full URI to query
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs Used in conjunction with the selection statement
     * @return The number of rows deleted
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        int numRowsDeleted;

        if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {

            case CODE_PROJECTS:
                numRowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        Contract.ProjectEntry.TABLE_NAME,
                        selection,
                        selectionArgs);

                break;

            case CODE_TASKS:
                numRowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        Contract.TaskEntry.TABLE_NAME,
                        selection,
                        selectionArgs);

                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        /* If we actually deleted any rows, notify that a change has occurred to this URI */
        if (numRowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numRowsDeleted;
    }

    /**
     * getType method. Unused in this app
     *
     * @param uri the URI to query.
     * @return MIME type string, or null if there is no type.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException("Not implementing getType.");
    }

    /**
     * insert method
     *
     * @param uri    The URI of the insertion request. This must not be null.
     * @param values A set of column_name/value pairs to add to the database.
     *               This must not be null
     * @return The URI for the newly inserted item.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
        long id;
        String idStr;

        switch (sUriMatcher.match(uri)) {
            case CODE_PROJECTS:
                id = db.insert(
                        Contract.ProjectEntry.TABLE_NAME,
                        null,
                        values
                );
                idStr = String.valueOf(id);
                returnUri = Contract.TaskEntry.CONTENT_URI.buildUpon().appendPath(idStr).build();
                break;

            case CODE_TASKS:
                id = db.insert(
                        Contract.TaskEntry.TABLE_NAME,
                        null,
                        values
                );
                idStr = String.valueOf(id);
                returnUri = Contract.TaskEntry.CONTENT_URI.buildUpon().appendPath(idStr).build();
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI:" + uri);
        }

        Context context = getContext();
        if (context != null){
            context.getContentResolver().notifyChange(uri, null);
        }

        return returnUri;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int returnInt;

        switch (sUriMatcher.match(uri)) {
            case CODE_PROJECTS:
                returnInt = db.update(
                        Contract.ProjectEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;

            case CODE_TASKS:
                returnInt = db.update(
                        Contract.TaskEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI:" + uri);
        }

        Context context = getContext();
        if (context != null){
            context.getContentResolver().notifyChange(uri, null);
        }

        //return returnUri;
        return returnInt;
    }

    /**
     * Method specifically to assist the testing framework in running smoothly.
     */
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
