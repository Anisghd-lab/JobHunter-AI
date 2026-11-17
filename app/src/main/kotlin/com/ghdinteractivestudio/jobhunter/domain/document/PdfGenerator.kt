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
 * Creates clean, ATS-compliant high-resolution vector PDFs and exact MLDP-formatted Cover Letters.
 */
object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width at 72dpi
    private const val PAGE_HEIGHT = 842 // A4 standard height at 72dpi
    private const val MARGIN = 44f

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
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 13f
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

        var currentY = MARGIN + 18f
        val maxContentWidth = PAGE_WIDTH - 2 * MARGIN

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN + 18f
            }
        }

        // Document Title with proper line wrapping (no horizontal overflow!)
        val wrappedTitle = wrapText(title, titlePaint, maxContentWidth)
        for (titleLine in wrappedTitle) {
            canvas.drawText(titleLine, MARGIN, currentY, titlePaint)
            currentY += 22f
        }
        canvas.drawLine(MARGIN, currentY - 6f, PAGE_WIDTH - MARGIN, currentY - 6f, linePaint)
        currentY += 16f

        for ((sectionTitle, paragraphs) in sections) {
            ensureSpace(45f)

            if (sectionTitle.isNotBlank()) {
                canvas.drawText(sectionTitle.uppercase(), MARGIN, currentY, sectionPaint)
                currentY += 18f
            }

            for (paragraph in paragraphs) {
                val wrappedLines = wrapText(paragraph, bodyPaint, maxContentWidth)
                for (line in wrappedLines) {
                    ensureSpace(16f)
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

    /**
     * Generates an A4 Cover Letter PDF following the exact 7-part spatial layout of MLDP
     * with Place & Date positioned Top-Right:
     * 1. Top-Left Sender | 2. Top-Right Place & Date | 3. Middle-Right Recipient |
     * 4. Left Bold Subject & Salutation | 5. Indented Body Paragraphs |
     * 6. Politeness Formula | 7. Right Signature.
     */
    fun generateMldpCoverLetterPdf(
        letter: MldpCoverLetterData,
        outputFile: File
    ) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val boldPaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val regularPaint = Paint().apply {
            color = Color.rgb(31, 41, 55)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val subtlePaint = Paint().apply {
            color = Color.rgb(75, 85, 99)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val maxContentWidth = PAGE_WIDTH - 2 * MARGIN
        val rightColX = 330f
        val rightColWidth = PAGE_WIDTH - MARGIN - rightColX
        val headerTopY = MARGIN + 16f

        // 1. Haut-Gauche: Vos noms et coordonnées
        var leftY = headerTopY
        canvas.drawText(letter.senderName, MARGIN, leftY, boldPaint)
        leftY += 15f
        listOfNotNull(
            letter.senderAddress.ifBlank { null },
            letter.senderPhone.ifBlank { null },
            letter.senderEmail.ifBlank { null }
        ).forEach { line ->
            wrapText(line, subtlePaint, 240f).forEach { wLine ->
                canvas.drawText(wLine, MARGIN, leftY, subtlePaint)
                leftY += 14f
            }
        }

        // 2. Haut-Droite: Lieu et Date tout en haut à droite
        var dateY = headerTopY
        wrapText(letter.placeAndDate, regularPaint, rightColWidth).forEach { wLine ->
            val wWidth = regularPaint.measureText(wLine)
            canvas.drawText(wLine, PAGE_WIDTH - MARGIN - wWidth, dateY, regularPaint)
            dateY += 15f
        }

        // 3. Milieu-Droite: Coordonnées du destinataire sous le bloc supérieur
        var rightY = maxOf(leftY, dateY + 24f) + 6f
        wrapText(letter.recipientCompany, boldPaint, rightColWidth).forEach { wLine ->
            canvas.drawText(wLine, rightColX, rightY, boldPaint)
            rightY += 15f
        }
        listOfNotNull(
            letter.recipientAttention.ifBlank { null },
            letter.recipientLocation.ifBlank { null }
        ).forEach { line ->
            wrapText(line, subtlePaint, rightColWidth).forEach { wLine ->
                canvas.drawText(wLine, rightColX, rightY, subtlePaint)
                rightY += 14f
            }
        }

        // Advance below header blocks
        var currentY = maxOf(leftY, rightY) + 26f

        fun ensureSpace(needed: Float) {
            if (currentY + needed > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN + 20f
            }
        }

        // 4. Objet & Salutation
        wrapText(letter.subject, boldPaint, maxContentWidth).forEach { line ->
            ensureSpace(16f)
            canvas.drawText(line, MARGIN, currentY, boldPaint)
            currentY += 16f
        }
        currentY += 14f
        ensureSpace(18f)
        canvas.drawText(letter.salutation, MARGIN, currentY, regularPaint)
        currentY += 22f

        // 5. Corps de texte (MLDP alinéas avec retrait de première ligne)
        val firstLineIndent = 26f
        for (para in letter.bodyParagraphs) {
            if (para.isBlank()) continue
            val lines = wrapTextWithIndent(para.trim(), regularPaint, maxContentWidth, firstLineIndent)
            lines.forEachIndexed { idx, line ->
                ensureSpace(16f)
                val x = if (idx == 0) MARGIN + firstLineIndent else MARGIN
                canvas.drawText(line, x, currentY, regularPaint)
                currentY += 15.5f
            }
            currentY += 10f
        }

        // 6. Formule de politesse
        if (letter.closingFormula.isNotBlank()) {
            val closingLines = wrapTextWithIndent(letter.closingFormula.trim(), regularPaint, maxContentWidth, firstLineIndent)
            closingLines.forEachIndexed { idx, line ->
                ensureSpace(16f)
                val x = if (idx == 0) MARGIN + firstLineIndent else MARGIN
                canvas.drawText(line, x, currentY, regularPaint)
                currentY += 15.5f
            }
            currentY += 24f
        }

        // 7. Signature (alignée à droite)
        ensureSpace(24f)
        val sigWidth = boldPaint.measureText(letter.signatureName)
        canvas.drawText(letter.signatureName, PAGE_WIDTH - MARGIN - sigWidth - 16f, currentY, boldPaint)

        document.finishPage(page)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val rawLines = text.split(Regex("\\r?\\n"))
        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }
            val words = trimmed.split(Regex("\\s+"))
            var currentLine = StringBuilder()
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }

    private fun wrapTextWithIndent(
        text: String,
        paint: Paint,
        maxWidth: Float,
        firstLineIndent: Float
    ): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        var currentLine = StringBuilder()
        var isFirst = true

        for (word in words) {
            val availWidth = if (isFirst) maxWidth - firstLineIndent else maxWidth
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= availWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    isFirst = false
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }
}
