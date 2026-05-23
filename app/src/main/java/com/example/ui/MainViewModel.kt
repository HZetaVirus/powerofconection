package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository Initialization
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "power_connection_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = AppRepository(database.dao())

    // ------------------------------------------------------------------------
    // Auth State - Defaulting to Empty to force Login Screen at startup
    // ------------------------------------------------------------------------
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<Profile?>(null)
    val currentUserProfile: StateFlow<Profile?> = _currentUserProfile.asStateFlow()

    private val _currentUserRole = MutableStateFlow("convidado")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    // ------------------------------------------------------------------------
    // Reactive Flows from Database
    // ------------------------------------------------------------------------
    val allProfiles: StateFlow<List<Profile>> = repository.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allRoles: StateFlow<List<UserRole>> = repository.getAllRolesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPosts: StateFlow<List<Post>> = repository.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allSubjects: StateFlow<List<Subject>> = repository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMindmaps: StateFlow<List<Mindmap>> = repository.getAllMindmaps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allHelpRequests: StateFlow<List<HelpRequest>> = repository.getAllHelpRequests()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMessages: StateFlow<List<Message>> = repository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allStudyMaterials: StateFlow<List<StudyMaterial>> = repository.getAllStudyMaterials()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allStudyGroups: StateFlow<List<StudyGroup>> = repository.getAllStudyGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCalendarEvents: StateFlow<List<CalendarEvent>> = repository.getAllCalendarEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    companion object {
        val PoloCoursesList = listOf(
            "Administração Pública/UFF",
            "Biblioteconomia/UNIRIO",
            "Ciências Contábeis/UFRJ",
            "Engenharia de Produção/CEFET",
            "Lic. Ciências Biológicas/UERJ",
            "Lic. Matemática/UNIRIO",
            "Lic. Pedagogia/UERJ",
            "Tec. Computação/UFF",
            "Tec. Segurança Pública/UFF"
        )

        fun normalizeCourse(course: String?): String {
            if (course.isNullOrBlank()) return "Lic. Pedagogia/UERJ"
            val trimmed = course.trim()
            if (trimmed == "Pedagogia (UERJ)" || trimmed.contains("Pedagogia", ignoreCase = true)) {
                return "Lic. Pedagogia/UERJ"
            }
            return trimmed
        }

        fun getDefaultSubjectForCourse(course: String, period: String): String {
            val lower = course.lowercase()
            return when {
                lower.contains("computação") -> "Ciência da Computação"
                lower.contains("pedagogia") -> "Pedagogia"
                lower.contains("administração") -> "Administração Pública"
                lower.contains("biblioteconomia") -> "Biblioteconomia"
                lower.contains("contábeis") -> "Ciências Contábeis"
                lower.contains("produção") -> "Engenharia de Produção"
                lower.contains("biológicas") -> "Ciências Biológicas"
                lower.contains("matemática") -> "Matemática"
                lower.contains("segurança") -> "Segurança Pública"
                else -> {
                    val firstPart = course.split("/").firstOrNull()?.trim() ?: course
                    var cleaned = firstPart
                    if (cleaned.startsWith("Lic. ", ignoreCase = true)) {
                        cleaned = cleaned.substring(5)
                    } else if (cleaned.startsWith("Tec. ", ignoreCase = true)) {
                        cleaned = cleaned.substring(5)
                    }
                    cleaned
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Filtered Lists Scoped by Course & Period
    // ------------------------------------------------------------------------
    val filteredProfiles: StateFlow<List<Profile>> = combine(allProfiles, currentUserProfile) { profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        profilesList.filter { normalizeCourse(it.curso) == myCourse }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredPosts: StateFlow<List<Post>> = combine(allPosts, allProfiles, currentUserProfile) { postsList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        postsList.filter { post ->
            val author = profilesList.find { it.id == post.author_id }
            author == null || normalizeCourse(author.curso) == myCourse
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredMindmaps: StateFlow<List<Mindmap>> = combine(allMindmaps, allProfiles, currentUserProfile) { mindmapsList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        mindmapsList.filter { map ->
            val author = profilesList.find { it.id == map.author_id }
            author == null || normalizeCourse(author.curso) == myCourse
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredHelpRequests: StateFlow<List<HelpRequest>> = combine(allHelpRequests, allProfiles, currentUserProfile) { reqsList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        reqsList.filter { req ->
            val author = profilesList.find { it.id == req.author_id }
            author == null || normalizeCourse(author.curso) == myCourse
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredMessages: StateFlow<List<Message>> = combine(allMessages, allProfiles, currentUserProfile) { messagesList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        val myPeriod = currentProfile?.periodo ?: "1º Período"
        val myMateria = currentProfile?.selected_materia ?: getDefaultSubjectForCourse(myCourse, myPeriod)
        val safeCourse = myCourse.replace(" ", "_").replace("/", "_")
        val safePeriod = myPeriod.replace(" ", "_")
        val safeMateria = myMateria.replace(" ", "_")
        val myRoomTag = "room_${safeCourse}_${safePeriod}_${safeMateria}"
        messagesList.filter { msg ->
            msg.room_tag == myRoomTag || (msg.room_tag == null && myPeriod == "1º Período" && myMateria == getDefaultSubjectForCourse("Lic. Pedagogia/UERJ", "1º Período") && myCourse == "Lic. Pedagogia/UERJ")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredStudyGroups: StateFlow<List<StudyGroup>> = combine(allStudyGroups, allProfiles, currentUserProfile) { groupsList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        groupsList.filter { group ->
            val creator = profilesList.find { it.id == group.created_by }
            creator == null || normalizeCourse(creator.curso) == myCourse
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredCalendarEvents: StateFlow<List<CalendarEvent>> = combine(allCalendarEvents, allProfiles, currentUserProfile) { eventsList, profilesList, currentProfile ->
        val myCourse = normalizeCourse(currentProfile?.curso)
        eventsList.filter { evt ->
            val creator = profilesList.find { it.id == evt.created_by }
            creator == null || normalizeCourse(creator.curso) == myCourse
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Direct User Lists derived
    val userFavorites: StateFlow<List<MindmapFavorite>> = currentUserId
        .flatMapLatest { userId -> repository.getFavoritesForUser(userId) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userLikes: StateFlow<List<PostLike>> = currentUserId
        .flatMapLatest { userId -> repository.getLikesForUser(userId) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userNotifications: StateFlow<List<Notification>> = currentUserId
        .flatMapLatest { userId -> repository.getNotificationsForUser(userId) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ------------------------------------------------------------------------
    // Screen / View State UI Managers
    // ------------------------------------------------------------------------
    var searchGlobalQuery = MutableStateFlow("")
    var filteredSubjectId = MutableStateFlow<Int?>(null)

    // Chat flood management
    private var lastMessageTime: Long = 0
    val chatError = MutableSharedFlow<String>()

    // Authentication Register / Log Error Feed
    val authError = MutableSharedFlow<String>()

    fun getCommentsForPost(postId: Int): Flow<List<Comment>> = repository.getCommentsForPost(postId)
    fun getThreadsForGroup(groupId: Int): Flow<List<GroupThread>> = repository.getThreadsForGroup(groupId)
    fun getCommentsForThread(threadId: Int): Flow<List<GroupComment>> = repository.getCommentsForThread(threadId)

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Seed database
            try {
                repository.prepopulateIfNeeded()
                // Force reload active profile after seed is complete to resolve race condition
                val email = currentUserId.value
                val profile = repository.getProfileById(email)
                _currentUserProfile.value = profile
                val uRole = repository.getRoleByUserId(email)
                _currentUserRole.value = uRole?.role ?: "aluno"
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Erro ao inicializar e pré-popular banco de dados: ${t.message}", t)
            }
        }
        observeActiveUserProfile()

        // Continuous background sync daemon with Supabase to keep everything freshly pulled
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            delay(5000) // Delay first run slightly to avoid competing with startup queries
            while (true) {
                try {
                    SupabaseSync.pullAllData(getApplication(), database.dao())
                } catch (t: Throwable) {
                    android.util.Log.e("MainViewModel", "Erro na sincronização constante com o Supabase: ${t.message}", t)
                }
                delay(15000) // Synchronize and pull every 15 seconds
            }
        }
    }

    private fun observeActiveUserProfile() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            currentUserId.collect { email ->
                try {
                    val profile = repository.getProfileById(email)
                    _currentUserProfile.value = profile
                    val uRole = repository.getRoleByUserId(email)
                    _currentUserRole.value = uRole?.role ?: "aluno"
                } catch (t: Throwable) {
                    android.util.Log.e("MainViewModel", "Erro ao observar perfil ativo: ${t.message}", t)
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Simulated Sign In / Sign Up / Onboarding
    // ------------------------------------------------------------------------
    fun loginUser(email: String, name: String = "", curso: String = "", periodo: String = "", bio: String = "") {
        viewModelScope.launch {
            if (email.isBlank() || !email.contains("@")) {
                authError.emit("Por favor, digite um e-mail corporativo ou uerj válido.")
                return@launch
            }
            // Check if profile exists, otherwise onboard
            val profile = repository.getProfileById(email)
            if (profile == null) {
                // First-time signup -> promote to default Aluno, unless super_admin conditions met
                val signupName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
                val newProfile = Profile(
                    id = email,
                    nome = signupName,
                    curso = curso.ifBlank { "Lic. Pedagogia/UERJ" },
                    periodo = periodo.ifBlank { "5º Período" },
                    bio = bio,
                    foto_url = "avatar_user_default"
                )
                repository.insertProfile(newProfile)
                val targetRole = if (email == "jefersonribeiro199026@gmail.com") "super_admin" else "aluno"
                val uRole = UserRole(user_id = email, role = targetRole)
                repository.insertRole(uRole)

                // Sync to Supabase
                SupabaseSync.syncProfile(getApplication(), newProfile)
                SupabaseSync.syncRole(getApplication(), email, targetRole)
            }
            _currentUserId.value = email
        }
    }

    fun switchAccount(email: String) {
        viewModelScope.launch {
            _currentUserId.value = email
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentUserId.value = ""
            _currentUserProfile.value = null
            _currentUserRole.value = "convidado"
        }
    }

    fun linkGoogleAccount(email: String, name: String, avatarUrl: String) {
        viewModelScope.launch {
            if (email.isBlank() || !email.contains("@")) {
                authError.emit("Por favor, selecione ou digite um e-mail do Google válido.")
                return@launch
            }
            val existingProfile = repository.getProfileById(email)
            if (existingProfile == null) {
                // First-time signup with Google -> Profile is created with empty course/period to force onboarding
                val signupName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
                val newProfile = Profile(
                    id = email,
                    nome = signupName,
                    curso = "", // Empty to trigger onboarding workflow
                    periodo = "",
                    bio = "",
                    foto_url = avatarUrl.ifBlank { "avatar_user_default" }
                )
                repository.insertProfile(newProfile)
                val targetRole = if (email == "jefersonribeiro199026@gmail.com") "super_admin" else "aluno"
                repository.insertRole(UserRole(user_id = email, role = targetRole))
                
                // Sync to Supabase
                SupabaseSync.syncProfile(getApplication(), newProfile)
                SupabaseSync.syncRole(getApplication(), email, targetRole)
            } else {
                // Sync profile state
                SupabaseSync.syncProfile(getApplication(), existingProfile)
            }
            _currentUserId.value = email
        }
    }

    fun handleOnboarding(nome: String, curso: String, periodo: String, bio: String, selectedMateria: String = "") {
        viewModelScope.launch {
            val user = currentUserId.value
            val current = repository.getProfileById(user)
            val finalCourse = normalizeCourse(curso.ifBlank { "Lic. Pedagogia/UERJ" })
            val updated = Profile(
                id = user,
                nome = nome.ifBlank { current?.nome ?: "Estudante" },
                curso = finalCourse,
                periodo = periodo.ifBlank { "1º Período" },
                bio = bio,
                foto_url = current?.foto_url ?: "avatar_user_default",
                selected_materia = selectedMateria,
                created_at = current?.created_at ?: System.currentTimeMillis()
            )
            repository.insertProfile(updated)
            _currentUserProfile.value = updated

            // Support adding custom selected subjects immediately so they exist in database
            if (selectedMateria.isNotBlank()) {
                val existing = repository.getAllSubjects().first()
                if (existing.none { it.title.equals(selectedMateria, ignoreCase = true) }) {
                    repository.insertSubject(Subject(title = selectedMateria, description = "Disciplina associada ao curso $finalCourse"))
                }
            }

            // Sync onboarding details to Supabase instantly!
            SupabaseSync.syncProfile(getApplication(), updated)
        }
    }

    fun updateModeratorOptions(userId: String, role: String, permissions: String, principalArea: String, online: Boolean) {
        viewModelScope.launch {
            val updatedRole = UserRole(
                user_id = userId,
                role = role,
                permissions = permissions,
                principal_area = principalArea,
                online = online
            )
            repository.insertRole(updatedRole)
            
            // Sync role to Supabase
            SupabaseSync.syncRole(
                getApplication(),
                userId = userId,
                roleName = role,
                permissions = permissions,
                principalArea = principalArea
            )
            
            logAdminAction("Att Permissões Staff", "Funcionário: $userId | Cargo: $role | Opções: $permissions | Online: $online")
            observeActiveUserProfile()
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Postings & Interactions
    // ------------------------------------------------------------------------
    fun createPost(content: String, subjectId: Int?, mediaUrl: String?) {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val newPost = Post(
                author_id = profile.id,
                author_name = profile.nome,
                author_avatar = profile.foto_url,
                subject_id = subjectId,
                content = content,
                media_url = mediaUrl
            )
            repository.insertPost(newPost)
            SupabaseSync.syncPost(getApplication(), newPost)

            // Dynamic Alert/Notification simulation (Step 3: Notify comments/new activity)
            // Send alerts to active students
            allProfiles.value.forEach { other ->
                if (other.id != profile.id) {
                    val alert = Notification(
                        user_id = other.id,
                        title = "Novo Post de ${profile.nome}",
                        body = if (content.length > 50) "${content.take(48)}..." else content
                    )
                    repository.insertNotification(alert)
                    SupabaseSync.syncNotification(getApplication(), alert)
                }
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            repository.deletePostById(post.id)
            repository.deleteCommentsForPost(post.id)
            SupabaseSync.deleteFromSupabase(getApplication(), "posts", "id", post.id)

            // Log mod actions (Step 5)
            if (currentUserRole.value != "aluno") {
                val profile = currentUserProfile.value
                repository.insertAuditLog(
                    AuditLog(
                        moderator_id = profile?.id ?: "desconhecido",
                        moderator_name = profile?.nome ?: "Moderador",
                        action = "Post Deletado",
                        target = "Autor: ${post.author_name} | Conteúdo: ${post.content.take(30)}..."
                    )
                )
            }
        }
    }

    fun togglePostLike(postId: Int) {
        viewModelScope.launch {
            val userId = currentUserId.value
            val isLiked = repository.isPostLiked(postId, userId)
            repository.toggleLike(postId, userId)

            // Reactively fetch post and update its likes_count
            val currentPosts = allPosts.value
            val post = currentPosts.find { it.id == postId } ?: return@launch
            val newCount = if (isLiked) (post.likes_count - 1).coerceAtLeast(0) else post.likes_count + 1
            repository.insertPost(post.copy(likes_count = newCount))

            // Simulated Push/Notification of Like
            if (!isLiked && post.author_id != userId) {
                val profile = currentUserProfile.value
                repository.insertNotification(
                    Notification(
                        user_id = post.author_id,
                        title = "Alguém curtiu seu post!",
                        body = "${profile?.nome ?: "Um colega"} curtiu seu post pedagógico."
                    )
                )
            }
        }
    }

    fun addComment(postId: Int, content: String) {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val comment = Comment(
                post_id = postId,
                author_id = profile.id,
                author_name = profile.nome,
                author_avatar = profile.foto_url,
                content = content
            )
            repository.insertComment(comment)
            SupabaseSync.syncComment(getApplication(), comment)

            // Notify post author
            val post = allPosts.value.find { it.id == postId }
            if (post != null && post.author_id != profile.id) {
                val notif = Notification(
                    user_id = post.author_id,
                    title = "Novo comentário no seu post",
                    body = "${profile.nome}: ${if (content.length > 50) "${content.take(48)}..." else content}"
                )
                repository.insertNotification(notif)
                SupabaseSync.syncNotification(getApplication(), notif)
            }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteCommentById(comment.id)
            SupabaseSync.deleteFromSupabase(getApplication(), "comments", "id", comment.id)
            if (currentUserRole.value != "aluno") {
                val profile = currentUserProfile.value
                repository.insertAuditLog(
                    AuditLog(
                        moderator_id = profile?.id ?: "desconhecido",
                        moderator_name = profile?.nome ?: "Moderador",
                        action = "Comentário Deletado",
                        target = "Autor: ${comment.author_name} | Comentário: ${comment.content}"
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Mindmaps
    // ------------------------------------------------------------------------
    fun createMindmap(title: String, description: String, tags: String, imageUrl: String) {
        viewModelScope.launch {
            if (currentUserRole.value != "super_admin" && currentUserRole.value != "moderador") {
                authError.emit("Aviso: Somente admins e super_admins podem enviar mapas mentais!")
                return@launch
            }
            if (title.isBlank() || description.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val newMindmap = Mindmap(
                title = title,
                description = description,
                image_url = imageUrl.ifBlank { "https://images.unsplash.com/photo-1517842645767-c639042777db" },
                author_id = profile.id,
                author_name = profile.nome,
                tags = tags
            )
            repository.insertMindmap(newMindmap)
            SupabaseSync.syncMindmap(getApplication(), newMindmap)
        }
    }

    fun toggleMindmapFav(mindmapId: Int) {
        viewModelScope.launch {
            val userId = currentUserId.value
            val isFavorite = userFavorites.value.any { it.mindmap_id == mindmapId }
            if (isFavorite) {
                repository.removeMindmapFavorite(mindmapId, userId)
            } else {
                repository.toggleMindmapFavorite(mindmapId, userId)
            }
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Help Requests
    // ------------------------------------------------------------------------
    fun createHelpRequest(title: String, description: String, subjectId: Int) {
        viewModelScope.launch {
            if (title.isBlank() || description.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val request = HelpRequest(
                title = title,
                description = description,
                subject_id = subjectId,
                author_id = profile.id,
                author_name = profile.nome
            )
            repository.insertHelpRequest(request)
            SupabaseSync.syncHelpRequest(getApplication(), request)
        }
    }

    fun markHelpResolved(id: Int, resolved: Boolean) {
        viewModelScope.launch {
            repository.updateHelpRequestStatus(id, resolved)
            // Retrieve help request and update status in Supabase if found
            repository.getAllHelpRequests().first().find { it.id == id }?.let { item ->
                SupabaseSync.syncHelpRequest(getApplication(), item.copy(is_resolved = resolved))
            }
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Chat with Flood Control
    // ------------------------------------------------------------------------
    fun sendChatMessage(content: String, mediaUrl: String? = null) {
        viewModelScope.launch {
            if (content.isBlank() && mediaUrl == null) return@launch
            val now = System.currentTimeMillis()
            // Flood control: check if less than 3 seconds elapsed (3000ms)
            if (now - lastMessageTime < 3000) {
                chatError.emit("Aviso: Flood control! Aguarde 3s para enviar nova mensagem.")
                return@launch
            }
            val profile = currentUserProfile.value ?: return@launch
            val role = currentUserRole.value

            val myCourse = normalizeCourse(profile.curso)
            val safeCourse = myCourse.replace(" ", "_").replace("/", "_")
            val safePeriod = profile.periodo.replace(" ", "_")
            val safeMateria = profile.selected_materia.replace(" ", "_")
            val myRoomTag = "room_${safeCourse}_${safePeriod}_${safeMateria}"

            val msg = Message(
                content = content,
                media_url = mediaUrl,
                author_id = profile.id,
                author_name = profile.nome,
                author_role = role,
                room_tag = myRoomTag
            )
            repository.insertMessage(msg)
            SupabaseSync.syncMessage(getApplication(), msg)
            lastMessageTime = now
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Study Groups & Threads
    // ------------------------------------------------------------------------
    fun createStudyGroup(name: String, description: String) {
        viewModelScope.launch {
            if (name.isBlank() || description.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val group = StudyGroup(
                name = name,
                description = description,
                created_by = profile.id
            )
            repository.insertStudyGroup(group)
            SupabaseSync.syncStudyGroup(getApplication(), group)
        }
    }

    fun createGroupThread(groupId: Int, title: String, content: String) {
        viewModelScope.launch {
            if (title.isBlank() || content.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val thread = GroupThread(
                group_id = groupId,
                title = title,
                content = content,
                author_id = profile.id,
                author_name = profile.nome
            )
            repository.insertGroupThread(thread)
            SupabaseSync.syncGroupThread(getApplication(), thread)
        }
    }

    fun deleteGroupThread(groupId: Int, threadId: Int) {
        viewModelScope.launch {
            repository.deleteGroupThread(threadId)
            SupabaseSync.deleteFromSupabase(getApplication(), "group_threads", "id", threadId)
        }
    }

    fun addGroupComment(threadId: Int, content: String) {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            val profile = currentUserProfile.value ?: return@launch
            val c = GroupComment(
                thread_id = threadId,
                content = content,
                author_id = profile.id,
                author_name = profile.nome
            )
            repository.insertGroupComment(c)
            SupabaseSync.syncGroupComment(getApplication(), c)
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Calendar Events (Admin Only)
    // ------------------------------------------------------------------------
    fun createCalendarEvent(title: String, description: String, date: Long, category: String) {
        viewModelScope.launch {
            if (title.isBlank() || description.isBlank() || category.isBlank()) return@launch
            val createdBy = currentUserId.value
            val event = CalendarEvent(
                title = title,
                description = description,
                date = date,
                category = category,
                created_by = createdBy
            )
            repository.insertCalendarEvent(event)
            SupabaseSync.syncCalendarEvent(getApplication(), event)

            // Log action
            val profile = currentUserProfile.value
            val log = AuditLog(
                moderator_id = profile?.id ?: "super_admin",
                moderator_name = profile?.nome ?: "Super Admin",
                action = "Evento Criado",
                target = "Título: $title | Categoria: $category"
            )
            repository.insertAuditLog(log)
            SupabaseSync.syncAuditLog(getApplication(), log)

            // Notify all students (including creator for immediate feedback)
            allProfiles.value.forEach { other ->
                val alert = Notification(
                    user_id = other.id,
                    title = "Novo aviso na Agenda: $title",
                    body = "Uma nova atividade de tipo [$category] foi adicionada."
                )
                repository.insertNotification(alert)
                SupabaseSync.syncNotification(getApplication(), alert)
            }
        }
    }

    fun deleteCalendarEvent(id: Int) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
            SupabaseSync.deleteFromSupabase(getApplication(), "calendar_events", "id", id)
            // Log action
            val profile = currentUserProfile.value
            repository.insertAuditLog(
                AuditLog(
                    moderator_id = profile?.id ?: "super_admin",
                    moderator_name = profile?.nome ?: "Super Admin",
                    action = "Evento Deletado",
                    target = "ID: $id"
                )
            )
        }
    }

    // ------------------------------------------------------------------------
    // CRUD: Role Promotion / Depromotion (Super Admin can promote up to 3 active moderadores)
    // ------------------------------------------------------------------------
    fun toggleModeratorRole(userId: String) {
        viewModelScope.launch {
            val currentRoles = allRoles.value
            val targetRole = currentRoles.find { it.user_id == userId }
            if (targetRole == null) return@launch

            if (targetRole.role == "moderador") {
                // Demote to aluno
                repository.insertRole(UserRole(user_id = userId, role = "aluno"))
                logAdminAction("Demoveu Moderador", "Usuário: $userId foi despromovido a Aluno.")
            } else {
                // Check if we already have 3 or more active moderators (cap = 3)
                val activeModsCount = currentRoles.count { it.role == "moderador" }
                if (activeModsCount >= 3) {
                    authError.emit("Aviso: Limite atingido! Você pode promover no máximo 3 moderadores ativos.")
                    return@launch
                }
                repository.insertRole(UserRole(user_id = userId, role = "moderador"))
                logAdminAction("Promoveu Moderador", "Usuário: $userId promovido a Moderador.")
            }
            observeActiveUserProfile()
        }
    }

    private suspend fun logAdminAction(action: String, target: String) {
        val profile = currentUserProfile.value
        repository.insertAuditLog(
            AuditLog(
                moderator_id = profile?.id ?: "super_admin",
                moderator_name = profile?.nome ?: "Super Admin",
                action = action,
                target = target
            )
        )
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(currentUserId.value)
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun addStudyMaterial(title: String, description: String, fileUrl: String, type: String) {
        viewModelScope.launch {
            if (currentUserRole.value != "super_admin" && currentUserRole.value != "moderador") {
                authError.emit("Aviso: Somente admins e super_admins podem enviar provas, materiais e audiobooks!")
                return@launch
            }
            val profile = currentUserProfile.value
            val mat = StudyMaterial(
                title = title,
                description = description,
                file_url = fileUrl,
                type = type,
                author_id = profile?.id ?: "unknown",
                author_name = profile?.nome ?: "Staff"
            )
            repository.insertStudyMaterial(mat)
            SupabaseSync.syncStudyMaterial(getApplication(), mat)
            logAdminAction("Material Adicionado", "$title ($type)")
        }
    }

    fun deleteStudyMaterial(id: Int) {
        viewModelScope.launch {
            repository.deleteStudyMaterialById(id)
            SupabaseSync.deleteFromSupabase(getApplication(), "study_materials", "id", id)
            logAdminAction("Material Deletado", "ID: $id")
        }
    }

    fun pullDataFromSupabase() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                SupabaseSync.pullAllData(getApplication(), database.dao())
            } catch (t: Throwable) {
                android.util.Log.e("MainViewModel", "Erro ao puxar dados manualmente: ${t.message}")
            }
        }
    }

    // Direct CRUD operations for Admin/SuperAdmin system-wide database manager
    fun saveProfileDirectly(profile: Profile) {
        viewModelScope.launch {
            repository.insertProfile(profile)
            SupabaseSync.syncProfile(getApplication(), profile)
            logAdminAction("Admin CRUD: Perfil Salvo", "ID: ${profile.id} | Nome: ${profile.nome}")
        }
    }

    fun deleteProfileDirectly(id: String) {
        viewModelScope.launch {
            repository.deleteProfileDirectly(id)
            repository.deleteRole(id) // also clean up role
            SupabaseSync.deleteFromSupabase(getApplication(), "profiles", "id", id)
            SupabaseSync.deleteFromSupabase(getApplication(), "roles", "user_id", id)
            logAdminAction("Admin CRUD: Perfil Deletado", "ID: $id")
        }
    }

    fun saveSubjectDirectly(subject: Subject) {
        viewModelScope.launch {
            repository.insertSubject(subject)
            logAdminAction("Admin CRUD: Disciplina Salva", "ID: ${subject.id} | Título: ${subject.title}")
        }
    }

    fun deleteSubjectDirectly(id: Int) {
        viewModelScope.launch {
            repository.deleteSubjectDirectly(id)
            logAdminAction("Admin CRUD: Disciplina Deletada", "ID: $id")
        }
    }

    fun saveMindmapDirectly(mindmap: Mindmap) {
        viewModelScope.launch {
            repository.insertMindmap(mindmap)
            SupabaseSync.syncMindmap(getApplication(), mindmap)
            logAdminAction("Admin CRUD: Mapa Mental Salvo", "ID: ${mindmap.id} | Título: ${mindmap.title}")
        }
    }

    fun deleteMindmapDirectly(id: Int) {
        viewModelScope.launch {
            repository.deleteMindmapDirectly(id)
            SupabaseSync.deleteFromSupabase(getApplication(), "mindmaps", "id", id)
            logAdminAction("Admin CRUD: Mapa Mental Deletado", "ID: $id")
        }
    }

    fun saveHelpRequestDirectly(req: HelpRequest) {
        viewModelScope.launch {
            repository.insertHelpRequest(req)
            SupabaseSync.syncHelpRequest(getApplication(), req)
            logAdminAction("Admin CRUD: Pedido de Ajuda Salvo", "ID: ${req.id} | Título: ${req.title}")
        }
    }

    fun deleteHelpRequestDirectly(id: Int) {
        viewModelScope.launch {
            repository.deleteHelpRequestDirectly(id)
            SupabaseSync.deleteFromSupabase(getApplication(), "help_requests", "id", id)
            logAdminAction("Admin CRUD: Pedido de Ajuda Deletado", "ID: $id")
        }
    }

    fun saveStudyGroupDirectly(group: StudyGroup) {
        viewModelScope.launch {
            repository.insertStudyGroup(group)
            SupabaseSync.syncStudyGroup(getApplication(), group)
            logAdminAction("Admin CRUD: Grupo Salvo", "ID: ${group.id} | Nome: ${group.name}")
        }
    }

    fun deleteStudyGroupDirectly(id: Int) {
        viewModelScope.launch {
            repository.deleteStudyGroupDirectly(id)
            SupabaseSync.deleteFromSupabase(getApplication(), "study_groups", "id", id)
            logAdminAction("Admin CRUD: Grupo Deletado", "ID: $id")
        }
    }

    fun saveCalendarEventDirectly(event: CalendarEvent) {
        viewModelScope.launch {
            repository.insertCalendarEvent(event)
            SupabaseSync.syncCalendarEvent(getApplication(), event)
            logAdminAction("Admin CRUD: Evento Salvo", "ID: ${event.id} | Título: ${event.title}")
        }
    }

    fun saveStudyMaterialDirectly(material: StudyMaterial) {
        viewModelScope.launch {
            repository.insertStudyMaterial(material)
            SupabaseSync.syncStudyMaterial(getApplication(), material)
            logAdminAction("Admin CRUD: Material Salvo", "ID: ${material.id} | Título: ${material.title}")
        }
    }

    fun savePostDirectly(post: Post) {
        viewModelScope.launch {
            repository.insertPost(post)
            SupabaseSync.syncPost(getApplication(), post)
            logAdminAction("Admin CRUD: Postagem Salva", "ID: ${post.id} | Conteúdo: ${post.content.take(20)}")
        }
    }
}
