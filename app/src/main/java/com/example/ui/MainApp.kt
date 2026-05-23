package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun getCourseIcon(course: String): ImageVector {
    return when {
        course.contains("Pedagogia", ignoreCase = true) -> Icons.Filled.School
        course.contains("Computação", ignoreCase = true) -> Icons.Filled.Computer
        course.contains("Segurança", ignoreCase = true) -> Icons.Filled.Shield
        course.contains("Matemática", ignoreCase = true) -> Icons.Filled.Star
        course.contains("Biológicas", ignoreCase = true) -> Icons.Filled.Science
        course.contains("Contábeis", ignoreCase = true) -> Icons.Filled.Percent
        course.contains("Produção", ignoreCase = true) -> Icons.Filled.Build
        course.contains("Biblioteconomia", ignoreCase = true) -> Icons.Filled.LibraryBooks
        course.contains("Administração", ignoreCase = true) -> Icons.Filled.AccountBalance
        else -> Icons.Filled.Book
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    darkThemeState: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Session flows
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    // Core Data flows
    val profiles by viewModel.filteredProfiles.collectAsStateWithLifecycle()
    val allRoles by viewModel.allRoles.collectAsStateWithLifecycle(emptyList())
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val subjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val mindmaps by viewModel.filteredMindmaps.collectAsStateWithLifecycle()
    val helpRequests by viewModel.filteredHelpRequests.collectAsStateWithLifecycle()
    val messages by viewModel.filteredMessages.collectAsStateWithLifecycle()
    val studyGroups by viewModel.filteredStudyGroups.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.filteredCalendarEvents.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()

    // Interactive user flows
    val userFavorites by viewModel.userFavorites.collectAsStateWithLifecycle()
    val userLikes by viewModel.userLikes.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()

    // Local Search & Filter UI flows
    val searchGlobalQuery by viewModel.searchGlobalQuery.collectAsStateWithLifecycle()
    val filteredSubjectId by viewModel.filteredSubjectId.collectAsStateWithLifecycle()
    val studyMaterials by viewModel.allStudyMaterials.collectAsStateWithLifecycle()

    // Navigation state
    var currentTab by remember { mutableStateOf("home") } // home, mindmaps, help_groups, chat, agenda
    var showSplashScreen by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2800)
        showSplashScreen = false
    }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    var uploadMindmapUrl by remember { mutableStateOf("") }
    var uploadMindmapFileName by remember { mutableStateOf<String?>(null) }
    var isUploadingMindmap by remember { mutableStateOf(false) }

    val mindmapImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "upload_mapa_mental.png"
            isUploadingMindmap = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(1200) // simulated network upload delay
                isUploadingMindmap = false
                uploadMindmapUrl = "${SupabaseSync.getUrl(context).trimEnd('/')}/storage/v1/object/public/mindmaps/$fileName"
                uploadMindmapFileName = fileName
                Toast.makeText(context, "Mapa Mental carregado: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var activeGroupDetail by remember { mutableStateOf<StudyGroup?>(null) }
    var activeThreadDetail by remember { mutableStateOf<GroupThread?>(null) }
    var activeJitsiRoomName by remember { mutableStateOf<String?>(null) }
    var activeJitsiRoomTitle by remember { mutableStateOf<String?>(null) }

    // Dialog creators
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateMindmapDialog by remember { mutableStateOf(false) }
    var showCreateHelpRequestDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateCalendarEventDialog by remember { mutableStateOf(false) }

    // Collect Toast Messages (Flood Control / Auth warnings)
    LaunchedEffect(Unit) {
        viewModel.chatError.collectLatest { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.authError.collectLatest { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    // Force onboarding on first signup / missing course details
    LaunchedEffect(currentUserId, currentUserProfile) {
        if (currentUserId.isNotEmpty() && (currentUserProfile == null || currentUserProfile?.curso.isNullOrEmpty())) {
            showOnboardingDialog = true
        }
    }

    Crossfade(targetState = showSplashScreen, label = "screen_transition") { isSplash ->
        if (isSplash) {
            AppSplashScreen()
        } else if (activeJitsiRoomName != null) {
            JitsiInframeScreen(
                roomName = activeJitsiRoomName!!,
                roomTitle = activeJitsiRoomTitle ?: "Tutoria Online",
                userName = currentUserProfile?.nome ?: "Discente da UERJ",
                userEmail = currentUserProfile?.id ?: "estudante@uerj.br",
                userAvatar = currentUserProfile?.foto_url ?: "",
                onClose = {
                    activeJitsiRoomName = null
                    activeJitsiRoomTitle = null
                }
            )
        } else if (currentUserId.isEmpty()) {
            // Sign In View
            LoginScreen(
                onGoogleLogin = { email, name, avatar ->
                    viewModel.linkGoogleAccount(email, name, avatar)
                    Toast.makeText(context, "Sua conta do Google foi vinculada!", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 840.dp
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(UerjBlue, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.School,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Poder da Conexão",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1A237E),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Rede Pedagógica UERJ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = UerjGreen,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Edit / View Profile Button
                                IconButton(onClick = { showEditProfileDialog = true }) {
                            val photoUrl = currentUserProfile?.foto_url
                            if (!photoUrl.isNullOrBlank() && photoUrl.startsWith("http")) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .border(1.dp, UerjBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Meu Perfil",
                                        modifier = Modifier.clip(CircleShape).fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Meu Perfil",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Notifications Badge trigger
                        val unreadNotifications = userNotifications.filter { !it.read }
                        val hasUnread = unreadNotifications.isNotEmpty()
                        val infiniteTransition = rememberInfiniteTransition(label = "bell_and_badge_glow")
                        val blinkingColor by infiniteTransition.animateColor(
                            initialValue = Color(0xFFFF3333), // Bright Red
                            targetValue = Color(0xFF00E676), // Bright Green
                            animationSpec = infiniteRepeatable(
                                animation = tween(700, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glowing_color"
                        )

                        IconButton(onClick = { showNotificationDialog = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notificações",
                                    tint = if (hasUnread) blinkingColor else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (hasUnread) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(blinkingColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }

                        // Log out
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "Sair",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == "home",
                            onClick = {
                                currentTab = "home"
                                activeGroupDetail = null
                                activeThreadDetail = null
                            },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text("Início", fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == "mindmaps",
                            onClick = {
                                currentTab = "mindmaps"
                            },
                            icon = { Icon(Icons.Filled.AccountTree, contentDescription = "Mapas") },
                            label = { Text("Mapas", fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == "help_groups",
                            onClick = {
                                currentTab = "help_groups"
                            },
                            icon = { Icon(Icons.Filled.QuestionAnswer, contentDescription = "Tutoria") },
                            label = { Text("Tutoria", fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == "chat",
                            onClick = {
                                currentTab = "chat"
                            },
                            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat") },
                            label = { Text("Chat", fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == "agenda",
                            onClick = {
                                currentTab = "agenda"
                            },
                            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Agenda") },
                            label = { Text("Agenda", fontWeight = FontWeight.Bold) }
                        )
                        if (currentUserRole == "super_admin" || currentUserRole == "moderador") {
                            NavigationBarItem(
                                selected = currentTab == "staff_admin",
                                onClick = {
                                    currentTab = "staff_admin"
                                },
                                icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Staff / Supabase") },
                                label = { Text("Staff", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Relevant Floating triggers per Tab
                when (currentTab) {
                    "home" -> {
                        FloatingActionButton(
                            onClick = { showCreatePostDialog = true },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(Icons.Filled.AddComment, contentDescription = "Novo Post")
                        }
                    }
                    "mindmaps" -> {
                        if (currentUserRole == "super_admin" || currentUserRole == "moderador") {
                            FloatingActionButton(
                                onClick = { showCreateMindmapDialog = true },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = "Subir Mapa")
                            }
                        }
                    }
                    "help_groups" -> {
                        FloatingActionButton(
                            onClick = {
                                // Double menu to ask if wants to create Question or Study Group
                                showCreateHelpRequestDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(Icons.Filled.Help, contentDescription = "Pedir Ajuda")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(80.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = currentTab == "home",
                            onClick = {
                                currentTab = "home"
                                activeGroupDetail = null
                                activeThreadDetail = null
                            },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text("Início", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = currentTab == "mindmaps",
                            onClick = { currentTab = "mindmaps" },
                            icon = { Icon(Icons.Filled.AccountTree, contentDescription = "Mapas") },
                            label = { Text("Mapas", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = currentTab == "help_groups",
                            onClick = { currentTab = "help_groups" },
                            icon = { Icon(Icons.Filled.QuestionAnswer, contentDescription = "Tutoria") },
                            label = { Text("Tutoria", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = currentTab == "chat",
                            onClick = { currentTab = "chat" },
                            icon = { Icon(Icons.Filled.Chat, contentDescription = "Chat") },
                            label = { Text("Chat", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = currentTab == "agenda",
                            onClick = { currentTab = "agenda" },
                            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Agenda") },
                            label = { Text("Agenda", style = MaterialTheme.typography.labelSmall) }
                        )
                        if (currentUserRole == "super_admin" || currentUserRole == "moderador") {
                            NavigationRailItem(
                                selected = currentTab == "staff_admin",
                                onClick = { currentTab = "staff_admin" },
                                icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Staff") },
                                label = { Text("Staff", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                when (currentTab) {
                    "home" -> {
                        DashboardScreen(
                            posts = posts,
                            subjects = subjects,
                            calendarEvents = calendarEvents,
                            userLikes = userLikes,
                            currentUserRole = currentUserRole,
                            currentUserId = currentUserId,
                            searchQuery = searchGlobalQuery,
                            filteredSubjectId = filteredSubjectId,
                            onQueryChange = { viewModel.searchGlobalQuery.value = it },
                            onSelectSubject = { viewModel.filteredSubjectId.value = it },
                            onToggleLike = { viewModel.togglePostLike(it) },
                            onAddComment = { postId, body -> viewModel.addComment(postId, body) },
                            onDeletePost = { viewModel.deletePost(it) },
                            onDeleteComment = { viewModel.deleteComment(it) },
                            commentsProvider = { postId -> viewModel.getCommentsForPost(postId) },
                            onTabChange = { currentTab = it },
                            allRoles = allRoles
                        )
                    }
                    "staff_admin" -> {
                        StaffAdminPanelScreen(
                            profiles = profiles,
                            allRoles = allRoles,
                            onUpdateRole = { userId, role, permissions, area, online ->
                                viewModel.updateModeratorOptions(userId, role, permissions, area, online)
                            },
                            context = context,
                            currentUserRole = currentUserRole,
                            viewModel = viewModel
                        )
                    }
                    "mindmaps" -> {
                        MindmapScreen(
                            mindmaps = mindmaps,
                            favorites = userFavorites,
                            searchQuery = searchGlobalQuery,
                            currentUser = currentUserProfile,
                            onQueryChange = { viewModel.searchGlobalQuery.value = it },
                            onToggleFav = { viewModel.toggleMindmapFav(it) },
                            currentUserRole = currentUserRole,
                            studyMaterials = studyMaterials,
                            onAddMaterial = { title, desc, url, type ->
                                viewModel.addStudyMaterial(title, desc, url, type)
                            },
                            onDeleteMaterial = { materialId ->
                                viewModel.deleteStudyMaterial(materialId)
                            }
                        )
                    }
                    "help_groups" -> {
                        HelpAndGroupsHub(
                            helpRequests = helpRequests,
                            studyGroups = studyGroups,
                            subjects = subjects,
                            currentUserId = currentUserId,
                            currentUserRole = currentUserRole,
                            activeGroupDetail = activeGroupDetail,
                            activeThreadDetail = activeThreadDetail,
                            onGroupClick = { activeGroupDetail = it },
                            onThreadClick = { activeThreadDetail = it },
                            onBackToHub = {
                                activeGroupDetail = null
                                activeThreadDetail = null
                            },
                            onBackToGroup = { activeThreadDetail = null },
                            onCreateGroup = { showCreateGroupDialog = true },
                            onCreateThread = { title, content ->
                                activeGroupDetail?.let {
                                    viewModel.createGroupThread(it.id, title, content)
                                    Toast.makeText(context, "Tópico criado!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAddThreadComment = { threadId, comment ->
                                viewModel.addGroupComment(threadId, comment)
                            },
                            onResolveHelp = { helpId, solved ->
                                viewModel.markHelpResolved(helpId, solved)
                                Toast.makeText(context, if (solved) "Marcada como Resolvida!" else "Reaberta!", Toast.LENGTH_SHORT).show()
                            },
                            threadsProvider = { groupId -> viewModel.getThreadsForGroup(groupId) },
                            threadCommentsProvider = { threadId -> viewModel.getCommentsForThread(threadId) },
                            onJoinVideoTutoria = { room, title ->
                                activeJitsiRoomName = room
                                activeJitsiRoomTitle = title
                            },
                            allRoles = allRoles,
                            profiles = profiles,
                            currentUserProfile = currentUserProfile
                        )
                    }
                    "chat" -> {
                        val currentProfile = currentUserProfile
                        val roommates = profiles.filter { 
                            currentProfile != null && 
                            it.curso == currentProfile.curso && 
                            it.periodo == currentProfile.periodo 
                        }
                        ChatRoomScreen(
                            messages = messages,
                            currentUserId = currentUserId,
                            currentUserProfile = currentProfile,
                            classmates = roommates,
                            onSendMessage = { body, media -> viewModel.sendChatMessage(body, media) },
                            onTriggerEditProfile = { showEditProfileDialog = true }
                        )
                    }
                    "agenda" -> {
                        val displayedRoles = remember(profiles, allRoles) { rolesListForDisplay(profiles, allRoles) }
                        AgendaScreen(
                            events = calendarEvents,
                            currentUserRole = currentUserRole,
                            auditLogs = auditLogs,
                            profiles = profiles,
                            roles = displayedRoles,
                            onCreateEventTrigger = { showCreateCalendarEventDialog = true },
                            onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                            onToggleModeratorRole = { viewModel.toggleModeratorRole(it) }
                        )
                    }
                }
            }
        }
    }
}
}

                 // 1. Onboarding Dialog
    if (showOnboardingDialog) {
        var nomeInput by remember { mutableStateOf("") }
        var selectedCurso by remember { mutableStateOf("Lic. Pedagogia/UERJ") }
        var selectedPeriodo by remember { mutableStateOf("1º Período") }
        var materiaInput by remember { mutableStateOf(getDefaultSubjectForCourse("Lic. Pedagogia/UERJ", "1º Período")) }
        var showCourseDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                viewModel.handleOnboarding(
                    nome = if (nomeInput.isBlank()) "Estudante" else nomeInput,
                    curso = selectedCurso,
                    periodo = selectedPeriodo,
                    bio = "Polo: $selectedCurso • Período: $selectedPeriodo",
                    selectedMateria = materiaInput
                )
                showOnboardingDialog = false
            },
            confirmButton = {}, // Handled inside text Column to support scrollability
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configuração de Onboarding", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Selecione seu Curso do Polo, Período e Disciplina Ativa para se conectar com seus colegas de sala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. SELECT CURSO
                    Text(
                        "Curso do Polo:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCourseDropdown = !showCourseDropdown },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    getCourseIcon(selectedCurso),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    selectedCurso,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                if (showCourseDropdown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showCourseDropdown) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                MainViewModel.PoloCoursesList.forEach { course ->
                                    val isSelected = selectedCurso == course
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCurso = course
                                                showCourseDropdown = false
                                                materiaInput = getDefaultSubjectForCourse(course, selectedPeriodo)
                                            }
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            getCourseIcon(course),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = course,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. SELECT PERÍODO
                    Text(
                        "Período Atual:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val periods = listOf("1º Período", "2º Período", "3º Período", "4º Período", "5º Período", "6º Período", "7º Período", "8º Período")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periods.forEach { p ->
                            val isSelected = selectedPeriodo == p
                            Card(
                                onClick = {
                                    selectedPeriodo = p
                                    materiaInput = getDefaultSubjectForCourse(selectedCurso, p)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = p,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. SELECT OR TYPE DISCIPLINA
                    Text(
                        "Disciplina Ativa Principal:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Você ingressará imediatamente no canal de chat desta disciplina para cooperar com seus colegas de polo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = materiaInput,
                        onValueChange = { materiaInput = it },
                        label = { Text("Nome da Disciplina") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // CONFIRM BUTTONS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                viewModel.handleOnboarding(
                                    nome = if (nomeInput.isBlank()) "Estudante" else nomeInput,
                                    curso = selectedCurso,
                                    periodo = selectedPeriodo,
                                    bio = "Polo: $selectedCurso • Período: $selectedPeriodo",
                                    selectedMateria = materiaInput.ifBlank { getDefaultSubjectForCourse(selectedCurso, selectedPeriodo) }
                                )
                                showOnboardingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar e Salvar Cadastro", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.handleOnboarding(
                                    nome = "Estudante",
                                    curso = "Lic. Pedagogia/UERJ",
                                    periodo = "1º Período",
                                    bio = "Pulou onboarding",
                                    selectedMateria = getDefaultSubjectForCourse("Lic. Pedagogia/UERJ", "1º Período")
                                )
                                showOnboardingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pular Onboarding")
                        }
                    }
                }
            }
        )
    }

    // 1b. Profile Controller Dialog (Meu Perfil / Atualizar Curso e Período)
    if (showEditProfileDialog) {
        val currentProfile = currentUserProfile
        var nomeInput by remember(currentProfile) { mutableStateOf(currentProfile?.nome ?: "") }
        var selectedCurso by remember(currentProfile) { mutableStateOf(currentProfile?.curso?.let { MainViewModel.normalizeCourse(it) } ?: "Lic. Pedagogia/UERJ") }
        var selectedPeriodo by remember(currentProfile) { mutableStateOf(currentProfile?.periodo ?: "1º Período") }
        var materiaInput by remember(currentProfile) { mutableStateOf(currentProfile?.selected_materia ?: getDefaultSubjectForCourse(selectedCurso, selectedPeriodo)) }
        var showCourseDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleOnboarding(
                            nome = nomeInput,
                            curso = selectedCurso,
                            periodo = selectedPeriodo,
                            bio = "Polo: $selectedCurso • Período: $selectedPeriodo",
                            selectedMateria = materiaInput
                        )
                        showEditProfileDialog = false
                        Toast.makeText(context, "Sua sala foi atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = nomeInput.isNotBlank() && materiaInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Meu Perfil / Atualizar Disciplina")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Ao atualizar seu curso ou disciplina ativa, você será direcionado instantaneamente para seu novo grupo de bate-papo em tempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nomeInput,
                        onValueChange = { nomeInput = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // CURSO DROPDOWN
                    Text(
                        "Seu Curso do Polo:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCourseDropdown = !showCourseDropdown },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    getCourseIcon(selectedCurso),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    selectedCurso,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                if (showCourseDropdown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showCourseDropdown) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                MainViewModel.PoloCoursesList.forEach { course ->
                                    val isSelected = selectedCurso == course
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCurso = course
                                                showCourseDropdown = false
                                                materiaInput = getDefaultSubjectForCourse(course, selectedPeriodo)
                                            }
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            getCourseIcon(course),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = course,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PERÍODO CHIPS
                    Text(
                        "Período Atual:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val periods = listOf("1º Período", "2º Período", "3º Período", "4º Período", "5º Período", "6º Período", "7º Período", "8º Período")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periods.forEach { p ->
                            val isSelected = selectedPeriodo == p
                            Card(
                                onClick = {
                                    selectedPeriodo = p
                                    materiaInput = getDefaultSubjectForCourse(selectedCurso, p)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = p,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DISCIPLINA TEXT INPUT
                    Text(
                        "Disciplina Ativa Principal:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = materiaInput,
                        onValueChange = { materiaInput = it },
                        label = { Text("Nome da Disciplina") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        )
    }

    // 2. Notification Panel Dialog
    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Central de Notificações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showNotificationDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    val activeNotifications = userNotifications.filter { !it.read }
                    if (activeNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nenhuma notificação por enquanto.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(activeNotifications) { notif ->
                                ListItem(
                                    modifier = Modifier.clickable {
                                        viewModel.markNotificationAsRead(notif.id)
                                    },
                                    headlineContent = { Text(notif.title, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text(notif.body) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            tint = UerjBlue
                                        )
                                    }
                                )
                                Divider()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.clearNotifications()
                                showNotificationDialog = false
                                Toast.makeText(context, "Lidas!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Marcar Todas como Lidas")
                        }
                    }
                }
            }
        }
    }

    // 3. Create Posting dialog
    if (showCreatePostDialog) {
        var textContent by remember { mutableStateOf("") }
        var selectedSubject by remember { mutableStateOf<Subject?>(null) }
        var mediaInputUrl by remember { mutableStateOf("") }
        var showSubjectDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPost(textContent, selectedSubject?.id, mediaInputUrl.ifBlank { null })
                        showCreatePostDialog = false
                    }
                ) {
                    Text("Publicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nova Ideia ou Notícia") },
            text = {
                Column {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("O que você está pensando agora?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Subject dropdown
                    Box {
                        OutlinedButton(
                            onClick = { showSubjectDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedSubject?.title ?: "Selecionar Matéria Vinculada (Opcional)")
                        }
                        DropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sem matéria") },
                                onClick = {
                                    selectedSubject = null
                                    showSubjectDropdown = false
                                }
                            )
                            subjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.title) },
                                    onClick = {
                                        selectedSubject = sub
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mediaInputUrl,
                        onValueChange = { mediaInputUrl = it },
                        label = { Text("URL de Mídia Opcional (Foto, Link)") },
                        placeholder = { Text("https://link.com/foto.jpg") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // 4. Create Mindmap Dialog
    if (showCreateMindmapDialog) {
        var mTitle by remember { mutableStateOf("") }
        var mDesc by remember { mutableStateOf("") }
        var mTags by remember { mutableStateOf("") }
        var mImgUrl by remember { mutableStateOf(uploadMindmapUrl) }

        LaunchedEffect(uploadMindmapUrl) {
            if (uploadMindmapUrl.isNotBlank()) {
                mImgUrl = uploadMindmapUrl
                if (mTitle.isBlank() && uploadMindmapFileName != null) {
                    mTitle = uploadMindmapFileName!!.substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateMindmapDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createMindmap(mTitle, mDesc, mTags, mImgUrl)
                        showCreateMindmapDialog = false
                        Toast.makeText(context, "Sincronizado com S3/Storage!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Enviar para a Galeria")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMindmapDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Subir Mapa Mental") },
            text = {
                Column {
                    Text("Os mapas estão sincronizados com o bucket 'mindmaps' do Supabase. Suporta PNG/JPG até 10MB.", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mTitle,
                        onValueChange = { mTitle = it },
                        label = { Text("Título do Mapa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mDesc,
                        onValueChange = { mDesc = it },
                        label = { Text("Descrição detalhada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mTags,
                        onValueChange = { mTags = it },
                        label = { Text("Tags (separadas por vírgula)") },
                        placeholder = { Text("Vygotsky, Piaget, Resumo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mImgUrl,
                        onValueChange = { mImgUrl = it },
                        label = { Text("URL da Imagem de Prévia") },
                        placeholder = { Text("Deixe em branco para usar uma padrão") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isUploadingMindmap) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Fazendo upload para o Storage...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (uploadMindmapFileName != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle, 
                                    contentDescription = "Sucesso", 
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Arquivo selecionado com sucesso!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(uploadMindmapFileName!!, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { mindmapImagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Selecionar e Subir Imagem")
                    }
                }
            }
        )
    }

    // 5. Create Help Question Dialog
    if (showCreateHelpRequestDialog) {
        var qTitle by remember { mutableStateOf("") }
        var qDesc by remember { mutableStateOf("") }
        var qSubject by remember { mutableStateOf<Subject?>(null) }
        var showHelpSubExpand by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateHelpRequestDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val subId = qSubject?.id ?: 1
                        viewModel.createHelpRequest(qTitle, qDesc, subId)
                        showCreateHelpRequestDialog = false
                    }
                ) {
                    Text("Postar Pergunta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHelpRequestDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Pedir Ajuda Acadêmica") },
            text = {
                Column {
                    OutlinedTextField(
                        value = qTitle,
                        onValueChange = { qTitle = it },
                        label = { Text("Sua Dúvida ou Pergunta") },
                        placeholder = { Text("Ex: Qual a diferença de Piaget e Vygotsky?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qDesc,
                        onValueChange = { qDesc = it },
                        label = { Text("Descreva sua dúvida com mais detalhes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { showHelpSubExpand = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(qSubject?.title ?: "Selecione a Matéria Relacionada")
                        }
                        DropdownMenu(
                            expanded = showHelpSubExpand,
                            onDismissRequest = { showHelpSubExpand = false }
                        ) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.title) },
                                    onClick = {
                                        qSubject = s
                                        showHelpSubExpand = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    // 6. Create Study Group
    if (showCreateGroupDialog) {
        var gName by remember { mutableStateOf("") }
        var gDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createStudyGroup(gName, gDesc)
                        showCreateGroupDialog = false
                        Toast.makeText(context, "Grupo de Estudos criado!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Criar Grupo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Novo Grupo de Estudos") },
            text = {
                Column {
                    OutlinedTextField(
                        value = gName,
                        onValueChange = { gName = it },
                        label = { Text("Nome do Grupo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gDesc,
                        onValueChange = { gDesc = it },
                        label = { Text("Descrição e Objetivos") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        )
    }

    // 7. Create Calendar Event Dialog (Admin/Admins only)
    if (showCreateCalendarEventDialog) {
        var evTitle by remember { mutableStateOf("") }
        var evDesc by remember { mutableStateOf("") }
        var evCat by remember { mutableStateOf("avaliação") } // avaliação, aviso, evento
        var showCatDropdown by remember { mutableStateOf(false) }

        val calendar = java.util.Calendar.getInstance()
        var dayStr by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH).toString()) }
        var selectedMonthIndex by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH)) } // 0..11
        var yearStr by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR).toString()) }

        val monthsList = listOf(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", 
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
        )

        AlertDialog(
            onDismissRequest = { showCreateCalendarEventDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val day = dayStr.toIntOrNull() ?: calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        val year = yearStr.toIntOrNull() ?: calendar.get(java.util.Calendar.YEAR)
                        val calcCal = java.util.Calendar.getInstance()
                        calcCal.set(java.util.Calendar.YEAR, year)
                        calcCal.set(java.util.Calendar.MONTH, selectedMonthIndex)
                        calcCal.set(java.util.Calendar.DAY_OF_MONTH, day.coerceIn(1, calcCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
                        
                        viewModel.createCalendarEvent(
                            evTitle,
                            evDesc,
                            calcCal.timeInMillis,
                            evCat
                        )
                        showCreateCalendarEventDialog = false
                        Toast.makeText(context, "Anotado na Agenda!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = evTitle.isNotBlank() && evDesc.isNotBlank() && dayStr.isNotBlank() && yearStr.isNotBlank()
                ) {
                    Text("Criar Atividade / Aviso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCalendarEventDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Criar Atividade / Aviso") },
            text = {
                Column {
                    Text("Área administrativa exclusiva para Admins e Super Admins.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = evTitle,
                        onValueChange = { evTitle = it },
                        label = { Text("Título da Atividade") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = evDesc,
                        onValueChange = { evDesc = it },
                        label = { Text("Descrição detalhada") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Data do Evento", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dayStr,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                                    dayStr = newValue
                                }
                            },
                            label = { Text("Dia", style = MaterialTheme.typography.labelSmall) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.weight(1.5f)) {
                            var showMonthDropdown by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showMonthDropdown = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(top = 8.dp)
                            ) {
                                Text(monthsList[selectedMonthIndex], maxLines = 1)
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false }
                            ) {
                                monthsList.forEachIndexed { index, mName ->
                                    DropdownMenuItem(
                                        text = { Text(mName) },
                                        onClick = {
                                            selectedMonthIndex = index
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 4)) {
                                    yearStr = newValue
                                }
                            },
                            label = { Text("Ano", style = MaterialTheme.typography.labelSmall) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showCatDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tipo: ${evCat.uppercase()}")
                        }
                        DropdownMenu(
                            expanded = showCatDropdown,
                            onDismissRequest = { showCatDropdown = false }
                        ) {
                            DropdownMenuItem(text = { Text("AVALIAÇÃO") }, onClick = { evCat = "avaliação"; showCatDropdown = false })
                            DropdownMenuItem(text = { Text("AVISO") }, onClick = { evCat = "aviso"; showCatDropdown = false })
                            DropdownMenuItem(text = { Text("EVENTO") }, onClick = { evCat = "evento"; showCatDropdown = false })
                        }
                    }
                }
            }
        )
    }
}
}

fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

// Helpers for role displaying
fun rolesListForDisplay(profiles: List<Profile>, roles: List<UserRole>): Map<String, String> {
    val roleMap = roles.associateBy { it.user_id }
    return profiles.associate { p ->
        p.id to (roleMap[p.id]?.role ?: "aluno")
    }
}

// ------------------------------------------------------------------------
// COMPONENT: Animated User Avatar with Pulsing Halo Ring for Online Moderators
// ------------------------------------------------------------------------
@Composable
fun AnimatedUserAvatar(
    avatarUrl: String?,
    initials: String,
    isOnline: Boolean,
    isModerator: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    if (isModerator && isOnline) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_avatar")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        Box(
            modifier = modifier.size(size + 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing halo ring background shadow
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(Color(0xFF22C55E), CircleShape)
            )

            // Outer glowing ring
            Box(
                modifier = Modifier
                    .size(size + 4.dp)
                    .border(2.dp, Color(0xFF22C55E), CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarContent(avatarUrl, initials, size)
            }

            // Small glowing green dot badge
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF22C55E), CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            AvatarContent(avatarUrl, initials, size)
            if (isOnline) {
                // simple green dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun AvatarContent(avatarUrl: String?, initials: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(UerjBlue.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val displayInitials = remember(initials) {
            if (initials.isBlank()) "U" else initials.take(2).uppercase()
        }
        if (avatarUrl != null && avatarUrl.startsWith("http")) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder initials
            Text(
                text = displayInitials,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = UerjBlue
            )
        }
    }
}

// ------------------------------------------------------------------------
// COMPONENT: Animated Graduation Cap and Circuit Board Logo (Canvas Native)
// ------------------------------------------------------------------------
@Composable
fun GraduationCircuitLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Left half Diamond/Rhombus Top (Solid White)
        val capLeftPath = Path().apply {
            moveTo(width * 0.15f, height * 0.45f)
            lineTo(width * 0.5f, height * 0.25f)
            lineTo(width * 0.5f, height * 0.65f)
            close()
        }
        drawPath(capLeftPath, Color.White)

        // Right half Diamond/Rhombus Top (Semi-transparent White for circuit base)
        val capRightPath = Path().apply {
            moveTo(width * 0.5f, height * 0.25f)
            lineTo(width * 0.85f, height * 0.45f)
            lineTo(width * 0.5f, height * 0.65f)
            close()
        }
        drawPath(capRightPath, Color.White.copy(alpha = 0.7f))

        // Right side teal glow overlay representing circuit integration
        val circuitBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF81E6D9), Color(0xFF2DD4BF)),
            center = Offset(width * 0.7f, height * 0.45f),
            radius = width * 0.3f
        )
        clipPath(capRightPath) {
            drawRect(
                brush = circuitBrush,
                topLeft = Offset(width * 0.5f, height * 0.2f),
                size = androidx.compose.ui.geometry.Size(width * 0.4f, height * 0.5f)
            )
        }

        // Left side base (Solid White)
        val baseLeftPath = Path().apply {
            moveTo(width * 0.35f, height * 0.55f)
            lineTo(width * 0.35f, height * 0.65f)
            quadraticTo(width * 0.42f, height * 0.70f, width * 0.5f, height * 0.70f)
            lineTo(width * 0.5f, height * 0.59f)
            close()
        }
        drawPath(baseLeftPath, Color.White)

        // Right side base (Teal/White blended base)
        val baseRightPath = Path().apply {
            moveTo(width * 0.5f, height * 0.59f)
            lineTo(width * 0.5f, height * 0.70f)
            quadraticTo(width * 0.58f, height * 0.70f, width * 0.65f, height * 0.65f)
            lineTo(width * 0.65f, height * 0.55f)
            close()
        }
        drawPath(baseRightPath, Color.White.copy(alpha = 0.7f))
        clipPath(baseRightPath) {
            drawRect(
                brush = circuitBrush,
                topLeft = Offset(width * 0.5f, height * 0.5f),
                size = androidx.compose.ui.geometry.Size(width * 0.2f, height * 0.3f)
            )
        }

        // Tassel on the left side (Solid White)
        val tasselPath = Path().apply {
            moveTo(width * 0.5f, height * 0.45f)
            quadraticTo(width * 0.32f, height * 0.45f, width * 0.27f, height * 0.55f)
            lineTo(width * 0.29f, height * 0.62f)
            lineTo(width * 0.25f, height * 0.62f)
            close()
        }
        drawPath(tasselPath, Color.White)

        // Circuit board lines & glowing nodes
        val strokeWidth = 3.dp.toPx()
        val circleRadius = 5.dp.toPx()

        // Upper right line & node
        val startLine1 = Offset(width * 0.58f, height * 0.38f)
        val midLine1 = Offset(width * 0.68f, height * 0.38f)
        val endLine1 = Offset(width * 0.76f, height * 0.29f)
        drawLine(Color.White, startLine1, midLine1, strokeWidth = strokeWidth)
        drawLine(Color.White, midLine1, endLine1, strokeWidth = strokeWidth)
        drawCircle(
            color = Color.White,
            radius = circleRadius * (0.8f + 0.3f * pulseGlow),
            center = endLine1
        )

        // Middle right line & node
        val startLine2 = Offset(width * 0.63f, height * 0.47f)
        val midLine2 = Offset(width * 0.74f, height * 0.39f)
        val endLine2 = Offset(width * 0.85f, height * 0.39f)
        drawLine(Color.White, startLine2, midLine2, strokeWidth = strokeWidth)
        drawLine(Color.White, midLine2, endLine2, strokeWidth = strokeWidth)
        drawCircle(
            color = Color.White,
            radius = circleRadius * (0.8f + 0.3f * pulseGlow),
            center = endLine2
        )

        // Lower right line & node
        val startLine3 = Offset(width * 0.58f, height * 0.56f)
        val midLine3 = Offset(width * 0.68f, height * 0.56f)
        val endLine3 = Offset(width * 0.74f, height * 0.64f)
        drawLine(Color.White, startLine3, midLine3, strokeWidth = strokeWidth)
        drawLine(Color.White, midLine3, endLine3, strokeWidth = strokeWidth)
        drawCircle(
            color = Color.White,
            radius = circleRadius * (0.8f + 0.3f * pulseGlow),
            center = endLine3
        )

        // Vertical bottom line & node
        val startLine4 = Offset(width * 0.5f, height * 0.70f)
        val endLine4 = Offset(width * 0.5f, height * 0.84f)
        drawLine(Color.White, startLine4, endLine4, strokeWidth = strokeWidth)
        drawCircle(
            color = Color.White,
            radius = circleRadius * (0.8f + 0.3f * pulseGlow),
            center = endLine4
        )
    }
}

// ------------------------------------------------------------------------
// SCREEN: Animated Application Splash Screen
// ------------------------------------------------------------------------
@Composable
fun AppSplashScreen() {
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(1200),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dots_anim")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0
                1f at 300
                0.2f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 200
                1f at 500
                0.2f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 400
                1f at 700
                0.2f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B4AB4), Color(0xFF5ED8BF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
        ) {
            GraduationCircuitLogo(
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 32.dp)
            )

            Text(
                text = "Poder da",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Conexão",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Dynamic dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White.copy(alpha = dot1Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White.copy(alpha = dot2Alpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White.copy(alpha = dot3Alpha), CircleShape)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onGoogleLogin: (String, String, String) -> Unit
) {
    var showGoogleChooser by remember { mutableStateOf(false) }

    // Background animation: slow zoom
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundZoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScaleAnimation"
    )

    // Premium Card Entry Animations (Scale & Fade-In)
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.93f) }
    val EaseOutBack = remember { CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.launch {
            cardAlpha.animateTo(1.0f, animationSpec = tween(700, easing = LinearOutSlowInEasing))
        }
        kotlinx.coroutines.launch {
            cardScale.animateTo(1.0f, animationSpec = tween(700, easing = EaseOutBack))
        }
    }

    // Circular green cap badge breathing animation
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeScale"
    )

    // Primary Google Sign-in Button breathing attention animation
    val btnScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BtnScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High density library background with subtle animation
        AsyncImage(
            model = "https://images.unsplash.com/photo-1541339907198-e08756ebafe1?q=80&w=2070&auto=format&fit=crop",
            contentDescription = "Library Background",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentScale = ContentScale.Crop
        )

        // Blue brand overlay tint (semi-transparent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1B4AB4).copy(alpha = 0.85f)) // Set overlay to high quality 0.85f blue as modeled
        )

        // Centered white elevation card with smooth entrance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(28.dp)
                .graphicsLayer(
                    scaleX = cardScale.value,
                    scaleY = cardScale.value,
                    alpha = cardAlpha.value
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Academic Icon in Mint/Green with breathing animation
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer(scaleX = badgeScale, scaleY = badgeScale)
                        .background(Color(0xFFECFDF5), CircleShape), // Updated to correct lighter green Color(0xFFECFDF5)
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = "Graduation Cap",
                        tint = Color(0xFF10B981), // Updated to matching emerald green Color(0xFF10B981)
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Poder da Conexão",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color(0xFF0F172A), // Updated to premium slate Color(0xFF0F172A)
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Portal Educacional",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF64748B), // Updated to premium Slate500 Color(0xFF64748B)
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(44.dp))

                // Standardized Student Google Login Button with Darker Border and breathing attention
                Surface(
                    onClick = { showGoogleChooser = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .graphicsLayer(scaleX = btnScale, scaleY = btnScale),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.8.dp, Color(0xFF1E293B)), // Updated to slate border matching mockup
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        // circular red "G" badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFEA4335), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Entrar com o Google de Estudante",
                            fontWeight = FontWeight.Bold, // Updated to match bold typography
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B) // Slate navy Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Vincule sua conta Google (Gmail/Institucional) para realizar o Onboarding de Estudante e sincronizar seus dados.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6B7280),
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }

    if (showGoogleChooser) {
        GoogleAuthenticatedLoginDialog(
            onAccountSelected = { email, name, avatar ->
                showGoogleChooser = false
                onGoogleLogin(email, name, avatar)
            },
            onDismiss = { showGoogleChooser = false }
        )
    }
}

@Composable
fun GoogleAuthenticatedLoginDialog(
    onAccountSelected: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualEmail by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var useManualEmail by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Escolher uma conta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Text(
                    text = "para prosseguir no app Poder da Conexão",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                )

                if (!useManualEmail) {
                    val accounts = listOf(
                        Triple("jefersonribeiro199026@gmail.com", "Jeferson Ribeiro", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=200&h=200&auto=format&fit=crop")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { (email, name, photo) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAccountSelected(email, name, photo) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (photo.startsWith("http")) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name.take(1), fontWeight = FontWeight.Bold, color = Color(0xFF1B4AB4))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text(email, fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { useManualEmail = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1B4AB4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vincular Outro Google", color = Color(0xFF1B4AB4), fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = manualEmail,
                        onValueChange = { manualEmail = it },
                        label = { Text("E-mail do Google (Gmail/Uerj)") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Nome Completo") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { useManualEmail = false }) {
                            Text("Voltar", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualEmail.isNotEmpty() && manualEmail.contains("@")) {
                                    onAccountSelected(manualEmail, manualName, "avatar_user_default")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4AB4)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Vincular Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SCREEN: STAFF ADMIN & SUPABASE CONFIG PANEL
// ------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffAdminPanelScreen(
    profiles: List<Profile>,
    allRoles: List<UserRole>,
    onUpdateRole: (String, String, String, String, Boolean) -> Unit,
    context: android.content.Context,
    currentUserRole: String,
    viewModel: MainViewModel
) {
    var sUrl by remember(context) { mutableStateOf(SupabaseSync.getUrl(context)) }
    var sKey by remember(context) { mutableStateOf(SupabaseSync.getKey(context)) }
    val isConfigured = remember(sUrl, sKey) { SupabaseSync.isConfigured(context) }
    var isSyncingAll by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var activeSubTab by remember { mutableStateOf("team") } // "team" or "crud"

    // Retrieve live flows of all system tables for CRUD inspection
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val mindmaps by viewModel.allMindmaps.collectAsStateWithLifecycle()
    val helpRequests by viewModel.allHelpRequests.collectAsStateWithLifecycle()
    val studyGroups by viewModel.allStudyGroups.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.allCalendarEvents.collectAsStateWithLifecycle()
    val studyMaterials by viewModel.allStudyMaterials.collectAsStateWithLifecycle()
    val subjects by viewModel.allSubjects.collectAsStateWithLifecycle()

    // CRUD active element states for modal dialog management
    var activeProfileDialog by remember { mutableStateOf<Profile?>(null) }
    var activePostDialog by remember { mutableStateOf<Post?>(null) }
    var activeSubjectDialog by remember { mutableStateOf<Subject?>(null) }
    var activeMindmapDialog by remember { mutableStateOf<Mindmap?>(null) }
    var activeHelpRequestDialog by remember { mutableStateOf<HelpRequest?>(null) }
    var activeStudyGroupDialog by remember { mutableStateOf<StudyGroup?>(null) }
    var activeCalendarEventDialog by remember { mutableStateOf<CalendarEvent?>(null) }
    var activeStudyMaterialDialog by remember { mutableStateOf<StudyMaterial?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row for toggle
        TabRow(
            selectedTabIndex = if (activeSubTab == "team") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = UerjBlue
        ) {
            Tab(
                selected = activeSubTab == "team",
                onClick = { activeSubTab = "team" },
                text = { Text("Membros & Configuração", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == "crud",
                onClick = { activeSubTab = "crud" },
                text = { Text("Console de CRUD (BD)", fontWeight = FontWeight.Bold) }
            )
        }

        if (activeSubTab == "team") {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Team management Section
                item {
                    Text(
                        text = "GESTÃO DE CARGOS & STAFF (${profiles.size} Usuários)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Slate800,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(profiles) { p ->
                    val uRole = allRoles.find { it.user_id == p.id } ?: UserRole(user_id = p.id, role = "aluno")
                    var expandedOptions by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Slate100),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedUserAvatar(
                                        avatarUrl = p.foto_url,
                                        initials = p.nome.take(2),
                                        isOnline = uRole.online,
                                        isModerator = uRole.role != "aluno",
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = p.nome,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = p.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = if (p.curso.isBlank()) "Aguardando onboarding..." else p.curso,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = UerjBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Role selection badge
                                Box {
                                    var showRoleMenu by remember { mutableStateOf(false) }
                                    AssistChip(
                                        onClick = { showRoleMenu = true },
                                        label = {
                                            Text(
                                                text = when(uRole.role) {
                                                    "super_admin" -> "Super Admin"
                                                    "moderador" -> "Moderador"
                                                    else -> "Aluno"
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when(uRole.role) {
                                                    "super_admin" -> Icons.Filled.Security
                                                    "moderador" -> Icons.Filled.VerifiedUser
                                                    else -> Icons.Filled.Person
                                                    },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    DropdownMenu(
                                        expanded = showRoleMenu,
                                        onDismissRequest = { showRoleMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Super Admin") },
                                            onClick = {
                                                onUpdateRole(p.id, "super_admin", uRole.permissions, uRole.principal_area, uRole.online)
                                                showRoleMenu = false
                                                Toast.makeText(context, "${p.nome} promovido a Super Admin", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Moderador") },
                                            onClick = {
                                                onUpdateRole(p.id, "moderador", uRole.permissions, uRole.principal_area, uRole.online)
                                                showRoleMenu = false
                                                Toast.makeText(context, "${p.nome} promovido a Moderador", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Aluno") },
                                            onClick = {
                                                onUpdateRole(p.id, "aluno", uRole.permissions, uRole.principal_area, uRole.online)
                                                showRoleMenu = false
                                                Toast.makeText(context, "${p.nome} despromovido a Aluno", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Config options triggers if is moderator or super admin
                            if (uRole.role == "moderador" || uRole.role == "super_admin") {
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedOptions = !expandedOptions }
                                        .background(Slate100, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate500)
                                        Text("Opções do Staff", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Slate800)
                                    }
                                    Icon(
                                        imageVector = if (expandedOptions) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Slate500
                                    )
                                }

                                if (expandedOptions) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp, start = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "ÁREA DE RESPONSABILIDADE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        var respArea by remember(uRole.principal_area) { mutableStateOf(uRole.principal_area) }
                                        OutlinedTextField(
                                            value = respArea,
                                            onValueChange = {
                                                respArea = it
                                                onUpdateRole(uRole.user_id, uRole.role, uRole.permissions, it, uRole.online)
                                            },
                                            placeholder = { Text("Geral ou Matéria específica") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )

                                        HorizontalDivider(color = Slate100)

                                        Text(
                                            text = "PERMISSÕES ESPECÍFICAS DA PLATAFORMA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        val permList = remember(uRole.permissions) {
                                            val items = uRole.permissions.split(",").filter { it.isNotEmpty() }
                                            mutableStateListOf<String>().apply { addAll(items) }
                                        }

                                        val options = listOf(
                                            Pair("delete_posts", "Permitir apagar posts de alunos"),
                                            Pair("manage_events", "Permitir postar avisos na Agenda"),
                                            Pair("moderate_chats", "Permitir moderar salas de chat")
                                        )

                                        options.forEach { (permCode, permLabel) ->
                                            val isChecked = permList.contains(permCode)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isChecked) {
                                                            permList.remove(permCode)
                                                        } else {
                                                            permList.add(permCode)
                                                        }
                                                        val newPermString = permList.joinToString(",")
                                                        onUpdateRole(uRole.user_id, uRole.role, newPermString, uRole.principal_area, uRole.online)
                                                    },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        if (permList.contains(permCode)) {
                                                            permList.remove(permCode)
                                                        } else {
                                                            permList.add(permCode)
                                                        }
                                                        val newPermString = permList.joinToString(",")
                                                        onUpdateRole(uRole.user_id, uRole.role, newPermString, uRole.principal_area, uRole.online)
                                                    }
                                                )
                                                Text(permLabel, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }

                                        HorizontalDivider(color = Slate100)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(if (uRole.online) Color(0xFF22C55E) else Color.Gray, CircleShape)
                                                )
                                                Text(
                                                    text = if (uRole.online) "Staff Conectado (Online)" else "Staff Ausente (Offline)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            
                                            Switch(
                                                checked = uRole.online,
                                                onCheckedChange = { onlineState ->
                                                    onUpdateRole(uRole.user_id, uRole.role, uRole.permissions, uRole.principal_area, onlineState)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // DATABASE UNIFIED CRUD EXPLORER & ADMIN CONSOLE
            var selectedTable by remember { mutableStateOf("profiles") } // profiles, posts, subjects, mindmaps, help_requests, study_groups, calendar_events, study_materials
            var queryText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Entity selection and management header elements removed per request

                // Filter bar removed per request

                // List of Records based on Selected Entity Table
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when(selectedTable) {
                        "profiles" -> {
                            val filtered = if (queryText.isBlank()) profiles else profiles.filter {
                                it.nome.contains(queryText, ignoreCase = true) || it.id.contains(queryText, ignoreCase = true) || it.curso.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👤", fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(p.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(p.id, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Curso: ${p.curso} | ${p.periodo}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                                        Text("Bio: ${p.bio.take(100)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        if (p.selected_materia.isNotBlank()) {
                                            Text("Matéria: ${p.selected_materia}", style = MaterialTheme.typography.bodySmall, color = UerjGreen)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeProfileDialog = p }) {
                                                Text("Editar", color = UerjBlue, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteProfileDirectly(p.id)
                                                Toast.makeText(context, "Perfil removido!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "posts" -> {
                            val filtered = if (queryText.isBlank()) posts else posts.filter {
                                it.content.contains(queryText, ignoreCase = true) || it.author_name.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { post ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📅 ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(post.created_at))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("Autor: ${post.author_name} (${post.author_id})", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = UerjBlue)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(post.content, style = MaterialTheme.typography.bodyMedium, color = Slate800)
                                        if (post.media_url != null) {
                                            Text("Attachment: ${post.media_url}", style = MaterialTheme.typography.labelSmall, color = UerjGreen, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activePostDialog = post }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deletePost(post)
                                                Toast.makeText(context, "Postagem removida!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "subjects" -> {
                            val filtered = if (queryText.isBlank()) subjects else subjects.filter {
                                it.title.contains(queryText, ignoreCase = true) || it.description.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { subject ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("📚 ID: ${subject.id}", style = MaterialTheme.typography.labelSmall, color = UerjGreen, fontWeight = FontWeight.Bold)
                                        Text(subject.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(subject.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeSubjectDialog = subject }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteSubjectDirectly(subject.id)
                                                Toast.makeText(context, "Disciplina deletada!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "mindmaps" -> {
                            val filtered = if (queryText.isBlank()) mindmaps else mindmaps.filter {
                                it.title.contains(queryText, ignoreCase = true) || it.author_name.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { map ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🧠 ID: ${map.id} | Autor: ${map.author_name}", style = MaterialTheme.typography.labelSmall, color = UerjGreen)
                                        Text(map.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(map.description, style = MaterialTheme.typography.bodySmall, color = Slate600)
                                        Text("Tags: ${map.tags}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeMindmapDialog = map }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteMindmapDirectly(map.id)
                                                Toast.makeText(context, "Mapa mental removido!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "help_requests" -> {
                            val filtered = if (queryText.isBlank()) helpRequests else helpRequests.filter {
                                it.title.contains(queryText, ignoreCase = true) || it.author_name.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { req ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("🤝 ID: ${req.id} | Disciplina ID: ${req.subject_id}", style = MaterialTheme.typography.labelSmall, color = UerjBlue)
                                            Text(
                                                text = if (req.is_resolved) "RESOLVIDO" else "PENDENTE",
                                                color = if (req.is_resolved) UerjGreen else UerjYellow,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Text(req.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(req.description, style = MaterialTheme.typography.bodySmall, color = Slate600)
                                        Text("Solicitante: ${req.author_name} (${req.author_id})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeHelpRequestDialog = req }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteHelpRequestDirectly(req.id)
                                                Toast.makeText(context, "Pedido excluído!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "study_groups" -> {
                            val filtered = if (queryText.isBlank()) studyGroups else studyGroups.filter {
                                it.name.contains(queryText, ignoreCase = true) || it.description.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { group ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("👥 ID: ${group.id} | Criado por: ${group.created_by}", style = MaterialTheme.typography.labelSmall, color = UerjGreen)
                                        Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(group.description, style = MaterialTheme.typography.bodySmall, color = Slate600)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeStudyGroupDialog = group }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteStudyGroupDirectly(group.id)
                                                Toast.makeText(context, "Grupo de estudos excluído!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "calendar_events" -> {
                            val filtered = if (queryText.isBlank()) calendarEvents else calendarEvents.filter {
                                it.title.contains(queryText, ignoreCase = true) || it.category.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { event ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("📅 ID: ${event.id} | Categoria: ${event.category.uppercase()}", style = MaterialTheme.typography.labelSmall, color = UerjBlue, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(event.date)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(event.description, style = MaterialTheme.typography.bodySmall, color = Slate600)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeCalendarEventDialog = event }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteCalendarEvent(event.id)
                                                Toast.makeText(context, "Evento da agenda removido!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "study_materials" -> {
                            val filtered = if (queryText.isBlank()) studyMaterials else studyMaterials.filter {
                                it.title.contains(queryText, ignoreCase = true) || it.type.contains(queryText, ignoreCase = true)
                            }
                            items(filtered) { mat ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate100),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("📑 ID: ${mat.id} | Tipo: ${mat.type.uppercase()}", style = MaterialTheme.typography.labelSmall, color = UerjGreen, fontWeight = FontWeight.Bold)
                                            Text("Postado por: ${mat.author_name}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Text(mat.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(mat.description, style = MaterialTheme.typography.bodySmall, color = Slate600)
                                        Text("Arquivo: ${mat.file_url}", style = MaterialTheme.typography.labelSmall, color = Slate500, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { activeStudyMaterialDialog = mat }) {
                                                Text("Editar", color = UerjBlue)
                                            }
                                            TextButton(onClick = { 
                                                viewModel.deleteStudyMaterial(mat.id)
                                                Toast.makeText(context, "Material excluído!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("Deletar", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal forms insertion/editing alerts
    if (activeProfileDialog != null) {
        val editingProfile = activeProfileDialog!!
        val isEdit = editingProfile.id.isNotBlank() && profiles.any { it.id == editingProfile.id }
        var id by remember { mutableStateOf(editingProfile.id) }
        var nome by remember { mutableStateOf(editingProfile.nome) }
        var curso by remember { mutableStateOf(editingProfile.curso) }
        var periodo by remember { mutableStateOf(editingProfile.periodo) }
        var bio by remember { mutableStateOf(editingProfile.bio) }
        var fotoUrl by remember { mutableStateOf(editingProfile.foto_url) }
        var selectedMateria by remember { mutableStateOf(editingProfile.selected_materia) }

        AlertDialog(
            onDismissRequest = { activeProfileDialog = null },
            title = { Text(if (isEdit) "Editar Perfil" else "Novo Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isEdit) {
                        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("ID E-mail de Usuário") }, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Editando conta: $id", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = UerjBlue)
                    }
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome Completo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = curso, onValueChange = { curso = it }, label = { Text("Curso (ex: Pedagogia (UERJ))") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = periodo, onValueChange = { periodo = it }, label = { Text("Período") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Biologia") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fotoUrl, onValueChange = { fotoUrl = it }, label = { Text("Caminho Avatar / URL Foto") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = selectedMateria, onValueChange = { selectedMateria = it }, label = { Text("Matéria Selecionada") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (id.isNotBlank() && nome.isNotBlank()) {
                        viewModel.saveProfileDirectly(Profile(id, nome, curso, periodo, bio, fotoUrl, selectedMateria))
                        activeProfileDialog = null
                        Toast.makeText(context, "Perfil salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeProfileDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activePostDialog != null) {
        val editingPost = activePostDialog!!
        val isEdit = editingPost.id != 0
        var authorId by remember { mutableStateOf(editingPost.author_id) }
        var authorName by remember { mutableStateOf(editingPost.author_name) }
        var authorAvatar by remember { mutableStateOf(editingPost.author_avatar) }
        var subjectIdStr by remember { mutableStateOf(editingPost.subject_id?.toString() ?: "") }
        var content by remember { mutableStateOf(editingPost.content) }
        var mediaUrl by remember { mutableStateOf(editingPost.media_url ?: "") }

        AlertDialog(
            onDismissRequest = { activePostDialog = null },
            title = { Text(if (isEdit) "Editar Postagem" else "Nova Postagem") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = authorId, onValueChange = { authorId = it }, label = { Text("E-mail Autor ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Nome do Autor") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorAvatar, onValueChange = { authorAvatar = it }, label = { Text("Identificador Avatar") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subjectIdStr, onValueChange = { subjectIdStr = it }, label = { Text("ID da Disciplina (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Conteúdo de Texto") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = mediaUrl, onValueChange = { mediaUrl = it }, label = { Text("Link de Mídia (Opcional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (authorId.isNotBlank() && authorName.isNotBlank() && content.isNotBlank()) {
                        viewModel.savePostDirectly(Post(
                            id = editingPost.id,
                            author_id = authorId,
                            author_name = authorName,
                            author_avatar = authorAvatar,
                            subject_id = subjectIdStr.toIntOrNull(),
                            content = content,
                            media_url = if (mediaUrl.isBlank()) null else mediaUrl,
                            likes_count = editingPost.likes_count
                        ))
                        activePostDialog = null
                        Toast.makeText(context, "Postagem salva!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activePostDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeSubjectDialog != null) {
        val editingSub = activeSubjectDialog!!
        var title by remember { mutableStateOf(editingSub.title) }
        var description by remember { mutableStateOf(editingSub.description) }

        AlertDialog(
            onDismissRequest = { activeSubjectDialog = null },
            title = { Text(if (editingSub.id != 0) "Editar Disciplina" else "Nova Disciplina") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nome da Disciplina") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.saveSubjectDirectly(Subject(id = editingSub.id, title = title, description = description))
                        activeSubjectDialog = null
                        Toast.makeText(context, "Disciplina salva!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeSubjectDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeMindmapDialog != null) {
        val editingMap = activeMindmapDialog!!
        var title by remember { mutableStateOf(editingMap.title) }
        var description by remember { mutableStateOf(editingMap.description) }
        var imageUrl by remember { mutableStateOf(editingMap.image_url) }
        var authorId by remember { mutableStateOf(editingMap.author_id) }
        var authorName by remember { mutableStateOf(editingMap.author_name) }
        var tags by remember { mutableStateOf(editingMap.tags) }

        AlertDialog(
            onDismissRequest = { activeMindmapDialog = null },
            title = { Text(if (editingMap.id != 0) "Editar Mapa Mental" else "Novo Mapa Mental") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título do Mapa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL / Caminho Imagem") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorId, onValueChange = { authorId = it }, label = { Text("Autor ID (E-mail)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Nome do Autor") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (separadas por vírgula)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && authorId.isNotBlank()) {
                        viewModel.saveMindmapDirectly(Mindmap(
                            id = editingMap.id, title = title, description = description, image_url = imageUrl,
                            author_id = authorId, author_name = authorName, tags = tags
                        ))
                        activeMindmapDialog = null
                        Toast.makeText(context, "Mapa mental salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeMindmapDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeHelpRequestDialog != null) {
        val editingReq = activeHelpRequestDialog!!
        var title by remember { mutableStateOf(editingReq.title) }
        var description by remember { mutableStateOf(editingReq.description) }
        var subjectIdStr by remember { mutableStateOf(editingReq.subject_id.toString()) }
        var authorId by remember { mutableStateOf(editingReq.author_id) }
        var authorName by remember { mutableStateOf(editingReq.author_name) }
        var isResolved by remember { mutableStateOf(editingReq.is_resolved) }

        AlertDialog(
            onDismissRequest = { activeHelpRequestDialog = null },
            title = { Text(if (editingReq.id != 0) "Editar Ajuda" else "Nova Ajuda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título da Discussão") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subjectIdStr, onValueChange = { subjectIdStr = it }, label = { Text("ID da Matéria") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorId, onValueChange = { authorId = it }, label = { Text("Autor ID (E-mail)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Nome do Autor") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isResolved, onCheckedChange = { isResolved = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dúvida já encontrada e resolvida")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && authorId.isNotBlank()) {
                        viewModel.saveHelpRequestDirectly(HelpRequest(
                            id = editingReq.id, title = title, description = description, 
                            subject_id = subjectIdStr.toIntOrNull() ?: 1, author_id = authorId,
                            author_name = authorName, is_resolved = isResolved
                        ))
                        activeHelpRequestDialog = null
                        Toast.makeText(context, "Pedido de ajuda salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeHelpRequestDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeStudyGroupDialog != null) {
        val editingGrp = activeStudyGroupDialog!!
        var name by remember { mutableStateOf(editingGrp.name) }
        var description by remember { mutableStateOf(editingGrp.description) }
        var createdBy by remember { mutableStateOf(editingGrp.created_by) }

        AlertDialog(
            onDismissRequest = { activeStudyGroupDialog = null },
            title = { Text(if (editingGrp.id != 0) "Editar Grupo" else "Novo Grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Grupo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = createdBy, onValueChange = { createdBy = it }, label = { Text("Criador ID (E-mail)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && createdBy.isNotBlank()) {
                        viewModel.saveStudyGroupDirectly(StudyGroup(
                            id = editingGrp.id, name = name, description = description, created_by = createdBy
                        ))
                        activeStudyGroupDialog = null
                        Toast.makeText(context, "Grupo de estudos salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeStudyGroupDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeCalendarEventDialog != null) {
        val editingEvt = activeCalendarEventDialog!!
        var title by remember { mutableStateOf(editingEvt.title) }
        var description by remember { mutableStateOf(editingEvt.description) }
        var dateValStr by remember { mutableStateOf(editingEvt.date.toString()) }
        var category by remember { mutableStateOf(editingEvt.category) }
        var createdBy by remember { mutableStateOf(editingEvt.created_by) }

        AlertDialog(
            onDismissRequest = { activeCalendarEventDialog = null },
            title = { Text(if (editingEvt.id != 0) "Editar Evento da Agenda" else "Novo Evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição de Evento") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateValStr, onValueChange = { dateValStr = it }, label = { Text("Timestamp Data (ou epoch millis)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria (avaliação, aviso, evento)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = createdBy, onValueChange = { createdBy = it }, label = { Text("Criador ID (E-mail)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && createdBy.isNotBlank()) {
                        viewModel.saveCalendarEventDirectly(CalendarEvent(
                            id = editingEvt.id, title = title, description = description,
                            date = dateValStr.toLongOrNull() ?: System.currentTimeMillis(),
                            category = category, created_by = createdBy
                        ))
                        activeCalendarEventDialog = null
                        Toast.makeText(context, "Evento de agenda salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeCalendarEventDialog = null }) { Text("Cancelar") } }
        )
    }

    if (activeStudyMaterialDialog != null) {
        val editingMat = activeStudyMaterialDialog!!
        var title by remember { mutableStateOf(editingMat.title) }
        var description by remember { mutableStateOf(editingMat.description) }
        var fileUrl by remember { mutableStateOf(editingMat.file_url) }
        var type by remember { mutableStateOf(editingMat.type) }
        var authorId by remember { mutableStateOf(editingMat.author_id) }
        var authorName by remember { mutableStateOf(editingMat.author_name) }

        AlertDialog(
            onDismissRequest = { activeStudyMaterialDialog = null },
            title = { Text(if (editingMat.id != 0) "Editar Material" else "Novo Material") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título do Material") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição Curta") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fileUrl, onValueChange = { fileUrl = it }, label = { Text("URL / Endereço Relativo do PDF") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Tipo (material, prova, audiobook)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorId, onValueChange = { authorId = it }, label = { Text("Autor ID (E-mail)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Nome do Autor") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && authorId.isNotBlank()) {
                        viewModel.saveStudyMaterialDirectly(StudyMaterial(
                            id = editingMat.id, title = title, description = description, file_url = fileUrl,
                            type = type, author_id = authorId, author_name = authorName
                        ))
                        activeStudyMaterialDialog = null
                        Toast.makeText(context, "Material de estudo salvo!", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { activeStudyMaterialDialog = null }) { Text("Cancelar") } }
        )
    }
}

// ------------------------------------------------------------------------
// SCREEN: Dashboard (Dashboard + Feed)
// ------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    posts: List<Post>,
    subjects: List<Subject>,
    calendarEvents: List<CalendarEvent>,
    userLikes: List<PostLike>,
    currentUserRole: String,
    currentUserId: String,
    searchQuery: String,
    filteredSubjectId: Int?,
    onQueryChange: (String) -> Unit,
    onSelectSubject: (Int?) -> Unit,
    onToggleLike: (Int) -> Unit,
    onAddComment: (Int, String) -> Unit,
    onDeletePost: (Post) -> Unit,
    onDeleteComment: (Comment) -> Unit,
    commentsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<Comment>>,
    onTabChange: (String) -> Unit = {},
    allRoles: List<UserRole> = emptyList()
) {
    val isDark = isSystemInDarkTheme()
    // Search & Filter computation
    val filteredPosts = remember(posts, searchQuery, filteredSubjectId) {
        posts.filter { p ->
            val matchesSearch = if (searchQuery.isBlank()) true else p.content.contains(searchQuery, ignoreCase = true)
            val matchesSubject = if (filteredSubjectId == null) true else p.subject_id == filteredSubjectId
            matchesSearch && matchesSubject
        }
    }

    // Weekly hot post calculation (most liked)
    val hotPost = remember(posts) {
        posts.filter { it.created_at > System.currentTimeMillis() - (86400000 * 7) }
            .maxByOrNull { it.likes_count }
    }

    // Next event calculation
    val nextEvent = remember(calendarEvents) {
        calendarEvents.minByOrNull { it.date }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isWide) Modifier.widthIn(max = 840.dp).align(Alignment.TopCenter) else Modifier),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


        // Subjects Carousel
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(UerjGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FILTRAR POR MATÉRIA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Slate500
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filteredSubjectId == null,
                            onClick = { onSelectSubject(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(subjects) { sub ->
                        FilterChip(
                            selected = filteredSubjectId == sub.id,
                            onClick = { onSelectSubject(sub.id) },
                            label = { Text(sub.title) }
                        )
                    }
                }
            }
        }

        // Section "Em alta" (semana)
        if (hotPost != null && searchQuery.isBlank() && filteredSubjectId == null) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFE11D48), CircleShape) // Rose-500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FEED: EM ALTA NA SEMANA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Slate800
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Blue50, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VER TODOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UerjBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    PostCard(
                        post = hotPost,
                        isLiked = userLikes.any { it.post_id == hotPost.id },
                        canModerate = currentUserRole != "aluno",
                        onToggleLike = { onToggleLike(hotPost.id) },
                        onAddComment = { onAddComment(hotPost.id, it) },
                        onDelete = { onDeletePost(hotPost) },
                        onDeleteComment = onDeleteComment,
                        commentsProvider = commentsProvider,
                        allRoles = allRoles
                    )
                }
            }
        }

        // Feed List Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(UerjBlue, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (filteredSubjectId != null) {
                        "TÓPICOS DE ${subjects.find { it.id == filteredSubjectId }?.title?.uppercase()}"
                    } else {
                        "FEED DE IDEIAS"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Slate800
                )
            }
        }

        if (filteredPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Abra o app e compartilhe as primeiras ideias!", color = Color.Gray)
                    }
                }
            }
        } else {
            val listPosts = if (hotPost != null && searchQuery.isBlank() && filteredSubjectId == null) {
                filteredPosts.filter { it.id != hotPost.id }
            } else {
                filteredPosts
            }
            items(listPosts) { p ->
                PostCard(
                    post = p,
                    isLiked = userLikes.any { it.post_id == p.id },
                    canModerate = currentUserRole != "aluno",
                    onToggleLike = { onToggleLike(p.id) },
                    onAddComment = { onAddComment(p.id, it) },
                    onDelete = { onDeletePost(p) },
                    onDeleteComment = onDeleteComment,
                    commentsProvider = commentsProvider,
                    allRoles = allRoles
                )
            }
        }
    }
    }
}

// ------------------------------------------------------------------------
// COMPONENT: GridActionItem
// ------------------------------------------------------------------------
@Composable
fun GridActionItem(
    title: String,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------------
// UI COMPONENT: Post Card & Simple Inline Comment Thread
// ------------------------------------------------------------------------
@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    canModerate: Boolean,
    onToggleLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteComment: (Comment) -> Unit,
    commentsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<Comment>>,
    allRoles: List<UserRole> = emptyList()
) {
    val comments by commentsProvider(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentExpanded by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate100),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Extract placeholder letters JP / MR for lovely design avatar blocks
                    val initials = remember(post.author_name) {
                        post.author_name.split(" ").filter { it.isNotEmpty() }.take(2)
                            .map { it.first().uppercaseChar() }.joinToString("")
                    }

                    val uRole = remember(allRoles, post.author_id) {
                        allRoles.find { it.user_id == post.author_id }
                    }

                    AnimatedUserAvatar(
                        avatarUrl = post.author_avatar,
                        initials = initials,
                        isOnline = uRole?.online ?: false,
                        isModerator = uRole?.role != null && uRole.role != "aluno",
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = post.author_name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(post.created_at)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            if (post.subject_id != null) {
                                Text(
                                    text = " • 4º Período",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Delete quick moderation check
                if (canModerate) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Moderar", tint = Color(0xFFEF4444))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            // Optional unique media (PNG/JPG simulated fallback representation)
            if (!post.media_url.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = post.media_url,
                        contentDescription = "Post Media Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Tag representation
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("1 Mídia", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interaction Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like Button
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFE11D48) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.likes_count}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    // Comments expand trigger
                    IconButton(onClick = { commentExpanded = !commentExpanded }) {
                        Icon(
                            imageVector = Icons.Outlined.Message,
                            contentDescription = "Comentar",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${comments.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                // Overlapping Stack of readers/listeners representation
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8)).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(col, CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }
            }

            // Inline comment threads
            if (commentExpanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate100)
                // Comment List
                comments.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Slate100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate500)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(c.author_name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(c.content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        // Moderator delete comment
                        if (canModerate) {
                            IconButton(onClick = { onDeleteComment(c) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete Comment", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Comment input
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Escreva um comentário...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = UerjGreen)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SCREEN: Mindmaps Visual Gallery & Study Hub
// ------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MindmapScreen(
    mindmaps: List<Mindmap>,
    favorites: List<MindmapFavorite>,
    searchQuery: String,
    currentUser: Profile?,
    onQueryChange: (String) -> Unit,
    onToggleFav: (Int) -> Unit,
    currentUserRole: String,
    studyMaterials: List<StudyMaterial>,
    onAddMaterial: (String, String, String, String) -> Unit,
    onDeleteMaterial: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isUploadingMaterial by remember { mutableStateOf(false) }
    var uploadedMaterialUrl by remember { mutableStateOf("") }
    var uploadedMaterialFileName by remember { mutableStateOf<String?>(null) }

    val materialFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "material_estudo.pdf"
            isUploadingMaterial = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(1200) // simulated upload delay
                isUploadingMaterial = false
                uploadedMaterialUrl = "${SupabaseSync.getUrl(context).trimEnd('/')}/storage/v1/object/public/materials/$fileName"
                uploadedMaterialFileName = fileName
                Toast.makeText(context, "Arquivo carregado para o Storage: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Mapas, 1: Provas & Resumos, 2: Audiobooks MP3
    
    // Dialog triggers
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var materialTypeToCreate by remember { mutableStateOf("prova") } // "prova", "material", "audiobook"

    // Search query filtering
    val filteredMaps = remember(mindmaps, searchQuery) {
        mindmaps.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.tags.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredMaterials = remember(studyMaterials, searchQuery) {
        studyMaterials.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    var selectedMapDetail by remember { mutableStateOf<Mindmap?>(null) }
    
    // MP3 Player State
    var currentPlayingAudio by remember { mutableStateOf<StudyMaterial?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0.35f) } // simulated slider progress

    // Simulated playback loop for visual realism
    LaunchedEffect(isAudioPlaying) {
        if (isAudioPlaying) {
            while (isAudioPlaying) {
                kotlinx.coroutines.delay(1000)
                audioProgress = (audioProgress + 0.02f)
                if (audioProgress >= 1.0f) {
                    audioProgress = 0.0f
                    isAudioPlaying = false
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(if (isWide) Modifier.widthIn(max = 840.dp).align(Alignment.TopCenter) else Modifier)
        ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            label = { Text("Pesquisar material didático e mapas...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal navigation tabs for study resources
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Mapas Mentais", style = MaterialTheme.typography.labelMedium) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Provas & Materiais", style = MaterialTheme.typography.labelMedium) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Audiobooks MP3", style = MaterialTheme.typography.labelMedium) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Conditional controls for staff/moderators
        val isStaff = currentUserRole == "moderador" || currentUserRole == "super_admin"
        if (isStaff) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ações do Corpo Docente / Staff",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = {
                        materialTypeToCreate = when (selectedSubTab) {
                            1 -> "prova"
                            2 -> "audiobook"
                            else -> "material"
                        }
                        uploadedMaterialUrl = ""
                        uploadedMaterialFileName = null
                        showAddMaterialDialog = true
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (selectedSubTab) {
                            1 -> "Enviar Prova/Material"
                            2 -> "Enviar Audiobook (MP3)"
                            else -> "Enviar Mapa Mental"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Sub tab contents
        when (selectedSubTab) {
            0 -> {
                // Mapas Mentais
                Text("Mapas de Aprendizagem Visual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Material gerado cooperativamente pelos estudantes de Pedagogia.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredMaps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum mapa mental encontrado.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredMaps) { map ->
                            val isFav = favorites.any { it.mindmap_id == map.id }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMapDetail = map },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .background(UerjBlue.copy(alpha = 0.1f))
                                    ) {
                                        AsyncImage(
                                            model = map.image_url,
                                            contentDescription = map.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                            IconButton(
                                                onClick = { onToggleFav(map.id) },
                                                modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                    contentDescription = "Favoritar",
                                                    tint = if (isFav) UerjYellow else Color.DarkGray
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(map.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(map.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            map.tags.split(",").forEach { tag ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(UerjGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(tag.trim(), color = UerjGreen, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Postado por: ${map.author_name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Provas & Materiais
                Text("Banco de Provas & Apostilas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Repositório oficial para download de materiais PDF e exames anteriores.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                val docMaterials = filteredMaterials.filter { it.type == "prova" || it.type == "material" }
                if (docMaterials.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum material de documento disponível.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(docMaterials) { doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (doc.type == "prova") Icons.Filled.AssignmentTurnedIn else Icons.Filled.FileCopy,
                                        contentDescription = null,
                                        tint = if (doc.type == "prova") UerjBlue else UerjGreen,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(doc.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "De: ${doc.author_name} • Tipo: ${doc.type.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                android.util.Log.d("STUDY_HUB", "Baixando material: ${doc.file_url}")
                                            }
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = "Download")
                                        }
                                        if (isStaff) {
                                            IconButton(
                                                onClick = { onDeleteMaterial(doc.id) }
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Deletar", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Audiobooks & Podcasts (MP3)
                Text("Resumos & Audiobooks em MP3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Estude ouvindo os episódios produzidos pelo corpo docente e tutoria.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                val audioMaterials = filteredMaterials.filter { it.type == "audiobook" }
                if (audioMaterials.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum audiobook no momento.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(audioMaterials) { audio ->
                            val isPlayingThis = currentPlayingAudio?.id == audio.id
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingThis) {
                                                isAudioPlaying = !isAudioPlaying
                                            } else {
                                                currentPlayingAudio = audio
                                                isAudioPlaying = true
                                                audioProgress = 0.0f
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis && isAudioPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                            contentDescription = "Ouvir",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(audio.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(audio.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Produtor: ${audio.author_name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isStaff) {
                                        IconButton(
                                            onClick = {
                                                if (isPlayingThis) {
                                                    currentPlayingAudio = null
                                                    isAudioPlaying = false
                                                }
                                                onDeleteMaterial(audio.id)
                                            }
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Deletar", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Audio Player Drawer Component (If any audio is selected)
                if (currentPlayingAudio != null) {
                    val audio = currentPlayingAudio!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = "Audiobook tocando", tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(audio.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Tocando agora unificado...", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    }
                                }
                                IconButton(onClick = {
                                    currentPlayingAudio = null
                                    isAudioPlaying = false
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Fechar player")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Simulated Slider Progress
                            Slider(
                                value = audioProgress,
                                onValueChange = { audioProgress = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("0:${String.format("%02d", (audioProgress * 120).toInt())}", style = MaterialTheme.typography.labelSmall)
                                Text("2:00", style = MaterialTheme.typography.labelSmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { audioProgress = java.lang.Math.max(0.0f, audioProgress - 0.1f) }) {
                                    Icon(Icons.Filled.Replay10, contentDescription = "Voltar 10s")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = { isAudioPlaying = !isAudioPlaying }
                                ) {
                                    Icon(
                                        imageVector = if (isAudioPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(onClick = { audioProgress = java.lang.Math.min(1.0f, audioProgress + 0.1f) }) {
                                    Icon(Icons.Filled.Forward10, contentDescription = "Avançar 10s")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal detailed View for Mindmaps
    if (selectedMapDetail != null) {
        val map = selectedMapDetail!!
        Dialog(onDismissRequest = { selectedMapDetail = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(map.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { selectedMapDetail = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                    ) {
                        AsyncImage(
                            model = map.image_url,
                            contentDescription = map.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Autor: ${map.author_name}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(map.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tags:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(map.tags, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // Create custom Study Material Dialog for Staff
    if (showAddMaterialDialog) {
        var resTitle by remember { mutableStateOf("") }
        var resDesc by remember { mutableStateOf("") }
        var resType by remember { mutableStateOf(materialTypeToCreate) }
        var resUrl by remember { mutableStateOf(uploadedMaterialUrl) }

        LaunchedEffect(uploadedMaterialUrl) {
            if (uploadedMaterialUrl.isNotBlank()) {
                resUrl = uploadedMaterialUrl
                if (resTitle.isBlank() && uploadedMaterialFileName != null) {
                    resTitle = uploadedMaterialFileName!!.substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddMaterialDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val fileLink = resUrl.ifBlank {
                            if (resType == "audiobook") "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                            else "https://www.uerj.br/material_enviado.pdf"
                        }
                        onAddMaterial(resTitle, resDesc, fileLink, resType)
                        showAddMaterialDialog = false
                    },
                    enabled = resTitle.isNotBlank()
                ) {
                    Text("Enviar Recurso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaterialDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text(if (resType == "audiobook") "Sincronizar Audiobook MP3" else "Enviar Prova / Resumo") },
            text = {
                Column {
                    Text("Especifique as notas do material para os vestibulandos e calouros estudarem.", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = resTitle,
                        onValueChange = { resTitle = it },
                        label = { Text("Título do Recurso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("Descrição acadêmica") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resUrl,
                        onValueChange = { resUrl = it },
                        label = { Text("Link ou Arquivo (URL)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isUploadingMaterial) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Enviando arquivo para o Storage...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (uploadedMaterialFileName != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle, 
                                    contentDescription = "Sucesso", 
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Upload concluído com sucesso!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(uploadedMaterialFileName!!, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val mimeType = when (resType) {
                                "audiobook" -> "audio/*"
                                "prova" -> "application/pdf"
                                else -> "*/*"
                            }
                            materialFilePickerLauncher.launch(mimeType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (resType) {
                                "audiobook" -> "Selecionar e Subir MP3"
                                "prova" -> "Selecionar e Subir PDF"
                                else -> "Selecionar e Subir Arquivo"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Tipo do arquivo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("prova", "material", "audiobook").forEach { t ->
                            FilterChip(
                                selected = resType == t,
                                onClick = { resType = t },
                                label = { Text(t.uppercase()) }
                            )
                        }
                    }
                }
            }
        )
    }
    }
}

// ------------------------------------------------------------------------
// SCREEN / HUB: Help Desk & Study Groups (Tutoria)
// ------------------------------------------------------------------------
@Composable
fun HelpAndGroupsHub(
    helpRequests: List<HelpRequest>,
    studyGroups: List<StudyGroup>,
    subjects: List<Subject>,
    currentUserId: String,
    currentUserRole: String,
    activeGroupDetail: StudyGroup?,
    activeThreadDetail: GroupThread?,
    onGroupClick: (StudyGroup) -> Unit,
    onThreadClick: (GroupThread) -> Unit,
    onBackToHub: () -> Unit,
    onBackToGroup: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreateThread: (String, String) -> Unit,
    onAddThreadComment: (Int, String) -> Unit,
    onResolveHelp: (Int, Boolean) -> Unit,
    threadsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<GroupThread>>,
    threadCommentsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<GroupComment>>,
    onJoinVideoTutoria: (String, String) -> Unit = { _, _ -> },
    allRoles: List<UserRole> = emptyList(),
    profiles: List<Profile> = emptyList(),
    currentUserProfile: Profile? = null
) {
    if (activeGroupDetail != null) {
        // Nested View: Study Group Details View
        StudyGroupDetailsView(
            group = activeGroupDetail,
            activeThreadDetail = activeThreadDetail,
            currentUserId = currentUserId,
            currentUserRole = currentUserRole,
            onBackToGroup = onBackToGroup,
            onBackToHub = onBackToHub,
            onThreadClick = onThreadClick,
            onCreateThread = onCreateThread,
            onAddThreadComment = onAddThreadComment,
            threadsProvider = threadsProvider,
            threadCommentsProvider = threadCommentsProvider
        )
    } else {
        // Main split tab list hub: Left Questions, Right Study Groups
        var splitTab by remember { mutableStateOf("ajuda") } // ajuda, grupos

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 840.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(if (isWide) Modifier.widthIn(max = 840.dp).align(Alignment.TopCenter) else Modifier)
            ) {
            // Tab Header Switch
            TabRow(selectedTabIndex = if (splitTab == "ajuda") 0 else 1) {
                Tab(
                    selected = splitTab == "ajuda",
                    onClick = { splitTab = "ajuda" },
                    text = { Text("Mural de Ajuda") }
                )
                Tab(
                    selected = splitTab == "grupos",
                    onClick = { splitTab = "grupos" },
                    text = { Text("Grupos de Estudos") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (splitTab == "ajuda") {
                val context = LocalContext.current
                val staffRole = remember(allRoles, currentUserId) {
                    allRoles.find { it.user_id == currentUserId }
                }

                val finalHelpRequests = remember(helpRequests, currentUserRole, staffRole, subjects, profiles) {
                    if (currentUserRole == "aluno") {
                        helpRequests
                    } else {
                        val area = staffRole?.principal_area?.trim() ?: "Geral"
                        if (area == "Geral" || area.isEmpty() || area == "Pedagogia (UERJ)" || area == "Lic. Pedagogia/UERJ" || currentUserRole == "super_admin") {
                            helpRequests
                        } else {
                            helpRequests.filter { req ->
                                val subject = subjects.find { it.id == req.subject_id }
                                val subjectMatches = subject != null && (
                                    area.contains(subject.title, ignoreCase = true) || 
                                    subject.title.contains(area, ignoreCase = true)
                                )
                                val creator = profiles.find { it.id == req.author_id }
                                val periodMatches = creator != null && (
                                    area.contains(creator.periodo, ignoreCase = true) || 
                                    creator.periodo.contains(area, ignoreCase = true)
                                )
                                subjectMatches || periodMatches || req.author_id == currentUserId
                            }
                        }
                    }
                }

                // --------------------------------------------------------------------
                // LIVE TUTORING CARD (JITSI INFRAME) - Accessible to Period & Subject Staff
                // --------------------------------------------------------------------
                if (currentUserId == "jefersonribeiro199026@gmail.com") {
                    val isStaffUser = currentUserRole == "moderador" || currentUserRole == "super_admin"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VideoCall,
                                        contentDescription = "Live Tutoria",
                                        tint = UerjBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Monitoria Online ao Vivo (Jitsi Framework)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = UerjBlue
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(UerjGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "EM APP",
                                        color = UerjGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isStaffUser) {
                                // Staff tutoring manager controls
                                Text(
                                    text = "Preste tutoria ao vivo para as salas do seu período (${currentUserProfile?.periodo ?: "Geral"}). Escolha a matéria para iniciar a transmissão:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                var selectedSubjectForTutoria by remember { mutableStateOf(subjects.firstOrNull()) }
                                var showSubjectDropdown by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { showSubjectDropdown = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedSubjectForTutoria?.title ?: "Selecionar Disciplina",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Expandir")
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSubjectDropdown,
                                        onDismissRequest = { showSubjectDropdown = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        subjects.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub.title, style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    selectedSubjectForTutoria = sub
                                                    showSubjectDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val currentSubject = selectedSubjectForTutoria ?: subjects.firstOrNull()
                                        if (currentSubject != null) {
                                            val safeSub = currentSubject.title
                                                .replace(" ", "")
                                                .replace("ã", "a")
                                                .replace("é", "e")
                                                .replace("á", "a")
                                                .replace("õ", "o")
                                                .replace("í", "i")
                                                .replace("ç", "c")
                                            val safePeriod = (currentUserProfile?.periodo ?: "Geral")
                                                .replace(" ", "")
                                                .replace("º", "")
                                                .replace("ª", "")
                                            val jitsiRoomName = "Uerj_Tutoria_${safeSub}_${safePeriod}"
                                            val title = "Monitoria: ${currentSubject.title} [${currentUserProfile?.periodo ?: "Semestre"}]"
                                            onJoinVideoTutoria(jitsiRoomName, title)
                                        } else {
                                            Toast.makeText(context, "Nenhuma disciplina disponível.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = UerjBlue)
                                ) {
                                    Icon(Icons.Filled.VideoCameraFront, contentDescription = "Join")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir Sala de Atendimento (Tutor)")
                                }
                            } else {
                                // Student view - direct in-app access to assigned tutor room
                                val studentPeriodo = currentUserProfile?.periodo ?: "1º Período"
                                val studentMateria = currentUserProfile?.selected_materia ?: getDefaultSubjectForCourse(currentUserProfile?.curso ?: "Lic. Pedagogia/UERJ", studentPeriodo)

                                Text(
                                    text = "Tutoria disponível para você em $studentMateria [$studentPeriodo]. Clique abaixo para entrar no Plantão de Dúvidas ao vivo com seu tutor e tirar dúvidas no modelo inframe integrado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val safeSub = studentMateria
                                            .replace(" ", "")
                                            .replace("ã", "a")
                                            .replace("é", "e")
                                            .replace("á", "a")
                                            .replace("õ", "o")
                                            .replace("í", "i")
                                            .replace("ç", "c")
                                        val safePeriod = studentPeriodo
                                            .replace(" ", "")
                                            .replace("º", "")
                                            .replace("ª", "")
                                        val jitsiRoomName = "Uerj_Tutoria_${safeSub}_${safePeriod}"
                                        val title = "Sessão de Tutoria: $studentMateria [$studentPeriodo]"
                                        onJoinVideoTutoria(jitsiRoomName, title)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = UerjGreen)
                                ) {
                                    Icon(Icons.Filled.VideoCameraFront, contentDescription = "Join")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Entrar na Reunião de Tutoria")
                                }
                            }
                        }
                    }
                }

                // Pedir Ajuda mural
                Text("Dúvidas Acadêmicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val areaLabel = if (currentUserRole != "aluno") " (Mostrando: ${staffRole?.principal_area ?: "Geral"})" else ""
                Text("Dúvidas enviadas para disciplinas de Pedagogia$areaLabel.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                if (finalHelpRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma dúvida em aberto nesta categoria.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(finalHelpRequests) { req ->
                            val linkedSubject = subjects.find { it.id == req.subject_id }

                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Resolution badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (req.is_resolved) UerjGreen else UerjYellow,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (req.is_resolved) "RESOLVIDO" else "ABERTO",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Subject category name
                                        Text(
                                            text = linkedSubject?.title ?: "Pedagogia",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = UerjBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(req.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(req.description, style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Perguntado por: ${req.author_name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )

                                        // Only creator or mods can toggle resolution
                                        if (req.author_id == currentUserId || currentUserRole != "aluno") {
                                            TextButton(onClick = { onResolveHelp(req.id, !req.is_resolved) }) {
                                                Icon(
                                                    imageVector = if (req.is_resolved) Icons.Filled.LockOpen else Icons.Filled.Check,
                                                    contentDescription = "Status",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (req.is_resolved) "Reabrir" else "Resolvido", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Study groups list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grupos de Estudo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Entre em qualquer grupo para debater matérias.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Button(onClick = onCreateGroup) {
                        Text("Criar Grupo")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (studyGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Crie o primeiro grupo pedagógico!")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(studyGroups) { g ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGroupClick(g) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(g.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = UerjBlue)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(g.description, style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Grupo de Acesso Aberto", style = MaterialTheme.typography.labelSmall, color = UerjGreen)
                                        Icon(Icons.Filled.ArrowForward, contentDescription = "Ver Grupo")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ------------------------------------------------------------------------
// NESTED COMPONENT: Dynamic Study Group Threads / Forum Screen
// ------------------------------------------------------------------------
@Composable
fun StudyGroupDetailsView(
    group: StudyGroup,
    activeThreadDetail: GroupThread?,
    currentUserId: String,
    currentUserRole: String,
    onBackToGroup: () -> Unit,
    onBackToHub: () -> Unit,
    onThreadClick: (GroupThread) -> Unit,
    onCreateThread: (String, String) -> Unit,
    onAddThreadComment: (Int, String) -> Unit,
    threadsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<GroupThread>>,
    threadCommentsProvider: (Int) -> kotlinx.coroutines.flow.Flow<List<GroupComment>>
) {
    if (activeThreadDetail != null) {
        // Forum thread comment detailed list
        val comments by threadCommentsProvider(activeThreadDetail.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var newCommentText by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToGroup) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tópico de Discussão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Thread Card details
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(activeThreadDetail.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(activeThreadDetail.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Iniciado por: ${activeThreadDetail.author_name}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Comentários da Comunidade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(comments) { c ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(c.author_name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(c.content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Comment Box
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Comentar no tópico...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddThreadComment(activeThreadDetail.id, newCommentText)
                            newCommentText = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = UerjGreen)
                }
            }
        }
    } else {
        // Main list of Threads inside the Group
        val groupThreads by threadsProvider(group.id).collectAsStateWithLifecycle(initialValue = emptyList())
        var threadTitleInput by remember { mutableStateOf("") }
        var threadBodyInput by remember { mutableStateOf("") }
        var showNewThreadForm by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Group header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToHub) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = UerjBlue)
                        Text("Grupo de Discussão Coletivo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Button(onClick = { showNewThreadForm = !showNewThreadForm }) {
                    Text(if (showNewThreadForm) "Ocultar" else "Novo Tópico")
                }
            }

            if (showNewThreadForm) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Criar Novo Tópico de Discussão", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = threadTitleInput,
                            onValueChange = { threadTitleInput = it },
                            label = { Text("Título do Tópico") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = threadBodyInput,
                            onValueChange = { threadBodyInput = it },
                            label = { Text("Diga algo para iniciar a conversa...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (threadTitleInput.isNotBlank() && threadBodyInput.isNotBlank()) {
                                    onCreateThread(threadTitleInput, threadBodyInput)
                                    threadTitleInput = ""
                                    threadBodyInput = ""
                                    showNewThreadForm = false
                                }
                            }
                        ) {
                            Text("Postar Tópico")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Tópicos de Discussão Recentes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            if (groupThreads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum debate criado ainda neste grupo.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(groupThreads) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThreadClick(t) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(t.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(t.content, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Iniciado por: ${t.author_name}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SCREEN: Realtime Chat Screen
// ------------------------------------------------------------------------
@Composable
fun ChatRoomScreen(
    messages: List<Message>,
    currentUserId: String,
    currentUserProfile: Profile?,
    classmates: List<Profile>,
    onSendMessage: (String, String?) -> Unit,
    onTriggerEditProfile: () -> Unit
) {
    var txtMsg by remember { mutableStateOf("") }
    var mockImgUrl by remember { mutableStateOf("") }
    var showMediaAttachForm by remember { mutableStateOf(false) }
    
    // Toggle tab state: "chat" or "classmates"
    var activeSubTab by remember { mutableStateOf("chat") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Chat room banner
        Card(
            colors = CardDefaults.cardColors(containerColor = UerjGreen.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, UerjGreen.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏫 Sala Virtual: " + (currentUserProfile?.curso ?: "Sem Curso"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = UerjBlueDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Turma: ${currentUserProfile?.periodo ?: "Sem Período Letivo"}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = UerjGreen
                        )
                    }
                    Button(
                        onClick = onTriggerEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = UerjBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atualizar Período", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Esta sala é exclusiva para alunos do mesmo curso e período. Ao progredir de semestre, atualize seu período para liberar sua vaga para novos calouros na sala anterior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Sub-Tabs to toggle between Chat feed and Classmate views
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab 1: Chat room
            val isChatActive = activeSubTab == "chat"
            Button(
                onClick = { activeSubTab = "chat" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isChatActive) UerjBlue else Color.LightGray.copy(alpha = 0.4f),
                    contentColor = if (isChatActive) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bate-Papo da Sala", style = MaterialTheme.typography.labelMedium)
            }

            // Tab 2: Classmates List
            val isClassmatesActive = activeSubTab == "classmates"
            Button(
                onClick = { activeSubTab = "classmates" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClassmatesActive) UerjBlue else Color.LightGray.copy(alpha = 0.4f),
                    contentColor = if (isClassmatesActive) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Filled.PeopleAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Colegas na Sala (${classmates.size})", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == "chat") {
            // Render the messages chat unificado list
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "O silêncio reina por aqui... Seja o primeiro a puxar assunto!",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMyMsg = msg.author_id == currentUserId

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMyMsg) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isMyMsg) UerjBlue else Color.LightGray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMyMsg) 12.dp else 0.dp,
                                            bottomEnd = if (isMyMsg) 0.dp else 12.dp
                                        )
                                    )
                                    .padding(12.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Column {
                                    // Header name
                                    Text(
                                        text = "${msg.author_name} [${msg.author_role.uppercase()}]",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMyMsg) UerjYellow else UerjBlueDark,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Body text
                                    Text(
                                        text = msg.content,
                                        color = if (isMyMsg) Color.White else Color.Black,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Chat attached media
                                    if (!msg.media_url.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.DarkGray)
                                        ) {
                                            AsyncImage(
                                                model = msg.media_url,
                                                contentDescription = "Chat Attached Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showMediaAttachForm) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mockImgUrl,
                    onValueChange = { mockImgUrl = it },
                    label = { Text("Insira URL da foto para anexar...") },
                    placeholder = { Text("https://url.com/foto.png") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Input container row
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showMediaAttachForm = !showMediaAttachForm }) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Anexar Mídia",
                        tint = if (showMediaAttachForm) UerjBlue else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = txtMsg,
                    onValueChange = { txtMsg = it },
                    placeholder = { Text("Digite sua mensagem...") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (txtMsg.isNotBlank() || mockImgUrl.isNotBlank()) {
                            onSendMessage(txtMsg, mockImgUrl.ifBlank { null })
                            txtMsg = ""
                            mockImgUrl = ""
                            showMediaAttachForm = false
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = UerjBlue)
                }
            }
        } else {
            // Render the classmates list ("deixando a sala vazia...")
            if (classmates.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MeetingRoom,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sala Vazia!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Todos os alunos do período anterior já se formaram ou atualizaram seus períodos para avançar de sala! Esta sala está limpa para que os novos calouros ingressem sem ruídos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Alunos nesta Sala atualmente:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = UerjBlueDark,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    items(classmates) { classmate ->
                        val isSelf = classmate.id == currentUserId
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelf) UerjYellow.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isSelf) UerjYellow else Color.LightGray.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(UerjBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Psychology,
                                        contentDescription = null,
                                        tint = UerjBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = classmate.nome,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (isSelf) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(UerjYellow, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("VOCÊ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (classmate.bio.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = classmate.bio,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// SCREEN: Agenda / Calendário + Moderação Admin Panel
// ------------------------------------------------------------------------
@Composable
fun AgendaScreen(
    events: List<CalendarEvent>,
    currentUserRole: String,
    auditLogs: List<AuditLog>,
    profiles: List<Profile>,
    roles: Map<String, String>,
    onCreateEventTrigger: () -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onToggleModeratorRole: (String) -> Unit
) {
    var coreAgendaSubTab by remember { mutableStateOf("agenda") } // agenda, logs, papeis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (currentUserRole == "super_admin") {
            // Tab switch exclusive to Super Admins
            TabRow(
                selectedTabIndex = when (coreAgendaSubTab) {
                    "agenda" -> 0
                    "papeis" -> 1
                    else -> 2
                }
            ) {
                Tab(selected = coreAgendaSubTab == "agenda", onClick = { coreAgendaSubTab = "agenda" }, text = { Text("Agenda") })
                Tab(selected = coreAgendaSubTab == "papeis", onClick = { coreAgendaSubTab = "papeis" }, text = { Text("Moderação Papéis") })
                Tab(selected = coreAgendaSubTab == "logs", onClick = { coreAgendaSubTab = "logs" }, text = { Text("Audit Logs") })
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (coreAgendaSubTab) {
            "papeis" -> {
                // Roles list
                Text("Gestão de Moderadores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Super Admin pode promover/demover até 3 moderadores acadêmicos.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles) { p ->
                        val role = roles[p.id] ?: "aluno"

                        if (p.id != "jefersonribeiro199026@gmail.com") {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.nome, fontWeight = FontWeight.Bold)
                                        Text(p.id, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text("Cargo: ${role.uppercase()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = UerjBlue)
                                    }

                                    Button(
                                        onClick = { onToggleModeratorRole(p.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(if (role == "moderador") "Despromover" else "Promover")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "logs" -> {
                // Audit logs timeline view
                Text("Auditoria do Sistema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Rastreamento de ações moderativas para integridade da rede.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                if (auditLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Auditoria sem logs pendentes.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(auditLogs) { log ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.action.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = UerjGreen)
                                        Text(
                                            text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(log.target, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Responsável: ${log.moderator_name} (${log.moderator_id})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Standard calendar list view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Calendário de Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Agenda oficial de avaliações, prazos e eventos acadêmicos.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    if (currentUserRole == "super_admin") {
                        Button(onClick = onCreateEventTrigger) {
                            Text("Novo Aviso")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (events.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhuma data cadastrada na agenda.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(events) { ev ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (ev.category) {
                                        "avaliação" -> Color(0xFFFEE2E2)
                                        "aviso" -> Color(0xFFFEF9C3)
                                        else -> Color(0xFFD1FAE5)
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ev.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (ev.category) {
                                                "avaliação" -> Color.Red
                                                "aviso" -> Color(0xFF854D0E)
                                                else -> UerjGreen
                                            }
                                        )

                                        if (currentUserRole == "super_admin") {
                                            IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete Event", tint = Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ev.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = ev.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Data Limite: " + SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(Date(ev.date)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// COMPONENT: Embedded Jitsi WebRTC WebView (Inframe) Screen
// ------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JitsiInframeScreen(
    roomName: String,
    roomTitle: String,
    userName: String,
    userEmail: String,
    userAvatar: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cam = permissions[android.Manifest.permission.CAMERA] ?: false
        val mic = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (!cam || !mic) {
            Toast.makeText(context, "As permissões de câmera e áudio são necessárias para a tutoria por vídeo.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val camOk = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val micOk = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!camOk || !micOk) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    val cleanRoomName = remember(roomName) {
        roomName.trim()
            .replace(" ", "_")
            .replace("º", "")
            .replace("ª", "")
            .replace("-", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
    }
    
    val displayName = remember(userName) {
        try {
            java.net.URLEncoder.encode(userName, "UTF-8")
        } catch (e: Exception) {
            userName
        }
    }
    val displayEmail = remember(userEmail) {
        try {
            java.net.URLEncoder.encode(userEmail, "UTF-8")
        } catch (e: Exception) {
            userEmail
        }
    }
    val displayAvatar = remember(userAvatar) { 
        try {
            if (userAvatar.startsWith("http")) java.net.URLEncoder.encode(userAvatar, "UTF-8") else ""
        } catch (e: Exception) {
            ""
        }
    }

    val jitsiUrl = remember(cleanRoomName, displayName, displayEmail, displayAvatar) {
        val hashBuilder = StringBuilder()
        hashBuilder.append("config.prejoinPageEnabled=false")
        hashBuilder.append("&userInfo.displayName=\"$displayName\"")
        if (displayEmail.isNotEmpty()) {
            hashBuilder.append("&userInfo.email=\"$displayEmail\"")
        }
        if (displayAvatar.isNotEmpty()) {
            hashBuilder.append("&avatarURL=\"$displayAvatar\"")
        }
        "https://meet.jit.si/$cleanRoomName#${hashBuilder.toString()}"
    }

    var hasLoadedUrl by remember(jitsiUrl) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = roomTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UerjBlue
                        )
                        Text(
                            text = "Acesso Vinculado: $userName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Sair do Vídeo")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(UerjGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, UerjGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Text(
                                "AO VIVO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = UerjGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    val hasCam = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    
                                    val grantedResources = mutableListOf<String>()
                                    for (res in request.resources) {
                                        if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE && hasCam) {
                                            grantedResources.add(res)
                                        } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE && hasMic) {
                                            grantedResources.add(res)
                                        } else if (res != PermissionRequest.RESOURCE_VIDEO_CAPTURE && res != PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                                            grantedResources.add(res)
                                        }
                                    }
                                    
                                    if (grantedResources.isNotEmpty()) {
                                        request.grant(grantedResources.toTypedArray())
                                    } else {
                                        request.deny()
                                    }
                                }
                            }
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                javaScriptCanOpenWindowsAutomatically = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                        }
                    } catch (t: Throwable) {
                        android.util.Log.e("JitsiInframeScreen", "Erro ao iniciar WebView: ${t.message}", t)
                        android.widget.TextView(ctx).apply {
                            text = "A videoconferência não está disponível neste dispositivo porque o Android System WebView não está instalado ou ativado."
                            textSize = 15f
                            gravity = android.view.Gravity.CENTER
                            setTextColor(android.graphics.Color.RED)
                            setPadding(32, 32, 32, 32)
                        }
                    }
                },
                update = { view ->
                    if (view is WebView && !hasLoadedUrl) {
                        hasLoadedUrl = true
                        try {
                            view.loadUrl(jitsiUrl)
                        } catch (t: Throwable) {
                            android.util.Log.e("JitsiInframeScreen", "Erro ao carregar URL: ${t.message}", t)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
