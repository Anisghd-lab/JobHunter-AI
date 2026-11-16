package com.ghdinteractivestudio.jobhunter.domain.document

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream

object CvParser {

    /**
     * Extracts plain text from either Word (.docx), PDF or raw text stream.
     */
    fun extractText(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val fileName = uri.lastPathSegment ?: ""

        return when {
            mimeType.contains("wordprocessingml") || fileName.endsWith(".docx", ignoreCase = true) -> {
                contentResolver.openInputStream(uri)?.use { extractTextFromDocx(it) } ?: ""
            }
            mimeType.contains("pdf") || fileName.endsWith(".pdf", ignoreCase = true) -> {
                // Read raw text streams or fallback string
                contentResolver.openInputStream(uri)?.use { extractTextFromPdf(it) } ?: ""
            }
            else -> {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zis.bufferedReader(Charsets.UTF_8).readText()
                    // Extract text inside <w:t> tags
                    val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                    val matches = regex.findAll(xml)
                    for (match in matches) {
                        sb.append(match.groupValues[1]).append(" ")
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        return sb.toString().replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    }

    private fun extractTextFromPdf(inputStream: InputStream): String {
        val raw = inputStream.bufferedReader(Charsets.ISO_8859_1).readText()
        val textBlocks = StringBuilder()
        // Basic PDF text stream extractor for common uncompressed Tj / TJ operators
        val tjRegex = Regex("""\((.*?)\)\s*Tj""")
        val matches = tjRegex.findAll(raw)
        for (m in matches) {
            textBlocks.append(m.groupValues[1]).append(" ")
        }
        if (textBlocks.length < 50) {
            // Fallback: extract printable ASCII and Latin-1 words
            val words = Regex("""[A-Za-z0-9À-ÿ]{3,}""").findAll(raw)
            for (w in words.take(500)) {
                textBlocks.append(w.value).append(" ")
            }
        }
        return textBlocks.toString()
    }
}
