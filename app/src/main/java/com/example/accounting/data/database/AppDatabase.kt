package com.example.accounting.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.accounting.data.dao.UserDao
import com.example.accounting.data.model.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {

        // 这里的可见性是为了配合下面的创建instance，可以让instance创建好其他线程可以立马看到
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // 如果 INSTANCE 不为空直接返回，如果为空则同步创建，主要防止都创建了数据库实例可能会违反单例原则
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accounting_database" // 数据库文件名
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}