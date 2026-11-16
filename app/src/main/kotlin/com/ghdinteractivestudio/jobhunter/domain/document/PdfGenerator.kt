package com.ghdinteractivestudio.jobhunter.domain.document

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Native Android PDF Generator :
 * Creates clean, ATS-compliant, high-resolution vector PDF documents.
 */
object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width at 72dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height at 72dpi
    private const val MARGIN = 40f

    fun generatePdf(
        title: String,
        sections: List<Pair<String, List<String>>>,
        outputFile: File
    ) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // Emerald accent
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(36, 42, 56)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var currentY = MARGIN + 25f

        // Document Title
        canvas.drawText(title, MARGIN, currentY, titlePaint)
        currentY += 15f
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        currentY += 25f

        for ((sectionTitle, paragraphs) in sections) {
            // Check page overflow
            if (currentY > PAGE_HEIGHT - MARGIN - 60f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN + 20f
            }

            if (sectionTitle.isNotBlank()) {
                canvas.drawText(sectionTitle.uppercase(), MARGIN, currentY, sectionPaint)
                currentY += 18f
            }

            for (paragraph in paragraphs) {
                val wrappedLines = wrapText(paragraph, bodyPaint, PAGE_WIDTH - 2 * MARGIN)
                for (line in wrappedLines) {
                    if (currentY > PAGE_HEIGHT - MARGIN - 30f) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = MARGIN + 20f
                    }
                    canvas.drawText(line, MARGIN, currentY, bodyPaint)
                    currentY += 15f
                }
                currentY += 6f
            }
            currentY += 10f
        }

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
