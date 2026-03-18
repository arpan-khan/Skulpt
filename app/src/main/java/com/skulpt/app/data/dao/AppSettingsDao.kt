package com.skulpt.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.skulpt.app.data.model.AppSettings

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsLive(): LiveData<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettings)
}
