package com.example.noted.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.noted.NoteElement
import com.example.noted.SerializablePathPoint
import dev.shreyaspatil.capturable.capturable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun NoteContent(
    noteTitle: String,
    onNoteTitleChange: (String) -> Unit,
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    importedElements: List<NoteElement>,
    onImportedElementsChange: (List<NoteElement>) -> Unit,
    panOffset: Offset,
    onPanOffsetChange: (Offset) -> Unit = {},
    zoomScale: Float = 1f,
    onZoomScaleChange: (Float) -> Unit = {},
    isDrawingMode: Boolean,
    currentTool: String = "Pen",
    currentPathPoints: List<SerializablePathPoint>,
    onCurrentPathPointsChange: (List<SerializablePathPoint>) -> Unit = {},
    strokeColor: Color,
    isExporting: Boolean = false,
    showWordCount: Boolean = false,
    showCharCount: Boolean = false
) {
    val wordCount by remember(noteContent) {
        androidx.compose.runtime.derivedStateOf {
            if (noteContent.isBlank()) 0
            else noteContent.trim().split("\\s+".toRegex()).size
        }
    }
    val charCount by remember(noteContent) {
        androidx.compose.runtime.derivedStateOf {
            noteContent.length
        }
    }

    val metrics by remember(importedElements, isExporting, noteTitle) {
        androidx.compose.runtime.derivedStateOf {
            var minX = 0f
            var minY = 0f
            importedElements.forEach { e ->
                if (e.type == "path") e.pathData?.forEach { p -> minX = min(minX, p.x); minY = min(minY, p.y) }
                else { minX = min(minX, e.x); minY = min(minY, e.y) }
            }
            if (isExporting) minY = min(minY, -120f)
            val buffer = if (isExporting) 40f else 0f
            Offset(minX - buffer, minY - buffer)
        }
    }
    val minX = metrics.x
    val minY = metrics.y

    val contentModifier = if (isExporting) {
        Modifier
            .wrapContentSize(unbounded = true)
            .padding(16.dp)
    } else {
        Modifier.fillMaxSize()
    }

    val updatedPanOffset by rememberUpdatedState(panOffset)
    val updatedZoomScale by rememberUpdatedState(zoomScale)
    val updatedCurrentPathPoints by rememberUpdatedState(currentPathPoints)
    val updatedIsDrawingMode by rememberUpdatedState(isDrawingMode)
    val updatedCurrentTool by rememberUpdatedState(currentTool)
    val updatedStrokeColor by rememberUpdatedState(strokeColor)
    val updatedImportedElements by rememberUpdatedState(importedElements)

    Column(modifier = contentModifier) {
        if (!isExporting) {
            TextField(
                value = noteTitle,
                onValueChange = onNoteTitleChange,
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineMedium
            )
        }

        Box(
            modifier = Modifier
                .then(if (isExporting) Modifier.wrapContentSize(unbounded = true) else Modifier.weight(1f))
                .then(if (!isExporting) Modifier.clipToBounds() else Modifier)
                .pointerInput(isExporting) {
                    if (!isExporting) {
                        awaitPointerEventScope {
                            while (true) { awaitPointerEvent() }
                        }
                    }
                }
                .pointerInput(isExporting) {
                    if (!isExporting) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (!updatedIsDrawingMode) {
                                onZoomScaleChange((updatedZoomScale * zoom).coerceIn(0.5f, 5f))
                                val newX = updatedPanOffset.x + pan.x
                                val newY = updatedPanOffset.y + pan.y
                                onPanOffsetChange(Offset(x = newX, y = newY.coerceAtMost(0f)))
                            }
                        }
                    }
                }
                .pointerInput(isExporting) {
                    if (!isExporting) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (updatedIsDrawingMode) {
                                    val canvasX = offset.x / updatedZoomScale
                                    val canvasY = offset.y / updatedZoomScale
                                    onCurrentPathPointsChange(
                                        listOf(SerializablePathPoint(canvasX, canvasY, isMoveTo = true))
                                    )
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (updatedIsDrawingMode) {
                                    val last = updatedCurrentPathPoints.lastOrNull() ?: return@detectDragGestures
                                    val newX = last.x + dragAmount.x / updatedZoomScale
                                    val newY = last.y + dragAmount.y / updatedZoomScale
                                    val dx = newX - last.x
                                    val dy = newY - last.y
                                    if (dx * dx + dy * dy > 4f) {
                                        onCurrentPathPointsChange(updatedCurrentPathPoints + SerializablePathPoint(newX, newY))
                                    }
                                }
                            },
                            onDragEnd = {
                                if (updatedIsDrawingMode && updatedCurrentPathPoints.isNotEmpty()) {
                                    val newElement = NoteElement(
                                        type = if (updatedCurrentTool == "Eraser") "eraser" else "path",
                                        x = 0f,
                                        y = 0f,
                                        pathData = updatedCurrentPathPoints,
                                        color = if (updatedCurrentTool == "Eraser") Color.Transparent.toArgb() else updatedStrokeColor.toArgb()
                                    )
                                    onImportedElementsChange(updatedImportedElements + newElement)
                                    onCurrentPathPointsChange(emptyList())
                                }
                            }
                        )
                    }
                }
                .pointerInput(isExporting) {
                    if (!isExporting && !updatedIsDrawingMode) {
                        detectTapGestures { offset ->
                            val canvasX = offset.x / updatedZoomScale
                            val canvasY = offset.y / updatedZoomScale
                            val newElement = NoteElement(text = "", x = canvasX, y = canvasY, type = "text")
                            onImportedElementsChange(updatedImportedElements + newElement)
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (isExporting) 1f else zoomScale,
                        scaleY = if (isExporting) 1f else zoomScale,
                        translationX = if (isExporting) 0f else panOffset.x,
                        translationY = if (isExporting) 0f else panOffset.y,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
                        compositingStrategy = CompositingStrategy.Offscreen
                    )
            ) {
                Layout(
                    content = {
                        if (isExporting) {
                            Text(
                                text = noteTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        importedElements.forEachIndexed { index, element ->
                            if (element.type != "text" && element.type != "path" && element.type != "eraser") {
                                ElementBox(
                                    index = index,
                                    element = element,
                                    isDrawingMode = isDrawingMode,
                                    onAddText = { x, y ->
                                        onImportedElementsChange(importedElements + NoteElement(text = "", x = x, y = y, type = "text"))
                                    },
                                    onUpdate = { updatedElement ->
                                        onImportedElementsChange(importedElements.toMutableList().apply { this[index] = updatedElement })
                                    }
                                )
                            }
                        }

                        BasicTextField(
                            value = noteContent,
                            onValueChange = onNoteContentChange,
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                                .width(IntrinsicSize.Max),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            readOnly = isExporting,
                            decorationBox = { innerTextField ->
                                if (noteContent.isEmpty() && !isExporting) {
                                    Text(
                                        "Note content",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        importedElements.forEachIndexed { index, element ->
                            if (element.type == "text") {
                                ElementBox(
                                    index = index,
                                    element = element,
                                    isDrawingMode = isDrawingMode,
                                    onAddText = { _, _ -> },
                                    onUpdate = { updatedElement ->
                                        onImportedElementsChange(importedElements.toMutableList().apply { this[index] = updatedElement })
                                    }
                                )
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        ) {
                            importedElements.forEach { element ->
                                if ((element.type == "path" || element.type == "eraser") && element.pathData != null) {
                                    val path = Path().apply {
                                        element.pathData.forEach { point ->
                                            if (point.isMoveTo) moveTo(point.x - minX, point.y - minY)
                                            else lineTo(point.x - minX, point.y - minY)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (element.type == "eraser") Color.Transparent else Color(element.color),
                                        style = Stroke(
                                            width = if (element.type == "eraser") 30f else 5f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        ),
                                        blendMode = if (element.type == "eraser") BlendMode.Clear else BlendMode.SrcOver
                                    )
                                }
                            }

                            if (isDrawingMode && currentPathPoints.isNotEmpty()) {
                                val path = Path().apply {
                                    currentPathPoints.forEachIndexed { i, point ->
                                        if (i == 0 || point.isMoveTo) moveTo(point.x - minX, point.y - minY)
                                        else lineTo(point.x - minX, point.y - minY)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = if (currentTool == "Eraser") Color.Transparent else strokeColor,
                                    style = Stroke(
                                        width = if (currentTool == "Eraser") 30f else 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = if (currentTool == "Eraser") BlendMode.Clear else BlendMode.SrcOver
                                )
                            }
                        }
                    }
                ) { measurables, constraints ->
                    var mIdx = 0
                    val titlePlaceable = if (isExporting) measurables[mIdx++].measure(Constraints()) else null

                    var mediaCount = 0
                    importedElements.forEach { if (it.type != "text" && it.type != "path" && it.type != "eraser") mediaCount++ }
                    val mediaMeasurables = measurables.subList(mIdx, mIdx + mediaCount)
                    mIdx += mediaCount

                    val mainTextMeasurable = measurables[mIdx++]

                    var stickyCount = 0
                    importedElements.forEach { if (it.type == "text") stickyCount++ }
                    val stickyMeasurables = measurables.subList(mIdx, mIdx + stickyCount)
                    mIdx += stickyCount

                    val drawingMeasurable = measurables[mIdx]

                    val mediaPlaceables = mediaMeasurables.map { it.measure(Constraints()) }
                    val mainTextPlaceable = mainTextMeasurable.measure(Constraints())
                    val stickyPlaceables = stickyMeasurables.map { it.measure(Constraints()) }

                    var maxX = mainTextPlaceable.width.toFloat()
                    var maxY = mainTextPlaceable.height.toFloat()

                    if (isExporting && titlePlaceable != null) {
                        maxX = max(maxX, titlePlaceable.width.toFloat())
                    }

                    var mPlaceIdx = 0
                    importedElements.forEach { e ->
                        if (e.type != "text" && e.type != "path" && e.type != "eraser") {
                            val p = mediaPlaceables[mPlaceIdx++]
                            maxX = max(maxX, e.x + p.width)
                            maxY = max(maxY, e.y + p.height)
                        }
                    }
                    var sPlaceIdx = 0
                    importedElements.forEach { e ->
                        if (e.type == "text") {
                            val p = stickyPlaceables[sPlaceIdx++]
                            maxX = max(maxX, e.x + p.width)
                            maxY = max(maxY, e.y + p.height)
                        }
                    }
                    importedElements.forEach { e ->
                        if (e.type == "path") e.pathData?.forEach { p ->
                            maxX = max(maxX, p.x); maxY = max(maxY, p.y)
                        }
                    }

                    val buffer = if (isExporting) 80f else 0f
                    val finalWidth = (maxX - minX + buffer).toInt().coerceAtLeast(100)
                    val finalHeight = (maxY - minY + buffer).toInt().coerceAtLeast(100)

                    val drawingPlaceable = drawingMeasurable.measure(Constraints.fixed(finalWidth, finalHeight))

                    layout(finalWidth, finalHeight) {
                        var mSearchIdx = 0
                        var sSearchIdx = 0

                        if (isExporting && titlePlaceable != null) {
                            titlePlaceable.placeRelative((-minX).toInt(), (-minY - titlePlaceable.height - 20).toInt())
                        }

                        importedElements.forEach { e ->
                            if (e.type != "text" && e.type != "path" && e.type != "eraser") {
                                mediaPlaceables[mSearchIdx++].placeRelative((e.x - minX).toInt(), (e.y - minY).toInt())
                            }
                        }

                        mainTextPlaceable.placeRelative((-minX).toInt(), (-minY).toInt())

                        importedElements.forEach { e ->
                            if (e.type == "text") {
                                stickyPlaceables[sSearchIdx++].placeRelative((e.x - minX).toInt(), (e.y - minY).toInt())
                            }
                        }

                        drawingPlaceable.placeRelative(0, 0)
                    }
                }
            }
        }

        if (!isExporting && (showWordCount || showCharCount)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (showWordCount) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showWordCount && showCharCount) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (showCharCount) {
                    Text(
                        text = "$charCount characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ElementBox(
    index: Int,
    element: NoteElement,
    isDrawingMode: Boolean,
    onAddText: (Float, Float) -> Unit,
    onUpdate: (NoteElement) -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(element.x.roundToInt(), element.y.roundToInt()) }
            .onGloballyPositioned { layoutCoordinates ->
                val height = layoutCoordinates.size.height.toFloat()
                if (kotlin.math.abs(element.height - height) > 1f) {
                    onUpdate(element.copy(height = height))
                }
            }
            .pointerInput(isDrawingMode) {
                if (!isDrawingMode) {
                    detectTapGestures { offset ->
                        if (element.type != "text") {
                            val canvasX = element.x + offset.x
                            val canvasY = element.y + offset.y
                            onAddText(canvasX, canvasY)
                        }
                    }
                }
            }
            .pointerInput(isDrawingMode) {
                if (!isDrawingMode) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onUpdate(element.copy(x = element.x + dragAmount.x, y = element.y + dragAmount.y))
                    }
                }
            }
            .border(
                if (element.type == "text") 0.dp else 1.dp,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.small
            )
            .shadow(if (element.type == "text") 0.dp else 2.dp)
            .background(if (element.type == "text") Color.Transparent else MaterialTheme.colorScheme.surface)
    ) {
        when (element.type) {
            "pdf" -> {
                PdfView(uri = Uri.parse(element.uri ?: ""), modifier = Modifier.width(300.dp))
            }
            "image" -> {
                AsyncImage(
                    model = element.uri,
                    contentDescription = null,
                    modifier = Modifier.width(300.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
            "text" -> {
                BasicTextField(
                    value = element.text ?: "",
                    onValueChange = { newText -> onUpdate(element.copy(text = newText)) },
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .width(IntrinsicSize.Max),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun PdfView(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            parcelFileDescriptor?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val width = 1000
                    val height = (page.height.toFloat() / page.width.toFloat() * width).toInt()
                    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap = b
                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "PDF Page",
            modifier = modifier,
            contentScale = ContentScale.FillWidth
        )
    } ?: Box(modifier = modifier.height(100.dp), contentAlignment = Alignment.Center) {
        Text("Loading PDF...")
    }
}
