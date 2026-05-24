package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String, // email do usuário
    val nome: String,
    val curso: String,
    val periodo: String,
    val bio: String,
    val foto_url: String, // fallback ou link
    val selected_materia: String = "",
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "roles")
data class UserRole(
    @PrimaryKey val user_id: String, // id do perfil
    val role: String, // super_admin, moderador, aluno
    val permissions: String = "delete_posts,manage_events,moderate_chats", // comma-separated custom options
    val principal_area: String = "Pedagogia (UERJ)", // principal area of responsibility
    val online: Boolean = true // online state for showing animations
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author_id: String,
    val author_name: String,
    val author_avatar: String,
    val subject_id: Int?, // opcional
    val content: String,
    val media_url: String?, // imagem ou anexo (até 1 única mídia)
    val created_at: Long = System.currentTimeMillis(),
    val likes_count: Int = 0
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val post_id: Int,
    val author_id: String,
    val author_name: String,
    val author_avatar: String,
    val content: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_likes", primaryKeys = ["post_id", "user_id"])
data class PostLike(
    val post_id: Int,
    val user_id: String
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String
)

@Entity(tableName = "mindmaps")
data class Mindmap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val image_url: String, // link ou resource string
    val author_id: String,
    val author_name: String,
    val tags: String, // tags separadas por vírgula
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "mindmap_favorites", primaryKeys = ["mindmap_id", "user_id"])
data class MindmapFavorite(
    val mindmap_id: Int,
    val user_id: String
)

@Entity(tableName = "help_requests")
data class HelpRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val subject_id: Int,
    val author_id: String,
    val author_name: String,
    val is_resolved: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val media_url: String? = null,
    val author_id: String,
    val author_name: String,
    val author_role: String,
    val room_tag: String? = null,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_id: String,
    val title: String,
    val body: String,
    val read: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_groups")
data class StudyGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val created_by: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_threads")
data class GroupThread(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val group_id: Int,
    val title: String,
    val content: String,
    val author_id: String,
    val author_name: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_comments")
data class GroupComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val thread_id: Int,
    val content: String,
    val author_id: String,
    val author_name: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: Long,
    val category: String, // avaliação, aviso, evento
    val created_by: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moderator_id: String,
    val moderator_name: String,
    val action: String, // e.g., "Post Deletado", "Evento Criado"
    val target: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_materials")
data class StudyMaterial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val file_url: String, // link or representation
    val type: String, // "material", "prova", "audiobook"
    val author_id: String,
    val author_name: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "chamadas_ativas")
data class ActiveCall(
    @PrimaryKey val id: Int,
    val zoom_meeting_id: String,
    val zoom_password: String,
    val status: String,
    val criado_por: String,
    val subject_id: Int?,
    val created_at: Long = System.currentTimeMillis()
)
