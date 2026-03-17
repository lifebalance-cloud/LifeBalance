package com.example.mylife.lifebalance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

@TypeConverters(DateConverter::class, TimeConverter::class, RepeatTypeConverter::class)
@Database(
    entities = [LifeSphere::class, Task::class, Goal::class, IdeaFolder::class, IdeaNote::class, User::class, DreamSectorPhoto::class, DreamAffirmation::class],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lifeSphereDao(): LifeSphereDao
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun ideaFolderDao(): IdeaFolderDao
    abstract fun ideaNoteDao(): IdeaNoteDao
    abstract fun userDao(): UserDao
    abstract fun dreamSectorPhotoDao(): DreamSectorPhotoDao
    abstract fun dreamAffirmationDao(): DreamAffirmationDao

    companion object {
        private const val DB_NAME = "life_balance_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                // Загрузка нативных библиотек SQLCipher перед первым использованием
                SQLiteDatabase.loadLibs(appContext)

                val passphrase = SecureStorageHelper.getOrCreateDbPassphrase(appContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                // Принудительное открытие БД: при обновлении с незашифрованной БД
                // SQLCipher выбросит исключение — удаляем старые файлы и создаём зашифрованную БД заново.
                try {
                    instance.openHelper.writableDatabase
                } catch (e: Exception) {
                    if (isLikelyUnencryptedDbError(e)) {
                        deleteDatabaseFiles(appContext)
                        INSTANCE = null
                        return getDatabase(context)
                    }
                    throw e
                }

                INSTANCE = instance
                instance
            }
        }

        private fun isLikelyUnencryptedDbError(e: Exception): Boolean {
            val msg = e.message?.lowercase() ?: ""
            return msg.contains("file is not a database") ||
                msg.contains("not a database") ||
                msg.contains("sqlite_format") ||
                msg.contains("encrypted")
        }

        private fun deleteDatabaseFiles(context: Context) {
            val dbPath = context.getDatabasePath(DB_NAME)
            val dir = dbPath.parentFile ?: return
            for (name in listOf(DB_NAME, "$DB_NAME-wal", "$DB_NAME-shm")) {
                File(dir, name).takeIf { it.exists() }?.delete()
            }
        }
    }
}
