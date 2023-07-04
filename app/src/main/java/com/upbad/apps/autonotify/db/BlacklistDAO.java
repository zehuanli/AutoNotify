package com.upbad.apps.autonotify.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BlacklistDAO {
    @Query("SELECT * FROM " + BlacklistRecord.TABLE_NAME)
    List<BlacklistRecord> getAll();

    @Query("SELECT COUNT(1) FROM " + BlacklistRecord.TABLE_NAME)
    int count();

    @Query("SELECT * FROM " + BlacklistRecord.TABLE_NAME + " WHERE keyword == :keyword")
    BlacklistRecord find(String keyword);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(BlacklistRecord... blacklistRecords);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlacklistRecord blacklistRecord);

    @Delete
    void delete(BlacklistRecord blacklistRecord);

    @Query("DELETE FROM " + BlacklistRecord.TABLE_NAME + " WHERE keyword == :keyword")
    void delete(String keyword);

    @Query("DELETE FROM " + BlacklistRecord.TABLE_NAME)
    void deleteAll();
}
