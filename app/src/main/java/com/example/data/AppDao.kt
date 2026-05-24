package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Profiles
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): Profile?

    @Query("SELECT * FROM profiles")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<Profile>)

    @Update
    suspend fun updateProfile(profile: Profile)

    // Roles
    @Query("SELECT * FROM roles WHERE user_id = :userId")
    suspend fun getRoleByUserId(userId: String): UserRole?

    @Query("SELECT * FROM roles")
    fun getAllRolesFlow(): Flow<List<UserRole>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: UserRole)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<UserRole>)

    @Query("DELETE FROM roles WHERE user_id = :userId")
    suspend fun deleteRole(userId: String)

    // Posts
    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE subject_id = :subjectId ORDER BY created_at DESC")
    fun getPostsBySubject(subjectId: Int): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    // Comments
    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at ASC")
    fun getCommentsForPost(postId: Int): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: Int)

    @Query("DELETE FROM comments WHERE post_id = :postId")
    suspend fun deleteCommentsForPost(postId: Int)

    // Post Likes
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLike)

    @Delete
    suspend fun deleteLike(like: PostLike)

    @Query("SELECT * FROM post_likes WHERE post_id = :postId AND user_id = :userId")
    suspend fun getLike(postId: Int, userId: String): PostLike?

    @Query("SELECT * FROM post_likes WHERE user_id = :userId")
    fun getLikesForUser(userId: String): Flow<List<PostLike>>

    // Subjects
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    // Mindmaps
    @Query("SELECT * FROM mindmaps ORDER BY created_at DESC")
    fun getAllMindmaps(): Flow<List<Mindmap>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindmap(mindmap: Mindmap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindmaps(mindmaps: List<Mindmap>)

    // Mindmap Favorites
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMindmapFavorite(fav: MindmapFavorite)

    @Delete
    suspend fun deleteMindmapFavorite(fav: MindmapFavorite)

    @Query("SELECT * FROM mindmap_favorites WHERE user_id = :userId")
    fun getFavoritesForUser(userId: String): Flow<List<MindmapFavorite>>

    // Help Requests
    @Query("SELECT * FROM help_requests ORDER BY created_at DESC")
    fun getAllHelpRequests(): Flow<List<HelpRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpRequest(req: HelpRequest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpRequests(reqs: List<HelpRequest>)

    @Query("UPDATE help_requests SET is_resolved = :resolved WHERE id = :id")
    suspend fun updateHelpRequestStatus(id: Int, resolved: Boolean)

    // Chat Messages
    @Query("SELECT * FROM messages ORDER BY created_at ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(msgs: List<Message>)

    // Notifications
    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: Notification)

    @Query("UPDATE notifications SET read = 1 WHERE user_id = :userId")
    suspend fun markAllNotificationsAsRead(userId: String)

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    // Study Groups
    @Query("SELECT * FROM study_groups ORDER BY created_at DESC")
    fun getAllStudyGroups(): Flow<List<StudyGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroup(group: StudyGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroups(groups: List<StudyGroup>)

    // Group Threads
    @Query("SELECT * FROM group_threads WHERE group_id = :groupId ORDER BY created_at DESC")
    fun getThreadsForGroup(groupId: Int): Flow<List<GroupThread>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupThread(thread: GroupThread)

    @Query("DELETE FROM group_threads WHERE id = :id")
    suspend fun deleteGroupThread(id: Int)

    // Group Comments
    @Query("SELECT * FROM group_comments WHERE thread_id = :threadId ORDER BY created_at ASC")
    fun getCommentsForThread(threadId: Int): Flow<List<GroupComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupComment(comment: GroupComment)

    // Calendar Events
    @Query("SELECT * FROM calendar_events ORDER BY date ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteCalendarEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEventById(id: Int)

    // Audit logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // Study Materials (Provas e Audiobooks)
    @Query("SELECT * FROM study_materials ORDER BY created_at DESC")
    fun getAllStudyMaterials(): Flow<List<StudyMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyMaterial(material: StudyMaterial)

    @Query("DELETE FROM study_materials WHERE id = :id")
    suspend fun deleteStudyMaterialById(id: Int)

    // Direct deletions for Admin/SuperAdmin System-Wide CRUD Console
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Int)

    @Query("DELETE FROM mindmaps WHERE id = :id")
    suspend fun deleteMindmapById(id: Int)

    @Query("DELETE FROM help_requests WHERE id = :id")
    suspend fun deleteHelpRequestById(id: Int)

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteStudyGroupById(id: Int)

    // Active Calls (Zoom)
    @Query("SELECT * FROM chamadas_ativas WHERE status = 'ativa'")
    fun getActiveCallsFlow(): Flow<List<ActiveCall>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveCalls(calls: List<ActiveCall>)

    @Query("DELETE FROM chamadas_ativas")
    suspend fun clearActiveCalls()
}
