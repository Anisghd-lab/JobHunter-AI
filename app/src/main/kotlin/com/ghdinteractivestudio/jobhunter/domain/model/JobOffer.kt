package com.ghdinteractivestudio.jobhunter.domain.model

import kotlinx.serialization.Serializable

enum class JobStatus {
    DETECTED,
    ANALYZED,
    READY_TO_SEND,
    APPLIED,
    INTERVIEW,
    REJECTED,
    ARCHIVED
}

enum class DocumentFormatRequirement {
    PDF,
    DOCX,
    ANY
}

@Serializable
data class JobOffer(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val isRemote: Boolean = false,
    val contractType: String = "CDI",
    val description: String,
    val sourcePlatform: String,
    val url: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val matchScore: Int = 0,
    val matchingHighlights: List<String> = emptyList(),
    val missingRequirements: List<String> = emptyList(),
    val status: JobStatus = JobStatus.DETECTED,
    val requiredFormat: DocumentFormatRequirement = DocumentFormatRequirement.ANY,
    val recruiterEmail: String = "",
    val generatedLetter: String = "",
    val tailoredCvPath: String = "",
    val letterPath: String = "",
    val tags: List<String> = emptyList(),
    val regionScope: String = "INTERNATIONAL", // "ALGERIA", "GULF", or "INTERNATIONAL"
    val workMode: String = "REMOTE", // "REMOTE", "ONSITE", "HYBRID"
    val applicationMethod: String = "WEBSITE", // "EMAIL" or "WEBSITE"
    val salary: String = "",
    val publishedDate: String = "",
    val aiVerdict: String = "", // "CONVIENT", "MOYEN", "NON_ADAPTE"
    val aiReasoning: String = "",
    val appliedAt: Long = 0L,
    val appliedChannel: String = "",
    val publishedAt: Long = 0L,
    val deadlineAt: Long = 0L,
    val deadlineLabel: String = "",
    val followUpCount: Int = 0,
    val lastFollowUpAt: Long = 0L,
    val lastReplySummary: String = "",
    val autoApplied: Boolean = false,
    val lastAutoAttemptAt: Long = 0L,
    val lastReplyFrom: String = "",
    val lastReplySubject: String = "",
    val lastReplyAt: Long = 0L,
    val lastReplyMessageId: String = ""
)
