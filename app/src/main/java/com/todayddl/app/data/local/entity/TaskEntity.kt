package com.todayddl.app.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["isCompleted"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val dueDateEpochDay: Long,
    val dueTimeMinutes: Int? = null,
    val tagId: Long? = null,
    val reminderOption: String = "SAME_DAY_0800",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null
)
