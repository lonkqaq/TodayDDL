package com.todayddl.app.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: Long
) {
    companion object {
        val PRESET_COLORS: List<Long> = listOf(
            0xFF2563EB, // 蓝
            0xFF059669, // 绿
            0xFFDC2626, // 红
            0xFF7C3AED, // 紫
            0xFFD97706, // 琥珀
            0xFF0891B2, // 青
            0xFFDB2777, // 粉
            0xFF475569  // 灰蓝
        )

        fun defaultTags(): List<TagEntity> = listOf(
            TagEntity(name = "作业", colorHex = 0xFF2563EB),
            TagEntity(name = "实验报告", colorHex = 0xFF059669),
            TagEntity(name = "考试", colorHex = 0xFFDC2626),
            TagEntity(name = "项目", colorHex = 0xFF7C3AED)
        )
    }
}
