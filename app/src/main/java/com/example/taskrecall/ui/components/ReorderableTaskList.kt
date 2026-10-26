package com.example.taskrecall.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * 单个区域内的拖拽排序列表。
 *
 * 只负责一个区内部：调用方为每个区分别使用一个实例，因此任务无法跨越区。
 * 两种拖动方式：
 * 1. 长按卡片任意位置后拖动；
 * 2. 按住右侧三条杠手柄拖动。
 *
 * 松手时根据被拖卡片中心落在哪个位置，计算新索引并回调 onReorder。
 * 顺序由 ViewModel 持久化。
 */
@Composable
fun <T> ReorderableTaskList(
    items: List<T>,
    itemKey: (T) -> Long,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (
        item: T,
        cardModifier: Modifier,
        handleModifier: Modifier,
        isDragging: Boolean,
    ) -> Unit,
) {
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rootTopInWindow by remember { mutableFloatStateOf(0f) }
    val itemBounds = remember { mutableStateMapOf<Long, ItemBounds>() }

    val localItems = remember { items.toMutableStateList() }
    LaunchedEffect(items) {
        // 拖拽过程中忽略外部刷新，避免列表在手势中途跳动。
        if (draggingId == null) {
            localItems.clear()
            localItems.addAll(items)
        }
    }

    val haptics = LocalHapticFeedback.current

    fun startDrag(id: Long) {
        if (draggingId != null) return
        draggingId = id
        dragOffsetY = 0f
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun dragBy(delta: Float) {
        if (draggingId != null) dragOffsetY += delta
    }

    fun finishDrag() {
        val id = draggingId ?: return
        val fromIndex = localItems.indexOfFirst { itemKey(it) == id }
        val bounds = itemBounds[id]
        if (fromIndex >= 0 && bounds != null) {
            val draggedCenter = bounds.top + bounds.height / 2f + dragOffsetY
            val others = localItems.filterIndexed { index, _ -> index != fromIndex }
            val beforeCount = others.count { other ->
                val otherBounds = itemBounds[itemKey(other)] ?: return@count false
                (otherBounds.top + otherBounds.height / 2f) < draggedCenter
            }
            if (beforeCount != fromIndex) {
                val moved = localItems.removeAt(fromIndex)
                localItems.add(beforeCount, moved)
                onReorder(localItems.map(itemKey))
            }
        }
        draggingId = null
        dragOffsetY = 0f
    }

    fun cancelDrag() {
        draggingId = null
        dragOffsetY = 0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                rootTopInWindow = coordinates.positionInWindow().y
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        localItems.forEach { item ->
            val id = itemKey(item)
            key(id) {
                val isDragging = draggingId == id

                val longPressModifier = Modifier.pointerInput(id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startDrag(id) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragBy(dragAmount.y)
                        },
                        onDragEnd = { finishDrag() },
                        onDragCancel = { cancelDrag() },
                    )
                }

                val handleModifier = Modifier.pointerInput(id) {
                    detectDragGestures(
                        onDragStart = { startDrag(id) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragBy(dragAmount.y)
                        },
                        onDragEnd = { finishDrag() },
                        onDragCancel = { cancelDrag() },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            itemBounds[id] = ItemBounds(
                                top = coordinates.positionInWindow().y - rootTopInWindow,
                                height = coordinates.size.height.toFloat(),
                            )
                        }
                        .zIndex(if (isDragging) 2f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                        }
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (isDragging) dragOffsetY.roundToInt() else 0,
                            )
                        },
                ) {
                    itemContent(item, longPressModifier, handleModifier, isDragging)
                }
            }
        }
    }
}

private data class ItemBounds(
    val top: Float,
    val height: Float,
)
