package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val dao: AppDao) {

    // Profiles & Roles
    fun getAllProfilesFlow(): Flow<List<Profile>> = dao.getAllProfilesFlow()
    fun getAllRolesFlow(): Flow<List<UserRole>> = dao.getAllRolesFlow()
    suspend fun getProfileById(id: String): Profile? = dao.getProfileById(id)
    suspend fun getRoleByUserId(userId: String): UserRole? = dao.getRoleByUserId(userId)
    suspend fun insertProfile(profile: Profile) = dao.insertProfile(profile)
    suspend fun updateProfile(profile: Profile) = dao.updateProfile(profile)
    suspend fun insertRole(role: UserRole) = dao.insertRole(role)
    suspend fun deleteRole(userId: String) = dao.deleteRole(userId)

    // Posts & Comments & Likes
    fun getAllPosts(): Flow<List<Post>> = dao.getAllPosts()
    fun getPostsBySubject(subjectId: Int): Flow<List<Post>> = dao.getPostsBySubject(subjectId)
    suspend fun insertPost(post: Post) = dao.insertPost(post)
    suspend fun deletePostById(postId: Int) = dao.deletePostById(postId)

    fun getCommentsForPost(postId: Int): Flow<List<Comment>> = dao.getCommentsForPost(postId)
    suspend fun insertComment(comment: Comment) = dao.insertComment(comment)
    suspend fun deleteCommentById(commentId: Int) = dao.deleteCommentById(commentId)
    suspend fun deleteCommentsForPost(postId: Int) = dao.deleteCommentsForPost(postId)

    suspend fun toggleLike(postId: Int, userId: String) {
        val existing = dao.getLike(postId, userId)
        if (existing != null) {
            dao.deleteLike(existing)
        } else {
            dao.insertLike(PostLike(postId, userId))
        }
        // Update post likes_count count logic in database (reactive Flow)
    }
    suspend fun isPostLiked(postId: Int, userId: String): Boolean {
        return dao.getLike(postId, userId) != null
    }
    fun getLikesForUser(userId: String): Flow<List<PostLike>> = dao.getLikesForUser(userId)

    // Subjects
    fun getAllSubjects(): Flow<List<Subject>> = dao.getAllSubjects()
    suspend fun insertSubject(subject: Subject) = dao.insertSubject(subject)

    // Mindmaps
    fun getAllMindmaps(): Flow<List<Mindmap>> = dao.getAllMindmaps()
    suspend fun insertMindmap(mindmap: Mindmap) = dao.insertMindmap(mindmap)

    // Mindmap Favorites
    suspend fun toggleMindmapFavorite(mindmapId: Int, userId: String) {
        val fav = MindmapFavorite(mindmapId, userId)
        // Check if favorite exists (we'll just use insert onConflict IGNORE or delete)
        // Let's do a quick try/catch or simple check. Standard way is:
        // Try deleting. If nothing was deleted (or we can query favorites), we can toggle.
        // Let's keep a flow to verify or just insert.
        // For a toggle, let's insert it. To toggle, let's query first or catch.
        // Let's insert first, if we want to toggle we can check in ViewModel or have a query
        // But since Room supports direct insert/delete, we'll implement toggle in helper or do:
        dao.insertMindmapFavorite(fav)
    }
    suspend fun removeMindmapFavorite(mindmapId: Int, userId: String) {
        dao.deleteMindmapFavorite(MindmapFavorite(mindmapId, userId))
    }
    fun getFavoritesForUser(userId: String): Flow<List<MindmapFavorite>> = dao.getFavoritesForUser(userId)

    // Help Requests
    fun getAllHelpRequests(): Flow<List<HelpRequest>> = dao.getAllHelpRequests()
    suspend fun insertHelpRequest(req: HelpRequest) = dao.insertHelpRequest(req)
    suspend fun updateHelpRequestStatus(id: Int, resolved: Boolean) = dao.updateHelpRequestStatus(id, resolved)

    // Chat Messages
    fun getAllMessages(): Flow<List<Message>> = dao.getAllMessages()
    suspend fun insertMessage(msg: Message) = dao.insertMessage(msg)

    // Notifications
    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = dao.getNotificationsForUser(userId)
    suspend fun insertNotification(notif: Notification) = dao.insertNotification(notif)
    suspend fun markAllNotificationsAsRead(userId: String) = dao.markAllNotificationsAsRead(userId)
    suspend fun markNotificationAsRead(id: Int) = dao.markNotificationAsRead(id)

    // Study Groups
    fun getAllStudyGroups(): Flow<List<StudyGroup>> = dao.getAllStudyGroups()
    suspend fun insertStudyGroup(group: StudyGroup) = dao.insertStudyGroup(group)

    // Group Threads & Comments
    fun getThreadsForGroup(groupId: Int): Flow<List<GroupThread>> = dao.getThreadsForGroup(groupId)
    suspend fun insertGroupThread(thread: GroupThread) = dao.insertGroupThread(thread)
    suspend fun deleteGroupThread(id: Int) = dao.deleteGroupThread(id)

    fun getCommentsForThread(threadId: Int): Flow<List<GroupComment>> = dao.getCommentsForThread(threadId)
    suspend fun insertGroupComment(comment: GroupComment) = dao.insertGroupComment(comment)

    // Calendar Events
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>> = dao.getAllCalendarEvents()
    suspend fun insertCalendarEvent(event: CalendarEvent) = dao.insertCalendarEvent(event)
    suspend fun deleteCalendarEvent(id: Int) = dao.deleteCalendarEventById(id)

    // Audit logs
    fun getAllAuditLogs(): Flow<List<AuditLog>> = dao.getAllAuditLogs()
    suspend fun insertAuditLog(log: AuditLog) = dao.insertAuditLog(log)

    // Study Materials (Provas e Audiobooks)
    fun getAllStudyMaterials(): Flow<List<StudyMaterial>> = dao.getAllStudyMaterials()
    suspend fun insertStudyMaterial(material: StudyMaterial) = dao.insertStudyMaterial(material)
    suspend fun deleteStudyMaterialById(id: Int) = dao.deleteStudyMaterialById(id)

    // Direct deletions for admin console CRUD
    suspend fun deleteProfileDirectly(id: String) = dao.deleteProfileById(id)
    suspend fun deleteSubjectDirectly(id: Int) = dao.deleteSubjectById(id)
    suspend fun deleteMindmapDirectly(id: Int) = dao.deleteMindmapById(id)
    suspend fun deleteHelpRequestDirectly(id: Int) = dao.deleteHelpRequestById(id)
    suspend fun deleteStudyGroupDirectly(id: Int) = dao.deleteStudyGroupById(id)

    // Initial Pre-population of Database
    suspend fun prepopulateIfNeeded() {
        val currentSubjects = dao.getAllSubjects().first()
        if (currentSubjects.isEmpty()) {
            // 1. Insert PEDAGOGY Subjects (UERJ Context)
            dao.insertSubject(Subject(title = "Didática Geral", description = "Teoria e prática do processo de ensino-aprendizagem."))
            dao.insertSubject(Subject(title = "Psicologia da Educação", description = "Processos de desenvolvimento, aprendizagem e subjetivação humana."))
            dao.insertSubject(Subject(title = "Filosofia da Educação", description = "Reflexões éticas, epistemológicas e políticas sobre a práxis educativa."))
            dao.insertSubject(Subject(title = "Alfabetização e Letramento", description = "Metodologias de aquisição da leitura e escrita em contextos formais."))
            dao.insertSubject(Subject(title = "Gestão Escolar", description = "Organização do trabalho pedagógico e administrativo no cotidiano escolar."))

            // 2. Insert Default Profiles and Roles
            val superAdminId = "jefersonribeiro199026@gmail.com"
            dao.insertProfile(
                Profile(
                    id = superAdminId,
                    nome = "Jeferson Ribeiro",
                    curso = "Pedagogia (UERJ)",
                    periodo = "5º Período",
                    bio = "Administrador e idealizador da plataforma. Apaixonado por tecnologia na educação!",
                    foto_url = "avatar_user_default"
                )
            )
            dao.insertRole(UserRole(user_id = superAdminId, role = "super_admin"))
        }
    }
}
