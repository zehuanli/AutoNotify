package com.upbad.apps.autonotify.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(primaryKeys = {"keyword"})
public class BlacklistRecord {

    public static final String TABLE_NAME = "BlacklistRecord";

    @ColumnInfo(name = "keyword")
    @NonNull
    public String keyword;

    public BlacklistRecord() {
    }

    @Ignore
    public BlacklistRecord(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return keyword;
    }
}
