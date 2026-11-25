package com.todayddl.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.todayddl.app.data.local.dao.TagDao
import com.todayddl.app.data.local.dao.TaskDao
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, TagEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao

    companion object {
        private const val DB_NAME = "today_ddl.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN customReminderDateEpochDay INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN customReminderTimeMinutes INTEGER")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
