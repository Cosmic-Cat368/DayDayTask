package com.example.taskrecall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskrecall.domain.TaskStatus

/**
 * 统一任务卡片。
 *
 * 规格六：status != PENDING 时卡片灰度。
 * 这里只把卡片底色和文字降级，状态圆点仍保持绿/红，方便看出完成或失败。
 */
@Composable
fun TaskCard(
    title: String,
    status: TaskStatus,
    metaText: String?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    statusEnabled: Boolean = true,
    /** 传入后右侧显示三条杠拖动手柄；历史区传 null。 */
    dragHandleModifier: Modifier? = null,
    /** 正在被拖动时使用高亮颜色和圆角阴影，避免出现白色方块。 */
    isDragging: Boolean = false,
) {
    val finished = status != TaskStatus.PENDING
    val containerColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer
        finished -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isDragging -> MaterialTheme.colorScheme.onPrimaryContainer
        finished -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                isDragging -> 8.dp
                finished -> 0.dp
                else -> 1.dp
            },
        ),
        border = if (isDragging) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(
                status = status,
                onToggle = onToggle,
                enabled = statusEnabled,
            )

            Spacer(modifier = Modifier.width(2.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!metaText.isNullOrBlank()) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (dragHandleModifier != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .then(dragHandleModifier)
                        .clickable { }
                        .semantics { contentDescription = "拖动排序" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}
