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
data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val title: String = "",
    val summary: String = "",
    val skills: List<String> = emptyList(),
    val experiences: List<ExperienceItem> = emptyList(),
    val educations: List<EducationItem> = emptyList(),
    val languages: List<String> = emptyList(),
    val sourceCvPath: String = "",
    val sourceCvFormat: String = "PDF"
)
