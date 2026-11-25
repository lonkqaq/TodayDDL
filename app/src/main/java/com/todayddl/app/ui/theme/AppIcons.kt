package com.todayddl.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight lazy-cached Material-style outlined icons to avoid bundling
 * the multi-megabyte `material-icons-extended` artifact.
 */
object AppIcons {

    val AccessTime: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.AccessTime",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
                reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
                reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.5f, 7f)
                horizontalLineTo(11f)
                verticalLineToRelative(6f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }
        }.build()
    }

    val Alarm: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Alarm",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.5f, 8f)
                horizontalLineTo(11f)
                verticalLineToRelative(6f)
                lineToRelative(4.75f, 2.85f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4f, -2.37f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.34f, 1.81f)
                lineToRelative(4.61f, 3.85f)
                lineToRelative(-1.28f, 1.54f)
                lineToRelative(-4.61f, -3.84f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.66f, 1.81f)
                lineToRelative(1.28f, 1.54f)
                lineTo(3.34f, 7.19f)
                lineToRelative(-1.28f, -1.54f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                reflectiveCurveToRelative(4.02f, 9f, 9f, 9f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
                reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                close()
            }
        }.build()
    }

    val NotificationsOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.NotificationsOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 16f)
                verticalLineToRelative(-5f)
                curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
                verticalLineTo(4f)
                curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(0.68f)
                curveToRelative(-0.24f, 0.06f, -0.47f, 0.15f, -0.69f, 0.23f)
                lineTo(18f, 13.1f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(5.41f, 3.35f)
                lineTo(4f, 4.76f)
                lineToRelative(2.81f, 2.81f)
                curveTo(6.29f, 8.57f, 6f, 9.74f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(14.24f)
                lineToRelative(1.74f, 1.74f)
                lineToRelative(1.41f, -1.41f)
                lineTo(5.41f, 3.35f)
                close()
                moveTo(16f, 17f)
                horizontalLineTo(8f)
                verticalLineToRelative(-6f)
                curveToRelative(0f, -0.68f, 0.12f, -1.32f, 0.34f, -1.9f)
                lineTo(16f, 16.76f)
                close()
            }
        }.build()
    }

    val BatteryCharging: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.BatteryCharging",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.67f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(2f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(2f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0f, 1.34f, -0.6f, 1.34f, -1.33f)
                verticalLineTo(5.33f)
                curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
                close()
                moveTo(11f, 20f)
                verticalLineToRelative(-5.5f)
                horizontalLineTo(9f)
                lineTo(13f, 7f)
                verticalLineToRelative(5.5f)
                horizontalLineToRelative(2f)
                lineTo(11f, 20f)
                close()
            }
        }.build()
    }
}
