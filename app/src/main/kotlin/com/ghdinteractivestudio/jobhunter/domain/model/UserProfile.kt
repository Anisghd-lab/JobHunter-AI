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
    val parsedAt: Long = 0L
)
