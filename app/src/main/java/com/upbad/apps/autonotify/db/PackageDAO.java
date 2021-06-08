package com.upbad.apps.autonotify.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PackageDAO {
    @Query("SELECT * FROM " + PackageData.TABLE_NAME)
    List<PackageData> getAll();

    @Query("SELECT * FROM " + PackageData.TABLE_NAME + " WHERE packageName == :packageName AND userId == :userId")
    PackageData find(String packageName, int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(PackageData... packageDatas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PackageData packageData);

    @Delete
    void delete(PackageData packageData);

    @Query("DELETE FROM " + PackageData.TABLE_NAME + " WHERE packageName == :packageName AND userId == :userId")
    void delete(String packageName, int userId);
}
