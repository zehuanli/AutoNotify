package com.upbad.apps.autonotify.db;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {PackageData.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PackageDAO packageDAO();
}
