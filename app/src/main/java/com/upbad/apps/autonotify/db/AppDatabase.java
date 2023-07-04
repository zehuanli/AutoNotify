package com.upbad.apps.autonotify.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.upbad.apps.autonotify.Config;

@androidx.room.Database(entities = {PackageData.class, BlacklistRecord.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract PackageDAO packageDAO();
    public abstract BlacklistDAO blacklistDAO();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, Config.DATABASE_FILENAME).allowMainThreadQueries().build();
        }
        return instance;
    }
}
