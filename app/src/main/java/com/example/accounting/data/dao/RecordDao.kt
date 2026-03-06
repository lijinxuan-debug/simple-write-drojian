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
     * 查询当前用户的所有账单（时间范围内）
     */
    @Query("SELECT * FROM records WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun selectRecordsByMonth(userId: Long, startTime: Long, endTime: Long): Flow<List<Record>>

    /**
     * 查询当前用户的所有账单（无时间限制，手动调用）
     * 使用 suspend 关键字，配合协程在后台执行
     */
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllRecords(userId: Long): List<Record>

    /**
     * 查询当前用户所有记录的最早时间戳
     */
    @Query("SELECT MIN(timestamp) FROM records WHERE userId = :userId")
    fun getMinTimestampFlow(userId: Long): Flow<Long?>

    /**
     * 查询当前用户所有记录的最晚时间戳
     */
    @Query("SELECT MAX(timestamp) FROM records WHERE userId = :userId")
    fun getMaxTimestampFlow(userId: Long): Flow<Long?>

    // 检查是否有更早的数据
    @Query("SELECT EXISTS(SELECT 1 FROM records WHERE timestamp > :timestamp AND userId = :userId LIMIT 1)")
    suspend fun hasFutureData(timestamp: Long,userId: Long): Boolean

    // 检查是否有更早的数据（过去）
    @Query("SELECT EXISTS(SELECT 1 FROM records WHERE timestamp < :timestamp AND userId = :userId LIMIT 1)")
    suspend fun hasPastData(timestamp: Long,userId: Long): Boolean
}