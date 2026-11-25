package com.todayddl.app.data.local.model

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation
import com.todayddl.app.data.local.entity.TagEntity
import com.todayddl.app.data.local.entity.TaskEntity

@Immutable
data class TaskWithTag(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "id"
    )
    val tag: TagEntity?
)
