package com.example.accounting.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.accounting.data.model.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    /**
     * 编辑完保存账单
     */
    @Upsert
    suspend fun insertRecord(record: Record)

    /**
     * 删除对应账单
     */
    @Query("DELETE FROM records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Long)

    /**
     * 查询当前用户的所有账单
     */
    @Query("SELECT * FROM records WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun selectRecordsByMonth(userId: Long, startTime: Long, endTime: Long): Flow<List<Record>>

    /**
     * 查询当前用户所有记录的最早时间戳
     */
    @Query("SELECT MIN(timestamp) FROM records WHERE userId = :userId")
    suspend fun getMinTimestamp(userId: Long): Long?

    /**
     * 查询当前用户所有记录的最晚时间戳
     */
    @Query("SELECT MAX(timestamp) FROM records WHERE userId = :userId")
    suspend fun getMaxTimestamp(userId: Long): Long?
}