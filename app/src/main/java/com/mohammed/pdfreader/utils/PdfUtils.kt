package com.mohammed.pdfreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ===== Compress PDF =====
    suspend fun compressPdf(
        inputUri: Uri,
        outputFile: File,
        quality: Int = 80 // 0-100
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val doc = PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = quality / 100f
                val width = (page.width * scale).toInt().coerceAtLeast(100)
                val height = (page.height * scale).toInt().coerceAtLeast(100)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(docPage)
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ===== Convert PDF pages to images =====
    suspend fun pdfToImages(
        inputUri: Uri,
        outputDir: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90,
        dpi: Int = 150
    ): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return@withContext files
            val renderer = PdfRenderer(pfd)
            outputDir.mkdirs()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = dpi / 72f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
                val outFile = File(outputDir, "page_${i + 1}.$ext")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(format, quality, out)
                }
                bitmap.recycle()
                files.add(outFile)
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        files
    }

    // ===== Convert images to PDF =====
    suspend fun imagesToPdf(
        imageUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            imageUris.forEachIndexed { i, uri ->
                val stream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                stream.close()

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(page)
                bitmap.recycle()
            }
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ===== Extract text from PDF page (basic) =====
    suspend fun extractTextFromPage(uri: Uri, pageIndex: Int): String =
        withContext(Dispatchers.IO) {
            // Full impl uses ML Kit OCR or iText
            // Basic: render page and run OCR
            ""
        }

    // ===== Copy URI to local file =====
    suspend fun copyUriToFile(uri: Uri, destFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val input: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val output: OutputStream = FileOutputStream(destFile)
            input.copyTo(output)
            input.close()
            output.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ===== Split PDF =====
    suspend fun splitPdf(
        inputUri: Uri,
        outputDir: File,
        ranges: List<IntRange>
    ): List<File> = withContext(Dispatchers.IO) {
        val result = mutableListOf<File>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return@withContext result
            outputDir.mkdirs()

            ranges.forEachIndexed { idx, range ->
                val renderer = PdfRenderer(pfd)
                val doc = PdfDocument()
                var localPage = 1

                for (pageIdx in range) {
                    if (pageIdx >= renderer.pageCount) break
                    val page = renderer.openPage(pageIdx)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, localPage++).create()
                    val docPage = doc.startPage(pageInfo)
                    docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    doc.finishPage(docPage)
                    bitmap.recycle()
                }

                val outFile = File(outputDir, "split_${idx + 1}.pdf")
                FileOutputStream(outFile).use { doc.writeTo(it) }
                doc.close()
                renderer.close()
                result.add(outFile)
            }
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    // ===== Merge PDFs =====
    suspend fun mergePdfs(
        inputUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = PdfDocument()
            var globalPage = 1

            inputUris.forEach { uri ->
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@forEach
                val renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, globalPage++).create()
                    val docPage = doc.startPage(pageInfo)
                    docPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    doc.finishPage(docPage)
                    bitmap.recycle()
                }

                renderer.close()
                pfd.close()
            }

            FileOutputStream(outputFile).use { doc.writeTo(it) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ===== Rotate pages =====
    suspend fun rotatePages(
        inputUri: Uri,
        outputFile: File,
        degrees: Float,
        pageIndices: List<Int>? = null // null = all pages
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return@withContext false
            val renderer = PdfRenderer(pfd)
            val doc = PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val shouldRotate = pageIndices == null || i in pageIndices
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val finalBitmap = if (shouldRotate) rotateBitmap(bitmap, degrees) else bitmap

                val pageInfo = PdfDocument.PageInfo.Builder(finalBitmap.width, finalBitmap.height, i + 1).create()
                val docPage = doc.startPage(pageInfo)
                docPage.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                doc.finishPage(docPage)

                if (shouldRotate) finalBitmap.recycle()
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    // ===== Get output dir =====
    fun getOutputDir(): File {
        val dir = File(context.getExternalFilesDir(null), "PDFReader/Output")
        dir.mkdirs()
        return dir
    }

    fun getThumbnailDir(): File {
        val dir = File(context.cacheDir, "thumbnails")
        dir.mkdirs()
        return dir
    }
}
