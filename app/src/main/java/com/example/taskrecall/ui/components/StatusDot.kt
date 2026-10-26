package com.example.taskrecall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskrecall.domain.TaskStatus

/** 规格六：完成绿色、失败红色。 */
val StatusGreen = Color(0xFF2E7D32)
val StatusRed = Color(0xFFC62828)

/**
 * 任务卡片左侧的三态圆点。
 *
 * 点击循环 PENDING -> DONE -> FAILED -> PENDING。
 * 历史记录区域会把 enabled 设为 false，避免修改过去的状态。
 */
@Composable
fun StatusDot(
    status: TaskStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill = when (status) {
        TaskStatus.PENDING -> Color.Transparent
        TaskStatus.DONE -> StatusGreen
        TaskStatus.FAILED -> StatusRed
    }
    val symbol = when (status) {
        TaskStatus.PENDING -> ""
        TaskStatus.DONE -> "✓"
        TaskStatus.FAILED -> "×"
    }
    val description = "状态：${status.displayName()}，点击切换"

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(fill)
                .then(
                    if (status == TaskStatus.PENDING) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (symbol.isNotEmpty()) {
                Text(
                    text = symbol,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
