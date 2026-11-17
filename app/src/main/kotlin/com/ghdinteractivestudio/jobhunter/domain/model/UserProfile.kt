package com.ghdinteractivestudio.jobhunter.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExperienceItem(
    val title: String,
    val company: String,
    val period: String,
    val location: String = "",
    val description: String,
    val keySkills: List<String> = emptyList()
)

@Serializable
data class EducationItem(
    val degree: String,
    val institution: String,
    val year: String,
    val description: String = ""
)

@Serializable
data class CertificationItem(
    val name: String,
    val issuer: String = "",
    val year: String = ""
)

@Serializable
enum class CvLanguage(val code: String, val label: String) {
    EN("EN", "Anglais"),
    FR("FR", "Français")
}

@Serializable
data class CvDocumentSlot(
    val fileName: String = "",
    val format: String = "",
    val filePath: String = "",
    val rawText: String = "",
    val isModified: Boolean = false,
    val parsedAt: Long = 0L
)

@Serializable
data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val title: String = "",
    val summary: String = "",
    val skills: List<String> = emptyList(),
    val softSkills: List<String> = emptyList(),
    val experiences: List<ExperienceItem> = emptyList(),
    val educations: List<EducationItem> = emptyList(),
    val certifications: List<CertificationItem> = emptyList(),
    val languages: List<String> = emptyList(),
    val sourceCvFileName: String = "",
    val sourceCvFormat: String = "",
    val sourceCvPath: String = "",
    val rawCvContent: String = "",
    val isModifiedCv: Boolean = false,
    val parsedAt: Long = 0L,
    val englishCv: CvDocumentSlot? = null,
    val frenchCv: CvDocumentSlot? = null
) {
    fun slotFor(lang: CvLanguage): CvDocumentSlot? = when (lang) {
        CvLanguage.EN -> englishCv
        CvLanguage.FR -> frenchCv
    }

    fun withSlot(lang: CvLanguage, slot: CvDocumentSlot?): UserProfile = when (lang) {
        CvLanguage.EN -> copy(englishCv = slot)
        CvLanguage.FR -> copy(frenchCv = slot)
    }

    fun combinedSearchCorpus(): String = buildString {
        append(title).append(' ').append(summary).append(' ')
        append((skills + softSkills).joinToString(" ")).append(' ')
        append(rawCvContent).append(' ')
        englishCv?.rawText?.let { append(it).append(' ') }
        frenchCv?.rawText?.let { append(it) }
    }

    /**
     * Ensures single-CV profiles imported prior to dual EN/FR slots automatically map
     * cleanly into englishCv or frenchCv without data loss.
     */
    fun migrateLegacySlotIfNeeded(): UserProfile {
        if (englishCv != null || frenchCv != null || rawCvContent.isBlank()) return this
        val sample = "${sourceCvFileName} ${rawCvContent.take(600)}".lowercase()
        val frHints = listOf("expérience", "compétences", "formation", "langues", "français", "francais", "algérie", "poste")
        val looksFrench = frHints.count { sample.contains(it) } >= 2 && !sourceCvFileName.contains("english", ignoreCase = true)
        val slot = CvDocumentSlot(
            fileName = sourceCvFileName.ifBlank { if (looksFrench) "CV_Francais.pdf" else "CV_English.pdf" },
            format = sourceCvFormat.ifBlank { "PDF" },
            filePath = sourceCvPath,
            rawText = rawCvContent,
            isModified = isModifiedCv,
            parsedAt = parsedAt
        )
        return if (looksFrench) copy(frenchCv = slot) else copy(englishCv = slot)
    }
}
