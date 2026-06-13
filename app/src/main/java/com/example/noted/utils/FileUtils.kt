package com.example.noted.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.noted.Note
import com.example.noted.NoteElement
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "bin"
        val fileName = "imported_${System.currentTimeMillis()}.$extension"
        val file = File(context.filesDir, fileName)
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveAsPng(bitmap: Bitmap, context: Context, title: String) {
    val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (bitmap.config == Bitmap.Config.HARDWARE || bitmap.config == null)) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
    } else {
        bitmap
    }
    val filename = "${title}_${System.currentTimeMillis()}.png"
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            outputStream = FileOutputStream(image)
        }
        outputStream?.use {
            softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}

fun saveAsPdf(bitmap: Bitmap, context: Context, title: String) {
    val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (bitmap.config == Bitmap.Config.HARDWARE || bitmap.config == null)) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
    } else {
        bitmap
    }
    val filename = "${title}_${System.currentTimeMillis()}.pdf"
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(softwareBitmap.width, softwareBitmap.height, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
    pdfDocument.finishPage(page)
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pdfFile = File(downloadsDir, filename)
            outputStream = FileOutputStream(pdfFile)
        }
        outputStream?.use {
            pdfDocument.writeTo(it)
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

fun saveAsText(context: Context, title: String, content: String) {
    val filename = "${title}_${System.currentTimeMillis()}.txt"
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val textFile = File(downloadsDir, filename)
            outputStream = FileOutputStream(textFile)
        }
        outputStream?.use {
            it.write(content.toByteArray())
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save Text file", Toast.LENGTH_SHORT).show()
    }
}
fun saveAsJPEG(bitmap: Bitmap, context: Context, title: String) {
    val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (bitmap.config == Bitmap.Config.HARDWARE || bitmap.config == null)) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
    } else {
        bitmap
    }
    val filename = "${title}_${System.currentTimeMillis()}.JPEG"
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            outputStream = FileOutputStream(image)
        }
        outputStream?.use {
            softwareBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}

fun saveAsMarkdown(context: Context, title: String, content: String) {
    val filename = "${title}_${System.currentTimeMillis()}.md"
    val markdown = "# $title\n\n$content"
    saveContentToFile(context, filename, markdown, "text/markdown")
}

fun saveAsHtml(context: Context, note: Note) {
    val filename = "${note.title}_${System.currentTimeMillis()}.html"
    val html = buildString {
        append("<!DOCTYPE html><html><head><title>${note.title}</title>")
        append("<style>body{font-family:sans-serif;padding:20px;} .drawing{border:1px solid #ccc; margin:10px 0;}</style>")
        append("</head><body>")
        append("<h1>${note.title}</h1>")
        append("<p style='white-space: pre-wrap;'>${note.content}</p>")
        
        note.elements.forEach { element ->
            when (element.type) {
                "text" -> append("<div style='border:1px dashed #aaa; padding:5px; margin:5px;'>${element.text}</div>")
                "path" -> {
                    append("<div class='drawing'><svg width='500' height='300' viewBox='0 0 500 300'>")
                    append(convertToSvgPath(element))
                    append("</svg></div>")
                }
            }
        }
        append("</body></html>")
    }
    saveContentToFile(context, filename, html, "text/html")
}

fun saveAsJson(context: Context, note: Note) {
    val filename = "${note.title}_${System.currentTimeMillis()}.json"
    val json = Gson().toJson(note)
    saveContentToFile(context, filename, json, "application/json")
}

fun saveAsZip(context: Context, note: Note) {
    val filename = "${note.title}_${System.currentTimeMillis()}.zip"
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val zipFile = File(downloadsDir, filename)
            outputStream = FileOutputStream(zipFile)
        }

        outputStream?.use { os ->
            ZipOutputStream(os).use { zos ->
                zos.putNextEntry(ZipEntry("note.json"))
                zos.write(Gson().toJson(note).toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("content.txt"))
                zos.write(note.content.toByteArray())
                zos.closeEntry()

                note.elements.forEach { element ->
                    element.uri?.let { uriString ->
                        val file = File(uriString)
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry("media/${file.name}"))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            Toast.makeText(context, "Saved Zip to Downloads", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to create Zip", Toast.LENGTH_SHORT).show()
    }
}

private fun saveContentToFile(context: Context, filename: String, content: String, mimeType: String) {
    val outputStream: OutputStream?
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            outputStream = FileOutputStream(file)
        }
        outputStream?.use {
            it.write(content.toByteArray())
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save $mimeType file", Toast.LENGTH_SHORT).show()
    }
}

private fun convertToSvgPath(element: NoteElement): String {
    val points = element.pathData ?: return ""
    return buildString {
        append("<path d='")
        points.forEach { point ->
            if (point.isMoveTo) append("M ${point.x} ${point.y} ")
            else append("L ${point.x} ${point.y} ")
        }
        val color = String.format("#%06X", (0xFFFFFF and element.color))
        append("' fill='none' stroke='$color' stroke-width='2' />")
    }
}
