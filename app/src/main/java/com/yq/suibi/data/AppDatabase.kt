package com.yq.suibi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Topic::class, Note::class, Discussion::class, Attachment::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun topicDao(): TopicDao
    abstract fun noteDao(): NoteDao
    abstract fun discussionDao(): DiscussionDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun searchDao(): SearchDao

    companion object {

        /** v2：话题与笔记增加「置顶」和「标记颜色」。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE topics ADD COLUMN color INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER")
            }
        }

        /** v3：新增附件表，笔记与讨论都能挂图片/文件。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER,
                        `discussionId` INTEGER,
                        `kind` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `size` INTEGER NOT NULL,
                        `relPath` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`discussionId`) REFERENCES `discussions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_noteId` ON `attachments` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_discussionId` ON `attachments` (`discussionId`)")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suibi.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}