package com.ghdinteractivestudio.jobhunter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobOfferDao {
    @Query("SELECT * FROM job_offers ORDER BY matchScore DESC, detectedAt DESC")
    fun getAllOffers(): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE status = :status ORDER BY detectedAt DESC")
    fun getOffersByStatus(status: String): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE status = :status ORDER BY appliedAt ASC")
    suspend fun getOffersByStatusOnce(status: String): List<JobOfferEntity>

    @Query("SELECT * FROM job_offers WHERE status = 'DETECTED' OR (status = 'ANALYZED' AND aiVerdict != 'NON_ADAPTE' AND matchScore >= 50) ORDER BY matchScore DESC, detectedAt DESC LIMIT :limit")
    suspend fun getTopAutoApplyCandidates(limit: Int): List<JobOfferEntity>

    @Query("SELECT * FROM job_offers WHERE status IN ('APPLIED', 'INTERVIEW', 'READY_TO_SEND') ORDER BY appliedAt DESC")
    suspend fun getTrackedFollowUpCandidates(): List<JobOfferEntity>

    @Query("SELECT * FROM job_offers WHERE status IN ('APPLIED', 'INTERVIEW', 'REJECTED')")
    suspend fun getAppliedSignatures(): List<JobOfferEntity>

    @Query("SELECT * FROM job_offers WHERE status = 'DETECTED'")
    suspend fun getDetectedOffersOnce(): List<JobOfferEntity>

    @Query("SELECT COUNT(*) FROM job_offers WHERE autoApplied = 1 AND appliedAt >= :sinceMillis")
    suspend fun countAutoAppliedSince(sinceMillis: Long): Int

    @Query("UPDATE job_offers SET status = 'DETECTED', appliedAt = 0, appliedChannel = '', followUpCount = 0, lastFollowUpAt = 0, lastReplySummary = '', lastReplyFrom = '', lastReplySubject = '', lastReplyAt = 0, lastReplyMessageId = '', autoApplied = 0, lastAutoAttemptAt = 0, generatedLetter = '', tailoredCvPath = '', letterPath = '' WHERE status IN ('ANALYZED', 'READY_TO_SEND', 'APPLIED', 'INTERVIEW', 'REJECTED')")
    suspend fun resetAllPipelineCounters(): Int

    @Query("UPDATE job_offers SET recruiterEmail = '', applicationMethod = 'WEBSITE' WHERE lower(recruiterEmail) LIKE '%accommodations%' OR lower(recruiterEmail) LIKE '%accessibility%' OR lower(recruiterEmail) LIKE '%privacy%' OR lower(recruiterEmail) LIKE '%legal@%' OR lower(recruiterEmail) LIKE '%noreply%'")
    suspend fun sanitizeInvalidRecruiterEmails(): Int

    @Query("SELECT * FROM job_offers WHERE status != 'DETECTED' ORDER BY appliedAt DESC, detectedAt DESC")
    fun getPipelineOffers(): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE id = :id")
    suspend fun getOfferById(id: String): JobOfferEntity?

    @Query("SELECT COUNT(*) FROM job_offers")
    fun countTotalOffers(): Flow<Int>

    @Query("SELECT COUNT(*) FROM job_offers WHERE status = 'APPLIED'")
    fun countAppliedOffers(): Flow<Int>

    @Query("SELECT COUNT(*) FROM job_offers WHERE status = 'INTERVIEW'")
    fun countInterviewOffers(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(offer: JobOfferEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(offers: List<JobOfferEntity>)

    @Query("DELETE FROM job_offers WHERE status = 'DETECTED'")
    suspend fun clearDetectedOffers()

    @Query("DELETE FROM job_offers WHERE status = 'DETECTED' AND ((deadlineAt > 0 AND deadlineAt < :nowMillis) OR (publishedAt > 0 AND publishedAt < :minPublishedAt))")
    suspend fun purgeExpiredDetectedOffers(nowMillis: Long, minPublishedAt: Long)

    @Update
    suspend fun update(offer: JobOfferEntity)

    @Query("UPDATE job_offers SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("DELETE FROM job_offers WHERE id = :id")
    suspend fun deleteById(id: String)
}
