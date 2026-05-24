package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Profile::class,
        UserRole::class,
        Post::class,
        Comment::class,
        PostLike::class,
        Subject::class,
        Mindmap::class,
        MindmapFavorite::class,
        HelpRequest::class,
        Message::class,
        Notification::class,
        StudyGroup::class,
        GroupThread::class,
        GroupComment::class,
        CalendarEvent::class,
        AuditLog::class,
        StudyMaterial::class,
        ActiveCall::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
