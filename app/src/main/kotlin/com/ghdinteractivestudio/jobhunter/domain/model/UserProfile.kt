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

    fun isInvalidName(name: String): Boolean {
        val trimmed = name.trim().lowercase()
        return trimmed.isBlank() ||
                trimmed == "curriculum vitae" ||
                trimmed == "curriculum vitæ" ||
                trimmed == "cv" ||
                trimmed == "resume" ||
                trimmed == "résumé" ||
                trimmed.startsWith("curriculum")
    }

    fun effectiveFullName(): String {
        val trimmedName = fullName.trim()
        if (!isInvalidName(trimmedName)) return trimmedName
        val trimmedTitle = title.trim()
        if (!isInvalidName(trimmedTitle) && trimmedTitle.split(Regex("\\s+")).size in 2..4 &&
            trimmedTitle.none { it.isDigit() } &&
            !trimmedTitle.contains("ingénieur", ignoreCase = true) &&
            !trimmedTitle.contains("engineer", ignoreCase = true) &&
            !trimmedTitle.contains("métreur", ignoreCase = true)
        ) {
            return trimmedTitle
        }
        return "GHEDIRI MOHAMED REDHA"
    }

    fun effectiveTitle(): String {
        val effName = effectiveFullName().lowercase()
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank() || trimmedTitle.lowercase() == effName || isInvalidName(trimmedTitle)) {
            return "Ingénieur Travaux Publics / Métreur-Vérificateur"
        }
        return trimmedTitle
    }

    /**
     * Ensures single-CV profiles imported prior to dual EN/FR slots automatically map
     * cleanly into englishCv or frenchCv without data loss and heals legacy corrupt headers.
     */
    fun migrateLegacySlotIfNeeded(): UserProfile {
        var prof = this
        val trimmedName = prof.fullName.trim()
        val isCorruptName = prof.isInvalidName(trimmedName)
        val trimmedTitle = prof.title.trim()
        val isTitleCandidateName = trimmedTitle.equals("GHEDIRI MOHAMED REDHA", ignoreCase = true) ||
                (trimmedTitle.isNotBlank() && trimmedTitle.equals(trimmedName, ignoreCase = true))

        if (isCorruptName || isTitleCandidateName) {
            prof = prof.copy(
                fullName = prof.effectiveFullName(),
                title = prof.effectiveTitle()
            )
        }

        if (prof.englishCv != null || prof.frenchCv != null || prof.rawCvContent.isBlank()) return prof
        val sample = "${prof.sourceCvFileName} ${prof.rawCvContent.take(600)}".lowercase()
        val frHints = listOf("expérience", "compétences", "formation", "langues", "français", "francais", "algérie", "poste")
        val looksFrench = frHints.count { sample.contains(it) } >= 2 && !prof.sourceCvFileName.contains("english", ignoreCase = true)
        val slot = CvDocumentSlot(
            fileName = prof.sourceCvFileName.ifBlank { if (looksFrench) "CV_Francais.pdf" else "CV_English.pdf" },
            format = prof.sourceCvFormat.ifBlank { "PDF" },
            filePath = prof.sourceCvPath,
            rawText = prof.rawCvContent,
            isModified = prof.isModifiedCv,
            parsedAt = prof.parsedAt
        )
        return if (looksFrench) prof.copy(frenchCv = slot) else prof.copy(englishCv = slot)
    }
}
