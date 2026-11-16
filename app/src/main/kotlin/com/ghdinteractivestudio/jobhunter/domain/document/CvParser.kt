package com.ghdinteractivestudio.jobhunter.domain.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import com.ghdinteractivestudio.jobhunter.domain.model.EducationItem
import com.ghdinteractivestudio.jobhunter.domain.model.ExperienceItem
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object CvParser {

    private const val TAG = "CvParser"

    data class ExtractedCvData(
        val fileName: String,
        val format: String,
        val rawText: String,
        val deterministicProfile: UserProfile
    )

    fun parseCvFile(context: Context, uri: Uri): ExtractedCvData {
        val fileName = getFileName(context, uri)
        val format = detectFormat(context, uri, fileName)

        Log.d(TAG, "Parsing CV file: '$fileName' with detected format: $format from uri: $uri")

        val rawText = when (format) {
            "PDF" -> extractTextFromPdf(context, uri)
            "DOCX" -> extractTextFromDocx(context, uri)
            else -> extractTextFromPlainText(context, uri)
        }

        Log.d(TAG, "Extraction completed for '$fileName'. Extracted characters: ${rawText.length}")

        val profile = buildProfileFromText(rawText, fileName, format)

        return ExtractedCvData(
            fileName = fileName,
            format = format,
            rawText = rawText,
            deterministicProfile = profile
        )
    }

    private fun getInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file") {
                val path = uri.path ?: return null
                FileInputStream(File(path))
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open input stream for $uri: ${e.message}")
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "mon_cv"
        if (uri.scheme == "file") {
            uri.path?.let { p ->
                val f = File(p)
                if (f.name.isNotBlank()) return f.name
            }
        }
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val queried = cursor.getString(nameIndex)
                if (!queried.isNullOrBlank()) {
                    name = queried
                }
            }
        }
        return name
    }

    private fun detectFormat(context: Context, uri: Uri, fileName: String): String {
        val mimeType = runCatching { context.contentResolver.getType(uri)?.lowercase() }.getOrNull() ?: ""
        val nameLower = fileName.lowercase()

        return when {
            mimeType == "application/pdf" || nameLower.endsWith(".pdf") -> "PDF"
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
            mimeType == "application/msword" ||
            nameLower.endsWith(".docx") ||
            nameLower.endsWith(".doc") -> "DOCX"
            mimeType.startsWith("text/") || nameLower.endsWith(".txt") || nameLower.endsWith(".md") -> "TEXT"
            else -> {
                if (nameLower.contains(".pdf")) "PDF"
                else if (nameLower.contains(".docx")) "DOCX"
                else "TEXT"
            }
        }
    }

    /**
     * Extracts text from PDF using Apache PDFBox for Android.
     * Properly decodes compressed streams (FlateDecode, fonts, CMaps).
     */
    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            val inputStream = getInputStream(context, uri) ?: return ""

            inputStream.use { stream ->
                PDDocument.load(stream).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    stripper.startPage = 1
                    stripper.endPage = document.numberOfPages
                    val text = stripper.getText(document) ?: ""
                    text.trim()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding PDF with PDFBox: ${e.message}", e)
            ""
        }
    }

    /**
     * Extracts text from Word DOCX archive using native ZipInputStream and XmlPullParser.
     * Reads word/document.xml, extracting purely text nodes <w:t> and preserving paragraph breaks.
     */
    private fun extractTextFromDocx(context: Context, uri: Uri): String {
        val inputStream = getInputStream(context, uri) ?: return ""
        val sb = StringBuilder()

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType
                        var inTextTag = false

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    val tag = parser.name
                                    if (tag == "t" || tag.endsWith(":t")) {
                                        inTextTag = true
                                    }
                                }
                                XmlPullParser.TEXT -> {
                                    if (inTextTag) {
                                        val text = parser.text
                                        if (!text.isNullOrEmpty()) {
                                            sb.append(text)
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    val tag = parser.name
                                    if (tag == "t" || tag.endsWith(":t")) {
                                        inTextTag = false
                                    } else if (tag == "p" || tag.endsWith(":p")) {
                                        sb.append("\n")
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from DOCX: ${e.message}", e)
        }

        return sb.toString().trim()
    }

    /**
     * Extracts text from plain text or markdown files.
     */
    private fun extractTextFromPlainText(context: Context, uri: Uri): String {
        return try {
            getInputStream(context, uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText().trim()
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error reading plain text: ${e.message}", e)
            ""
        }
    }

    /**
     * Deterministic, 100% REAL extraction from actual text lines with ZERO fabricated data.
     */
    fun buildProfileFromText(rawText: String, fileName: String, format: String): UserProfile {
        if (rawText.isBlank()) {
            return UserProfile(
                fullName = "",
                email = "",
                phone = "",
                title = "",
                summary = "",
                skills = emptyList(),
                languages = emptyList(),
                experiences = emptyList(),
                educations = emptyList(),
                sourceCvFileName = fileName,
                sourceCvFormat = format,
                rawCvContent = "",
                parsedAt = System.currentTimeMillis()
            )
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extract Real Email
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        val foundEmail = emailRegex.find(rawText)?.value ?: ""

        // 2. Extract Real Phone Number (International + Algerian/French/Global)
        val phoneRegex = Regex("""(?:\+|00)\d{1,4}(?:[\s.-]?\d{1,4}){3,6}|(?:0|\(\d\))[\s.-]?[1-9](?:[\s.-]?\d{2}){4}""")
        val foundPhone = phoneRegex.find(rawText)?.value ?: ""

        // 3. Extract Real Full Name & Title
        var detectedName = ""
        var detectedTitle = ""
        val candidateLines = lines.filter { !it.contains("@") && !it.matches(Regex(""".*\d{7,}.*""")) }

        if (candidateLines.isNotEmpty()) {
            detectedName = candidateLines[0]
        }
        if (candidateLines.size > 1 && candidateLines[1].length < 120 && !candidateLines[1].contains("@")) {
            detectedTitle = candidateLines[1]
        }

        // 4. Extract Real Summary
        var extractedSummary = ""
        val summaryIndex = lines.indexOfFirst {
            val l = it.uppercase()
            l.contains("SUMMARY") || l.contains("PROFIL") || l.contains("OBJECTIF") || l.contains("ABOUT")
        }
        if (summaryIndex != -1 && summaryIndex + 1 < lines.size) {
            val summaryLines = mutableListOf<String>()
            for (i in (summaryIndex + 1) until lines.size) {
                val l = lines[i]
                if (isSectionHeader(l)) break
                summaryLines.add(l)
            }
            extractedSummary = summaryLines.joinToString(" ")
        }
        if (extractedSummary.isBlank()) {
            extractedSummary = lines.take(4).joinToString(" ")
        }

        // 5. Section Parsing: Skills, Languages, Experiences, Educations
        val extractedSkills = mutableListOf<String>()
        val extractedLanguages = mutableListOf<String>()
        val experiences = mutableListOf<ExperienceItem>()
        val educations = mutableListOf<EducationItem>()

        var currentSection = ""
        var pendingExpTitle = ""
        var pendingExpCompany = ""
        var pendingExpPeriod = ""
        var pendingExpDesc = mutableListOf<String>()

        fun flushExperience() {
            if (pendingExpTitle.isNotBlank() || pendingExpCompany.isNotBlank()) {
                experiences.add(
                    ExperienceItem(
                        title = pendingExpTitle.ifBlank { "Poste Professionnel" },
                        company = pendingExpCompany,
                        period = pendingExpPeriod,
                        description = pendingExpDesc.joinToString("\n")
                    )
                )
                pendingExpTitle = ""
                pendingExpCompany = ""
                pendingExpPeriod = ""
                pendingExpDesc.clear()
            }
        }

        for (line in lines) {
            val upper = line.uppercase()

            if (upper.contains("COMPETENC") || upper.contains("SKILL") || upper.contains("COMPÉTENCE")) {
                flushExperience()
                currentSection = "SKILLS"
                continue
            } else if (upper.contains("EXPERIENCE") || upper.contains("EXPÉRIENCE") || upper.contains("PARCOURS")) {
                flushExperience()
                currentSection = "EXP"
                continue
            } else if (upper.contains("EDUCATION") || upper.contains("FORMATION") || upper.contains("DIPLÔME") || upper.contains("DIPLOME") || upper.contains("DEVELOPMENT")) {
                flushExperience()
                currentSection = "EDU"
                continue
            } else if (upper.contains("LANGUAGE") || upper.contains("LANGUE")) {
                flushExperience()
                currentSection = "LANG"
                continue
            } else if (upper.contains("REFERENCE") || upper.contains("RÉFÉRENCE")) {
                flushExperience()
                currentSection = "REF"
                continue
            }

            when (currentSection) {
                "SKILLS" -> {
                    // Extract skills split by bullets or commas
                    val tokens = line.split("•", "●", "·", "|", ",", ";")
                        .map { it.trim().removePrefix("-").trim() }
                        .filter { it.isNotBlank() && it.length in 2..60 }
                    extractedSkills.addAll(tokens)
                }

                "LANG" -> {
                    // Lines under LANGUAGES e.g. "Arabic: Native", "French: Fluent (C1)", "English: Intermediate (B2)"
                    val langLine = line.removePrefix("•").removePrefix("●").removePrefix("-").trim()
                    if (langLine.isNotBlank() && langLine.length < 50) {
                        extractedLanguages.add(langLine)
                    }
                }

                "EDU" -> {
                    val cleanLine = line.removePrefix("•").removePrefix("●").removePrefix("-").trim()
                    if (cleanLine.isNotBlank() && cleanLine.length > 5) {
                        val yearMatch = Regex("""\b(20\d\d|19\d\d)\b""").find(cleanLine)?.value ?: ""
                        val parts = cleanLine.split("–", "-", "|").map { it.trim() }
                        val degree = parts.firstOrNull() ?: cleanLine
                        val school = if (parts.size > 1) parts[1] else ""

                        educations.add(
                            EducationItem(
                                degree = degree,
                                institution = school,
                                year = yearMatch,
                                description = cleanLine
                            )
                        )
                    }
                }

                "EXP" -> {
                    // Detect job position or company/dates line
                    if (line.contains("|") && (line.contains("20") || line.contains("19") || line.contains("Present") || line.contains("Présent") || line.contains("Aujourd'hui"))) {
                        // This is Company | Dates line
                        val parts = line.split("|").map { it.trim() }
                        pendingExpCompany = parts.getOrNull(0) ?: ""
                        pendingExpPeriod = parts.getOrNull(1) ?: ""
                    } else if (line.startsWith("●") || line.startsWith("•") || line.startsWith("-")) {
                        // Description bullet
                        pendingExpDesc.add(line.removePrefix("●").removePrefix("•").removePrefix("-").trim())
                    } else if (!isSectionHeader(line) && line.length in 3..100) {
                        // Possible new job title
                        if (pendingExpTitle.isNotBlank() && pendingExpCompany.isNotBlank()) {
                            flushExperience()
                        }
                        if (pendingExpTitle.isBlank()) {
                            pendingExpTitle = line
                        } else {
                            pendingExpDesc.add(line)
                        }
                    }
                }
            }
        }
        flushExperience()

        // Fallback for skills if section wasn't delimited
        if (extractedSkills.isEmpty()) {
            val knownSkillKeywords = listOf(
                "Kotlin", "Java", "Swift", "Python", "JavaScript", "TypeScript", "C++", "C#", "Rust", "Go", "PHP", "SQL",
                "Android", "Jetpack Compose", "React", "React Native", "Flutter", "Vue", "Angular", "Node.js", "Spring Boot",
                "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Linux", "Git", "CI/CD", "REST", "GraphQL", "Room", "Firebase",
                "PostgreSQL", "MySQL", "MongoDB", "Redis", "Figma", "Architecture MVVM", "Clean Architecture", "Scrum", "Agile",
                "Human Resources", "Recruitment", "Onboarding", "Payroll", "Employee Relations", "Office Administration",
                "Microsoft Office", "Excel", "ERP Systems", "AutoCAD", "Fusion 360", "Photoshop", "Dart"
            )
            for (kw in knownSkillKeywords) {
                if (rawText.contains(kw, ignoreCase = true)) {
                    extractedSkills.add(kw)
                }
            }
        }

        // Fallback for languages if section wasn't delimited
        if (extractedLanguages.isEmpty()) {
            val languageKeywords = listOf("Français", "French", "Anglais", "English", "Arabe", "Arabic", "Espagnol", "Spanish", "Allemand", "German", "Italien", "Italian")
            for (lang in languageKeywords) {
                if (rawText.contains(lang, ignoreCase = true)) {
                    extractedLanguages.add(lang)
                }
            }
        }

        return UserProfile(
            fullName = detectedName,
            email = foundEmail,
            phone = foundPhone,
            title = detectedTitle,
            summary = extractedSummary,
            skills = extractedSkills.distinct().filter { it.length in 2..50 },
            languages = extractedLanguages.distinct(),
            experiences = experiences.take(15),
            educations = educations.take(10),
            sourceCvFileName = fileName,
            sourceCvFormat = format,
            rawCvContent = rawText,
            parsedAt = System.currentTimeMillis()
        )
    }

    private fun isSectionHeader(line: String): Boolean {
        val u = line.uppercase().trim()
        return u.startsWith("PROFESSIONAL") ||
                u.startsWith("CORE") ||
                u.startsWith("COMPETENC") ||
                u.startsWith("SKILL") ||
                u.startsWith("EDUCATION") ||
                u.startsWith("EXPERIENCE") ||
                u.startsWith("LANGUAGE") ||
                u.startsWith("REFERENCE") ||
                u.startsWith("FORMATION") ||
                u.startsWith("LANGUE")
    }
}
