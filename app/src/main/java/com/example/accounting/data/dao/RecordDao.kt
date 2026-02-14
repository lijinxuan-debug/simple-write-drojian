package com.example.accounting.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.accounting.data.model.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    // 保存账单
    @Insert
    suspend fun insertRecord(record: Record)

    // 查询当前用户的所有账单
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY timestamp DESC")
    fun selectAllRecord(userId: Long): Flow<List<Record>>
}