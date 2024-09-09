import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

sealed class UIComponent {
    var id: Int = 0
    var layoutCoordinates: LayoutCoordinates? = null

    val sizeOffset
        get() = layoutCoordinates?.let { Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
            ?: Offset.Zero
    val offset: Offset
        get() = layoutCoordinates?.positionInRoot() ?: Offset.Zero

    data class SimpleComponent(val type: String) : UIComponent()
    data class LayoutComponent(
        val type: String,
        val children: MutableList<UIComponent> = mutableStateListOf(),
        var isFocused: Boolean = false,
    ) : UIComponent()
}

@Composable
fun UIBuilder() {
    var draggedItem by remember { mutableStateOf<UIComponent?>(null) }
    var draggedItemOffset by remember { mutableStateOf(Offset.Zero) }
    val uiItems = remember {
        listOf("Button", "TextField", "Checkbox", "RadioButton", "Switch", "Row", "Column", "Group")
    }
//    var previewCardOffSet by remember { mutableStateOf(Offset.Zero) }
    var previewCardComponent by remember { mutableStateOf<UIComponent>(UIComponent.LayoutComponent("Card")) }
    val uiItemsOffset = remember {
        mutableMapOf<String, Offset>()
    }
    val previewItems = remember { mutableStateListOf<UIComponent>() }

    fun addComponentToLayout(component: UIComponent, targetLayout: UIComponent.LayoutComponent) {
        println("Adding $component to $targetLayout")
        targetLayout.children.add(component)
    }

    fun removedAllFocused(currentSelectedIndex: Int = -1) {
        previewItems.forEachIndexed { index, component ->
            if (index == currentSelectedIndex) return
            println("Removing $component")
            if (component is UIComponent.LayoutComponent) {
//                previewItems[index] = component.copy(isFocused = false)
                if (component.isFocused)
                    previewItems[index] = component.copy(isFocused = false)
                if (component.children.isNotEmpty()) {
                    removeFocusedUpdateRecursive(
                        component.children
                    )
                }
            }
        }
    }

    fun setFocus(index: Int, draggedItemOffset: Offset) {
        if (index < 0) return
        with(previewItems[index]) {
            if (this is UIComponent.LayoutComponent) {
                previewItems[index] = copy(
                    isFocused = when {
                        children.isEmpty() -> true
                        !setFocusedUpdateRecursive(
                            children,
                            draggedItemOffset
                        ) && touches(draggedItemOffset) -> true

                        else -> false

                    }
                )
            }
        }
    }

    fun onItemDragEnd(
        item: String,
    ) {
        draggedItem?.let { component ->
            val targetLayout =
                getFocusedComponent(previewItems, draggedItemOffset)
            println("Drag ended for $item, targetLayout: $targetLayout")
            if (targetLayout != null) {
                addComponentToLayout(component, targetLayout)
            } else if (previewCardComponent.touches(draggedItemOffset)) {
                previewItems.add(component)
            }
        }
        removedAllFocused()
        draggedItem = null
        draggedItemOffset = Offset.Zero
    }

    // Show items
    Row(modifier = Modifier.fillMaxSize()) {
        // UI Items List
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp), elevation = 4.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)
            ) {
                items(uiItems) { item ->
                    OutlinedButton(onClick = { /* No action on click */ },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .onGloballyPositioned {
                                uiItemsOffset[item] = it.localToRoot(Offset.Zero)
                            }.pointerInput(Unit) {
                                detectDragGestures(onDragStart = {
                                    println("Drag started for $item")
                                    uiItemsOffset[item]?.let {
                                        draggedItemOffset = it
                                    }
                                    draggedItem = if (item in listOf("Row", "Column", "Group")) {
                                        UIComponent.LayoutComponent(item)
                                    } else {
                                        UIComponent.SimpleComponent(item)
                                    }
                                    println("Dragged item ${draggedItem.toString()}")
                                }, onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedItemOffset += dragAmount
                                    val focusedItemIndex = previewItems.indexOfLast {
                                        it is UIComponent.LayoutComponent && it.touches(
                                            draggedItemOffset
                                        )
                                    }
                                    removedAllFocused(focusedItemIndex)
                                    setFocus(focusedItemIndex, draggedItemOffset)
                                }, onDragEnd = {
                                    onItemDragEnd(
                                        item
                                    )
                                })
                            }) {
                        Text(item)
                    }
                }
            }
        }

        // Show Previews
        PreviewCard(previewItems, onOffsetChanged = {
            previewCardComponent.layoutCoordinates = it
        }, modifier = Modifier.weight(2f).fillMaxHeight())

    }

    // Show dragged item
    draggedItem?.let { item ->
        DraggedItemPreview(item, draggedItemOffset)
    }
}

@Composable
private fun PreviewCard(
    previewItems: List<UIComponent>,
    onOffsetChanged: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    // Preview Section
    Card(
        modifier = modifier.padding(8.dp)
            .onGloballyPositioned { coordinates ->
                onOffsetChanged(coordinates)
            }, elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Button(onClick = {

            }) {
                Text("Show Json")
            }
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black,
                thickness = 1.dp
            )
            previewItems.forEach { component ->
                when (component) {
                    is UIComponent.SimpleComponent -> SimpleComponentPreview(component)
                    is UIComponent.LayoutComponent -> LayoutComponentPreview(component)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SimpleComponentPreview(
    component: UIComponent.SimpleComponent, offset: Offset = component.offset
) {
    val modifierWithOffset = Modifier.wrapContentSize().onGloballyPositioned { coordinates ->
        component.layoutCoordinates = coordinates
    }.clickable {
        println("Clicked on ${component.type}, offset(x: ${component.offset.x.roundToInt()}, y: ${component.offset.y.roundToInt()})")
    }
    when (component.type) {
        "Button" -> Button(
            onClick = { /* No action */ }, modifier = modifierWithOffset
        ) {
            Text(component.type)
        }

        "TextField" -> OutlinedTextField(
            value = "",
            onValueChange = { /* No action */ },
            label = { Text(component.type) },
            modifier = modifierWithOffset
        )

        "Checkbox" -> Checkbox(
            checked = false, onCheckedChange = { /* No action */ }, modifier = modifierWithOffset
        )

        "RadioButton" -> RadioButton(
            selected = false, onClick = { /* No action */ }, modifier = modifierWithOffset
        )

        "Switch" -> Switch(
            checked = false, onCheckedChange = { /* No action */ }, modifier = modifierWithOffset
        )

        else -> Text(
            text = component.type, modifier = modifierWithOffset
        )
    }
}

@Composable
private fun LayoutComponentPreview(
    component: UIComponent.LayoutComponent, offset: Offset = component.offset
) {
    val containerModifier =
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                component.layoutCoordinates = coordinates
            }.clickable {
                println("Clicked on ${component.type}, offset(x: ${component.offset.x.roundToInt()}, y: ${component.offset.y.roundToInt()})")
            }.border(
                BorderStroke(1.dp, if (component.isFocused) Color.Green else Color.Gray),
                shape = RoundedCornerShape(4.dp)
            ).padding(8.dp)

    when (component.type) {
        "Row" -> Row(
            modifier = containerModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            component.children.forEach { child ->
                when (child) {
                    is UIComponent.SimpleComponent -> SimpleComponentPreview(child, Offset.Zero)
                    is UIComponent.LayoutComponent -> LayoutComponentPreview(child, Offset.Zero)
                    else -> {}
                }
            }
        }

        "Column" -> Column(
            modifier = containerModifier, verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            component.children.forEach { child ->
                when (child) {
                    is UIComponent.SimpleComponent -> SimpleComponentPreview(child, Offset.Zero)
                    is UIComponent.LayoutComponent -> LayoutComponentPreview(child, Offset.Zero)
                    else -> {}
                }
            }
        }

        "Group" -> Box(modifier = containerModifier) {
            component.children.forEach { child ->
                when (child) {
                    is UIComponent.SimpleComponent -> SimpleComponentPreview(child, Offset.Zero)
                    is UIComponent.LayoutComponent -> LayoutComponentPreview(child, Offset.Zero)
                    else -> {}
                }
            }
        }
    }
}


@Composable
private fun DraggedItemPreview(draggedItem: UIComponent, draggedItemOffset: Offset) {
    // Dragged item preview
    Column(modifier = Modifier.offset {
        IntOffset(draggedItemOffset.x.roundToInt(), draggedItemOffset.y.roundToInt())
    }.border(1.dp, Color.Red)) {
        Text(
            text = "x:${draggedItemOffset.x.roundToInt()}, y: ${draggedItemOffset.y.roundToInt()}",
        )
        when (draggedItem) {
            is UIComponent.SimpleComponent -> {
                println("Dragged SimpleComponentPreview")
                SimpleComponentPreview(
                    draggedItem, Offset.Zero
                )
            }

            is UIComponent.LayoutComponent -> {
                println("Dragged LayoutComponentPreview")
                LayoutComponentPreview(
                    draggedItem, Offset.Zero
                )
            }

            else -> {}
        }
    }
}

private fun UIComponent.touches(draggable: Offset): Boolean {
    val draggableBounds =
        Rect(draggable, draggable + Offset(50f, 50f)) // Assuming draggable size is 50.dp
    val otherComponentBounds = Rect(offset, offset + sizeOffset)
    return !draggableBounds.intersect(otherComponentBounds).isEmpty
}

private fun getFocusedComponent(
    previewItems: List<UIComponent>,
    draggedItemOffset: Offset
): UIComponent.LayoutComponent? {
    val targetLayout = previewItems.lastOrNull {
        it is UIComponent.LayoutComponent && it.touches(
            draggedItemOffset
        )
    } as? UIComponent.LayoutComponent
    return targetLayout
}

private fun setFocusedUpdateRecursive(
    component: MutableList<UIComponent>,
    draggedItemOffset: Offset
): Boolean {
    component.forEachIndexed { index, uiComponent ->
        if (uiComponent is UIComponent.LayoutComponent) {
            if (uiComponent.children.isNotEmpty()) {
                return setFocusedUpdateRecursive(
                    uiComponent.children,
                    draggedItemOffset
                )
            } else {
                if (uiComponent.touches(draggedItemOffset)) {
                    component[index] = uiComponent.copy(isFocused = true)
                    return true
                } else {
                    // Reset the current items if not focused
                    if (uiComponent.isFocused)
                        component[index] = uiComponent.copy(isFocused = false)
                }
            }
        }
    }
    return false
}

private fun getFocusedRecursive(
    component: MutableList<UIComponent>,
    draggedItemOffset: Offset
): UIComponent? {
    component.forEachIndexed { index, uiComponent ->
        if (uiComponent is UIComponent.LayoutComponent) {
            return if (uiComponent.children.isNotEmpty()) {
                getFocusedRecursive(
                    uiComponent.children,
                    draggedItemOffset
                )
            } else {
                if (uiComponent.touches(draggedItemOffset)) {
                    uiComponent
                } else {
                    null
                }
            }
        }
    }
    return null
}

private fun removeFocusedUpdateRecursive(
    component: MutableList<UIComponent>
) {
    // Check if any children are focused
    component.forEachIndexed { index, uiComponent ->
        if (uiComponent is UIComponent.LayoutComponent) {
            if (uiComponent.children.isNotEmpty()) {
                return removeFocusedUpdateRecursive(
                    uiComponent.children
                )
            } else {
                if (uiComponent.isFocused)
                    component[index] = uiComponent.copy(isFocused = false)
            }
        }
    }
}