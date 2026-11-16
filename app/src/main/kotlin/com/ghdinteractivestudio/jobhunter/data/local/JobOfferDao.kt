package com.ghdinteractivestudio.jobhunter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobOfferDao {
    @Query("SELECT * FROM job_offers ORDER BY detectedAt DESC")
    fun getAllOffers(): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE status = :status ORDER BY detectedAt DESC")
    fun getOffersByStatus(status: String): Flow<List<JobOfferEntity>>

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

    @Update
    suspend fun update(offer: JobOfferEntity)

    @Query("UPDATE job_offers SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("DELETE FROM job_offers WHERE id = :id")
    suspend fun deleteById(id: String)
}
