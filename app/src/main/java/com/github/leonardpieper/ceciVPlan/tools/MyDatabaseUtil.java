package com.github.leonardpieper.ceciVPlan.tools;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Prevents:
 * com.google.firebase.database.DatabaseException:
 * Calls to setPersistenceEnabled() must be made before any other usage of FirebaseDatabase instance.
 *
 *
 * --> Always call MyDatabaseUtil.getDatabase() instead of FirebaseDatabase.getInstance()
 */
public class MyDatabaseUtil {
    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }

        return mDatabase;

    }
}
