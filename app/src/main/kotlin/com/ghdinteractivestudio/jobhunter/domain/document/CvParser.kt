package com.ghdinteractivestudio.jobhunter.domain.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ghdinteractivestudio.jobhunter.domain.model.EducationItem
import com.ghdinteractivestudio.jobhunter.domain.model.ExperienceItem
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import java.io.InputStream
import java.util.zip.ZipInputStream

object CvParser {

    data class ExtractedCvData(
        val fileName: String,
        val format: String,
        val rawText: String,
        val deterministicProfile: UserProfile
    )

    fun parseCvFile(context: Context, uri: Uri): ExtractedCvData {
        val fileName = getFileName(context, uri)
        val format = when {
            fileName.endsWith(".docx", ignoreCase = true) -> "DOCX"
            fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
            else -> "TEXT"
        }

        val rawText = extractText(context, uri, format)
        val profile = buildProfileFromText(rawText, fileName, format)

        return ExtractedCvData(
            fileName = fileName,
            format = format,
            rawText = rawText,
            deterministicProfile = profile
        )
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "mon_cv"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun extractText(context: Context, uri: Uri, format: String): String {
        val contentResolver = context.contentResolver
        return when (format) {
            "DOCX" -> contentResolver.openInputStream(uri)?.use { extractTextFromDocx(it) } ?: ""
            "PDF" -> contentResolver.openInputStream(uri)?.use { extractTextFromPdf(it) } ?: ""
            else -> contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zis.bufferedReader(Charsets.UTF_8).readText()
                    // Extract text inside <w:t> tags and recognize paragraph breaks
                    val pRegex = Regex("<w:p[^>]*>(.*?)</w:p>")
                    val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>")

                    val paragraphs = pRegex.findAll(xml)
                    for (p in paragraphs) {
                        val pContent = p.groupValues[1]
                        val texts = tRegex.findAll(pContent)
                        val line = texts.joinToString("") { it.groupValues[1] }
                        if (line.isNotBlank()) {
                            sb.append(line.trim()).append("\n")
                        }
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        return sb.toString()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()
    }

    private fun extractTextFromPdf(inputStream: InputStream): String {
        val raw = inputStream.bufferedReader(Charsets.ISO_8859_1).readText()
        val textBlocks = StringBuilder()

        // 1. Match uncompressed PDF literal text strings: (text) Tj and [(t)10(e)10(xt)] TJ
        val tjRegex = Regex("""\((.*?)\)\s*Tj""")
        val matches = tjRegex.findAll(raw)
        for (m in matches) {
            val decoded = decodePdfString(m.groupValues[1])
            if (decoded.isNotBlank()) {
                textBlocks.append(decoded).append(" ")
            }
        }

        // Match array TJ strings
        val arrayTjRegex = Regex("""\[(.*?)\]\s*TJ""")
        val arrayMatches = arrayTjRegex.findAll(raw)
        for (m in arrayMatches) {
            val inner = m.groupValues[1]
            val partRegex = Regex("""\((.*?)\)""")
            for (p in partRegex.findAll(inner)) {
                val decoded = decodePdfString(p.groupValues[1])
                textBlocks.append(decoded)
            }
            textBlocks.append("\n")
        }

        var result = textBlocks.toString().trim()

        // Fallback: If uncompressed strings were scarce (compressed streams), extract readable word tokens
        if (result.length < 50) {
            val words = Regex("""[A-Za-z0-9À-ÿ@._+-]{2,}""").findAll(raw)
            val filtered = words.map { it.value }
                .filter { !it.startsWith("obj") && !it.startsWith("endobj") && !it.startsWith("xref") }
                .take(1500)
                .joinToString(" ")
            result = filtered
        }

        return result
    }

    private fun decodePdfString(str: String): String =
        str.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")

    /**
     * Deterministic, 100% REAL extraction from actual text lines with ZERO fabricated data.
     */
    fun buildProfileFromText(rawText: String, fileName: String, format: String): UserProfile {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extract Real Email
        val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}""")
        val foundEmail = emailRegex.find(rawText)?.value ?: ""

        // 2. Extract Real Phone Number
        val phoneRegex = Regex("""(?:(?:\+|00)\d{1,3}[\s.-]?)?(?:0|\(\d\))[\s.-]?[1-9](?:[\s.-]?\d{2}){4}""")
        val foundPhone = phoneRegex.find(rawText)?.value ?: ""

        // 3. Extract Real Full Name & Title
        // Usually the first non-empty lines that don't contain email or phone
        var detectedName = ""
        var detectedTitle = ""
        val candidateLines = lines.filter { !it.contains("@") && !it.matches(Regex(""".*\d{8,}.*""")) }

        if (candidateLines.isNotEmpty()) {
            detectedName = candidateLines[0]
        }
        if (candidateLines.size > 1 && candidateLines[1].length < 80) {
            detectedTitle = candidateLines[1]
        }

        // 4. Extract Real Skills (Keywords matching actual text)
        val extractedSkills = mutableListOf<String>()
        val knownSkillKeywords = listOf(
            "Kotlin", "Java", "Swift", "Python", "JavaScript", "TypeScript", "C++", "C#", "Rust", "Go", "PHP", "SQL",
            "Android", "Jetpack Compose", "React", "React Native", "Flutter", "Vue", "Angular", "Node.js", "Spring Boot",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Linux", "Git", "CI/CD", "REST", "GraphQL", "Room", "Firebase",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Figma", "Architecture MVVM", "Clean Architecture", "Scrum", "Agile"
        )
        for (kw in knownSkillKeywords) {
            if (rawText.contains(kw, ignoreCase = true)) {
                extractedSkills.add(kw)
            }
        }

        // 5. Extract Real Languages
        val extractedLanguages = mutableListOf<String>()
        val languageKeywords = listOf("Français", "Anglais", "Espagnol", "Arabe", "Allemand", "Italien", "Chinois", "Russe")
        for (lang in languageKeywords) {
            if (rawText.contains(lang, ignoreCase = true)) {
                extractedLanguages.add(lang)
            }
        }

        // 6. Section Parsing (Expériences, Formations)
        val experiences = mutableListOf<ExperienceItem>()
        val educations = mutableListOf<EducationItem>()

        var currentSection = ""
        var currentExpLines = mutableListOf<String>()

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("expérience") || lower.contains("experience") || lower.contains("parcours")) {
                currentSection = "EXP"
                continue
            } else if (lower.contains("formation") || lower.contains("diplôme") || lower.contains("diplome") || lower.contains("éducation") || lower.contains("education")) {
                currentSection = "EDU"
                continue
            } else if (lower.contains("compétence") || lower.contains("competence") || lower.contains("skills")) {
                currentSection = "SKILLS"
                continue
            }

            if (currentSection == "EXP" && line.length > 5) {
                // If line contains year/date or bullet, treat as experience
                if (line.matches(Regex(""".*\b(20\d\d|19\d\d)\b.*""")) || line.startsWith("-") || line.startsWith("•")) {
                    experiences.add(
                        ExperienceItem(
                            title = line.take(80),
                            company = "",
                            period = Regex("""\b(20\d\d|19\d\d)\b.*""").find(line)?.value ?: "",
                            description = line
                        )
                    )
                }
            } else if (currentSection == "EDU" && line.length > 5) {
                if (line.matches(Regex(""".*\b(20\d\d|19\d\d)\b.*""")) || lower.contains("bac") || lower.contains("licence") || lower.contains("master") || lower.contains("ingénieur")) {
                    educations.add(
                        EducationItem(
                            degree = line.take(80),
                            institution = "",
                            year = Regex("""\b(20\d\d|19\d\d)\b""").find(line)?.value ?: "",
                            description = line
                        )
                    )
                }
            }
        }

        return UserProfile(
            fullName = detectedName,
            email = foundEmail,
            phone = foundPhone,
            title = detectedTitle,
            summary = lines.take(5).joinToString(" "),
            skills = extractedSkills.distinct(),
            languages = extractedLanguages.distinct(),
            experiences = experiences.take(10),
            educations = educations.take(5),
            sourceCvFileName = fileName,
            sourceCvFormat = format,
            rawCvContent = rawText,
            parsedAt = System.currentTimeMillis()
        )
    }
}
