package com.example.data

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object SupabaseSync {
    private const val TAG = "SupabaseSync"
    private const val PREFS_NAME = "supabase_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_KEY = "supabase_anon_key"

    val DEFAULT_URL: String = try {
        val url: String? = com.example.BuildConfig.SUPABASE_URL
        if (!url.isNullOrBlank()) url else "https://lqjaxkeumlmvdapocfab.supabase.co"
    } catch (e: Throwable) {
        "https://lqjaxkeumlmvdapocfab.supabase.co"
    }

    val DEFAULT_KEY: String = try {
        val key: String? = com.example.BuildConfig.SUPABASE_KEY
        if (!key.isNullOrBlank()) key else "sb_publishable_GCzmUNYSMxt3iSXREs-d2Q_APXqUN8L"
    } catch (e: Throwable) {
        "sb_publishable_GCzmUNYSMxt3iSXREs-d2Q_APXqUN8L"
    }

    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun getUrl(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URL, "") ?: ""
        return if (saved.isBlank()) DEFAULT_URL else saved
    }

    fun getKey(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_KEY, "") ?: ""
        return if (saved.isBlank()) DEFAULT_KEY else saved
    }

    private fun <T> parseArray(jsonStr: String, transformer: (JSONObject) -> T): List<T> {
        val list = mutableListOf<T>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(transformer(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no parsing do array JSON: ${e.message}")
        }
        return list
    }

    fun saveConfig(context: Context, url: String, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_KEY, key.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        return true // Always configured with our robust default fallback
    }

    // Generic HTTP network call helper
    private fun postToSupabase(context: Context, tableName: String, jsonPayload: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val url = getUrl(context)
        val key = getKey(context)
        if (url.isEmpty() || key.isEmpty()) {
            onComplete?.invoke(false, "Supabase não configurado")
            return
        }

        val endpoint = "${url.trimEnd('/')}/rest/v1/$tableName"
        
        Log.d(TAG, "Request to table $tableName endpoint: $endpoint")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates") // Standard Supabase upsert rule
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Falha na sincronização do Supabase: ${e.message}", e)
                onComplete?.invoke(false, e.localizedMessage)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "Sincronização bem sucedida: $tableName. Resposta: $body")
                    onComplete?.invoke(true, body)
                } else {
                    Log.e(TAG, "Código de erro Supabase: ${response.code}. Resposta: $body")
                    onComplete?.invoke(false, "Erro ${response.code}: $body")
                }
                response.close()
            }
        })
    }

    // Generic HTTP DELETE network call helper
    fun deleteFromSupabase(context: Context, tableName: String, idFieldName: String, idValue: Any, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val url = getUrl(context)
        val key = getKey(context)
        if (url.isEmpty() || key.isEmpty()) {
            onComplete?.invoke(false, "Supabase não configurado")
            return
        }

        val endpoint = "${url.trimEnd('/')}/rest/v1/$tableName?$idFieldName=eq.$idValue"
        
        Log.d(TAG, "Request to DELETE from table $tableName endpoint: $endpoint")

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Falha na exclusão do Supabase: ${e.message}", e)
                onComplete?.invoke(false, e.localizedMessage)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "Exclusão bem sucedida: $tableName. Resposta: $body")
                    onComplete?.invoke(true, body)
                } else {
                    Log.e(TAG, "Código de erro Supabase DELETE: ${response.code}. Resposta: $body")
                    onComplete?.invoke(false, "Erro ${response.code}: $body")
                }
                response.close()
            }
        })
    }

    // Synchronous HTTP GET fetch helper
    private suspend fun getFromSupabaseSync(context: Context, tableName: String, queryParams: String = ""): String? {
        val url = getUrl(context)
        val key = getKey(context)
        if (url.isEmpty() || key.isEmpty()) return null

        val endpoint = "${url.trimEnd('/')}/rest/v1/$tableName$queryParams"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else {
                        Log.e(TAG, "Error getting $tableName: ${response.code} - ${response.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed getting $tableName", e)
                null
            }
        }
    }

    // Sync individual models (Upsert)
    fun syncProfile(context: Context, profile: Profile, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                put("id", profile.id)
                put("nome", profile.nome)
                put("curso", profile.curso)
                put("periodo", profile.periodo)
                put("bio", profile.bio)
                put("foto_url", profile.foto_url)
                put("selected_materia", profile.selected_materia)
                put("created_at", profile.created_at)
            }
            postToSupabase(context, "profiles", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncRole(
        context: Context, 
        userId: String, 
        roleName: String, 
        permissions: String = "delete_posts,manage_events,moderate_chats", 
        principalArea: String = "Pedagogia (UERJ)",
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        try {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("role", roleName)
                put("permissions", permissions)
                put("principal_area", principalArea)
                put("online", true)
            }
            postToSupabase(context, "roles", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncPost(context: Context, post: Post, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (post.id != 0) put("id", post.id)
                put("author_id", post.author_id)
                put("author_name", post.author_name)
                put("author_avatar", post.author_avatar)
                put("subject_id", if (post.subject_id == null) JSONObject.NULL else post.subject_id)
                put("content", post.content)
                put("media_url", if (post.media_url == null) JSONObject.NULL else post.media_url)
                put("created_at", post.created_at)
                put("likes_count", post.likes_count)
            }
            postToSupabase(context, "posts", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncComment(context: Context, comment: Comment, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (comment.id != 0) put("id", comment.id)
                put("post_id", comment.post_id)
                put("author_id", comment.author_id)
                put("author_name", comment.author_name)
                put("author_avatar", comment.author_avatar)
                put("content", comment.content)
                put("created_at", comment.created_at)
            }
            postToSupabase(context, "comments", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncMindmap(context: Context, map: Mindmap, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (map.id != 0) put("id", map.id)
                put("title", map.title)
                put("description", map.description)
                put("image_url", map.image_url)
                put("author_id", map.author_id)
                put("author_name", map.author_name)
                put("tags", map.tags)
                put("created_at", map.created_at)
            }
            postToSupabase(context, "mindmaps", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncHelpRequest(context: Context, req: HelpRequest, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (req.id != 0) put("id", req.id)
                put("title", req.title)
                put("description", req.description)
                put("subject_id", req.subject_id)
                put("author_id", req.author_id)
                put("author_name", req.author_name)
                put("is_resolved", req.is_resolved)
                put("created_at", req.created_at)
            }
            postToSupabase(context, "help_requests", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncMessage(context: Context, m: Message, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (m.id != 0) put("id", m.id)
                put("content", m.content)
                put("media_url", if (m.media_url == null) JSONObject.NULL else m.media_url)
                put("author_id", m.author_id)
                put("author_name", m.author_name)
                put("author_role", m.author_role)
                put("room_tag", if (m.room_tag == null) JSONObject.NULL else m.room_tag)
                put("created_at", m.created_at)
            }
            postToSupabase(context, "messages", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncNotification(context: Context, n: Notification, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (n.id != 0) put("id", n.id)
                put("user_id", n.user_id)
                put("title", n.title)
                put("body", n.body)
                put("read", n.read)
                put("created_at", n.created_at)
            }
            postToSupabase(context, "notifications", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncStudyGroup(context: Context, g: StudyGroup, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (g.id != 0) put("id", g.id)
                put("name", g.name)
                put("description", g.description)
                put("created_by", g.created_by)
                put("created_at", g.created_at)
            }
            postToSupabase(context, "study_groups", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncGroupThread(context: Context, t: GroupThread, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (t.id != 0) put("id", t.id)
                put("group_id", t.group_id)
                put("title", t.title)
                put("content", t.content)
                put("author_id", t.author_id)
                put("author_name", t.author_name)
                put("created_at", t.created_at)
            }
            postToSupabase(context, "group_threads", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncGroupComment(context: Context, c: GroupComment, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (c.id != 0) put("id", c.id)
                put("thread_id", c.thread_id)
                put("content", c.content)
                put("author_id", c.author_id)
                put("author_name", c.author_name)
                put("created_at", c.created_at)
            }
            postToSupabase(context, "group_comments", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncCalendarEvent(context: Context, e: CalendarEvent, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (e.id != 0) put("id", e.id)
                put("title", e.title)
                put("description", e.description)
                put("date", e.date)
                put("category", e.category)
                put("created_by", e.created_by)
            }
            postToSupabase(context, "calendar_events", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncStudyMaterial(context: Context, m: StudyMaterial, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (m.id != 0) put("id", m.id)
                put("title", m.title)
                put("description", m.description)
                put("file_url", m.file_url)
                put("type", m.type)
                put("author_id", m.author_id)
                put("author_name", m.author_name)
                put("created_at", m.created_at)
            }
            postToSupabase(context, "study_materials", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    fun syncAuditLog(context: Context, l: AuditLog, onComplete: ((Boolean, String?) -> Unit)? = null) {
        try {
            val json = JSONObject().apply {
                if (l.id != 0) put("id", l.id)
                put("moderator_id", l.moderator_id)
                put("moderator_name", l.moderator_name)
                put("action", l.action)
                put("target", l.target)
                put("timestamp", l.timestamp)
            }
            postToSupabase(context, "audit_logs", json.toString(), onComplete)
        } catch (e: Exception) {
            onComplete?.invoke(false, e.message)
        }
    }

    // Pull database data from Supabase and mirror to Room
    suspend fun pullAllData(context: Context, dao: AppDao) {
        val url = getUrl(context)
        val key = getKey(context)
        if (url.isEmpty() || key.isEmpty()) return

        Log.d(TAG, "Iniciando sincronização de Supabase para Room...")

        // 1. Profiles
        getFromSupabaseSync(context, "profiles")?.let { json ->
            val list = parseArray(json) { op ->
                Profile(
                    id = op.getString("id"),
                    nome = op.getString("nome"),
                    curso = op.optString("curso", "Pedagogia (UERJ)"),
                    periodo = op.optString("periodo", "1º Período"),
                    bio = op.optString("bio", ""),
                    foto_url = op.optString("foto_url", "avatar_user_default"),
                    selected_materia = op.optString("selected_materia", ""),
                    created_at = op.optLong("created_at", System.currentTimeMillis())
                )
            }
            if (list.isNotEmpty()) dao.insertProfiles(list)
        }

        // 2. Roles
        getFromSupabaseSync(context, "roles")?.let { json ->
            val list = parseArray(json) { op ->
                UserRole(
                    user_id = op.getString("user_id"),
                    role = op.optString("role", "aluno"),
                    permissions = op.optString("permissions", "delete_posts,manage_events,moderate_chats"),
                    principal_area = op.optString("principal_area", "Pedagogia (UERJ)"),
                    online = op.optBoolean("online", false)
                )
            }
            if (list.isNotEmpty()) dao.insertRoles(list)
        }

        // 3. Subjects
        getFromSupabaseSync(context, "subjects")?.let { json ->
            val list = parseArray(json) { op ->
                Subject(
                    id = op.getInt("id"),
                    title = op.getString("title"),
                    description = op.optString("description", "")
                )
            }
            if (list.isNotEmpty()) dao.insertSubjects(list)
        }

        // 4. Posts
        getFromSupabaseSync(context, "posts")?.let { json ->
            val list = parseArray(json) { op ->
                Post(
                    id = op.getInt("id"),
                    author_id = op.getString("author_id"),
                    author_name = op.getString("author_name"),
                    author_avatar = op.optString("author_avatar", "avatar_user_default"),
                    subject_id = if (op.isNull("subject_id")) null else op.getInt("subject_id"),
                    content = op.getString("content"),
                    media_url = if (op.isNull("media_url")) null else op.getString("media_url"),
                    created_at = op.optLong("created_at", System.currentTimeMillis()),
                    likes_count = op.optInt("likes_count", 0)
                )
            }
            if (list.isNotEmpty()) dao.insertPosts(list)
        }

        // 5. Comments
        getFromSupabaseSync(context, "comments")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertComment(
                        Comment(
                            id = op.getInt("id"),
                            post_id = op.getInt("post_id"),
                            author_id = op.getString("author_id"),
                            author_name = op.getString("author_name"),
                            author_avatar = op.optString("author_avatar", "avatar_user_default"),
                            content = op.getString("content"),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar comments: ${e.message}")
            }
        }

        // 6. Mindmaps
        getFromSupabaseSync(context, "mindmaps")?.let { json ->
            val list = parseArray(json) { op ->
                Mindmap(
                    id = op.getInt("id"),
                    title = op.getString("title"),
                    description = op.optString("description", ""),
                    image_url = op.getString("image_url"),
                    author_id = op.getString("author_id"),
                    author_name = op.getString("author_name"),
                    tags = op.optString("tags", ""),
                    created_at = op.optLong("created_at", System.currentTimeMillis())
                )
            }
            if (list.isNotEmpty()) dao.insertMindmaps(list)
        }

        // 7. Help Requests
        getFromSupabaseSync(context, "help_requests")?.let { json ->
            val list = parseArray(json) { op ->
                HelpRequest(
                    id = op.getInt("id"),
                    title = op.getString("title"),
                    description = op.getString("description"),
                    subject_id = op.getInt("subject_id"),
                    author_id = op.getString("author_id"),
                    author_name = op.getString("author_name"),
                    is_resolved = op.optBoolean("is_resolved", false),
                    created_at = op.optLong("created_at", System.currentTimeMillis())
                )
            }
            if (list.isNotEmpty()) dao.insertHelpRequests(list)
        }

        // 8. Messages
        getFromSupabaseSync(context, "messages")?.let { json ->
            val list = parseArray(json) { op ->
                Message(
                    id = op.getInt("id"),
                    content = op.getString("content"),
                    media_url = if (op.isNull("media_url")) null else op.getString("media_url"),
                    author_id = op.getString("author_id"),
                    author_name = op.getString("author_name"),
                    author_role = op.optString("author_role", "aluno"),
                    room_tag = if (op.isNull("room_tag")) null else op.getString("room_tag"),
                    created_at = op.optLong("created_at", System.currentTimeMillis())
                )
            }
            if (list.isNotEmpty()) dao.insertMessages(list)
        }

        // 9. Notifications
        getFromSupabaseSync(context, "notifications")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertNotification(
                        Notification(
                            id = op.getInt("id"),
                            user_id = op.getString("user_id"),
                            title = op.getString("title"),
                            body = op.getString("body"),
                            read = op.optBoolean("read", false),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar notifications: ${e.message}")
            }
        }

        // 10. Study Groups
        getFromSupabaseSync(context, "study_groups")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertStudyGroup(
                        StudyGroup(
                            id = op.getInt("id"),
                            name = op.getString("name"),
                            description = op.optString("description", ""),
                            created_by = op.getString("created_by"),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar study_groups: ${e.message}")
            }
        }

        // 11. Group Threads
        getFromSupabaseSync(context, "group_threads")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertGroupThread(
                        GroupThread(
                            id = op.getInt("id"),
                            group_id = op.getInt("group_id"),
                            title = op.getString("title"),
                            content = op.getString("content"),
                            author_id = op.getString("author_id"),
                            author_name = op.getString("author_name"),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar group_threads: ${e.message}")
            }
        }

        // 12. Group Comments
        getFromSupabaseSync(context, "group_comments")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertGroupComment(
                        GroupComment(
                            id = op.getInt("id"),
                            thread_id = op.getInt("thread_id"),
                            content = op.getString("content"),
                            author_id = op.getString("author_id"),
                            author_name = op.getString("author_name"),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar group_comments: ${e.message}")
            }
        }

        // 13. Calendar Events
        getFromSupabaseSync(context, "calendar_events")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertCalendarEvent(
                        CalendarEvent(
                            id = op.getInt("id"),
                            title = op.getString("title"),
                            description = op.optString("description", ""),
                            date = op.getLong("date"),
                            category = op.getString("category"),
                            created_by = op.getString("created_by")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar calendar_events: ${e.message}")
            }
        }

        // 14. Study Materials
        getFromSupabaseSync(context, "study_materials")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertStudyMaterial(
                        StudyMaterial(
                            id = op.getInt("id"),
                            title = op.getString("title"),
                            description = op.optString("description", ""),
                            file_url = op.getString("file_url"),
                            type = op.getString("type"),
                            author_id = op.getString("author_id"),
                            author_name = op.getString("author_name"),
                            created_at = op.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar study_materials: ${e.message}")
            }
        }

        // 15. Audit Logs
        getFromSupabaseSync(context, "audit_logs")?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val op = arr.getJSONObject(i)
                    dao.insertAuditLog(
                        AuditLog(
                            id = op.getInt("id"),
                            moderator_id = op.getString("moderator_id"),
                            moderator_name = op.getString("moderator_name"),
                            action = op.getString("action"),
                            target = op.getString("target"),
                            timestamp = op.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar audit_logs: ${e.message}")
            }
        }
    }

    const val DATABASE_SCHEMA_SQL = """-- Habilita extensão de UUID caso queira migrar chaves futuras para UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. PROFILES (Perfis de Usuários)
CREATE TABLE IF NOT EXISTS "profiles" (
    "id" VARCHAR(255) PRIMARY KEY, -- Email do usuário / ID único
    "nome" VARCHAR(255) NOT NULL,
    "curso" VARCHAR(255) NOT NULL DEFAULT 'Pedagogia (UERJ)',
    "periodo" VARCHAR(100) NOT NULL,
    "bio" TEXT DEFAULT '',
    "foto_url" TEXT DEFAULT '',
    "selected_materia" VARCHAR(255) DEFAULT '',
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 2. ROLES (Papéis e Permissões Administrativas)
CREATE TABLE IF NOT EXISTS "roles" (
    "user_id" VARCHAR(255) PRIMARY KEY REFERENCES "profiles"("id") ON DELETE CASCADE,
    "role" VARCHAR(50) NOT NULL DEFAULT 'aluno', -- super_admin, moderador, aluno
    "permissions" TEXT DEFAULT 'delete_posts,manage_events,moderate_chats', -- custom csv permissions
    "principal_area" VARCHAR(255) DEFAULT 'Pedagogia (UERJ)',
    "online" BOOLEAN DEFAULT true
);

-- 3. SUBJECTS (Disciplinas / Matérias)
CREATE TABLE IF NOT EXISTS "subjects" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT ''
);

-- 4. POSTS (Feed Compartilhado)
CREATE TABLE IF NOT EXISTS "posts" (
    "id" SERIAL PRIMARY KEY,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_avatar" TEXT DEFAULT '',
    "subject_id" INT REFERENCES "subjects"("id") ON DELETE SET NULL,
    "content" TEXT NOT NULL,
    "media_url" TEXT DEFAULT NULL, -- Link para imagem ou anexo do Storage
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT,
    "likes_count" INT NOT NULL DEFAULT 0
);

-- 5. COMMENTS (Comentários nos posts do Feed)
CREATE TABLE IF NOT EXISTS "comments" (
    "id" SERIAL PRIMARY KEY,
    "post_id" INT NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_avatar" TEXT DEFAULT '',
    "content" TEXT NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 6. POST_LIKES (Controle de Likes únicos de posts)
CREATE TABLE IF NOT EXISTS "post_likes" (
    "post_id" INT NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    PRIMARY KEY ("post_id", "user_id")
);

-- 7. MINDMAPS (Mapas Mentais compartilhados pelos alunos)
CREATE TABLE IF NOT EXISTS "mindmaps" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "image_url" TEXT NOT NULL, -- URL do bucket 'mindmaps' do Storage
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "tags" VARCHAR(255) DEFAULT '', -- Separadas por vírgula
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 8. MINDMAP_FAVORITES (Controle de curtidas/favoritos em Mapas Mentais)
CREATE TABLE IF NOT EXISTS "mindmap_favorites" (
    "mindmap_id" INT NOT NULL REFERENCES "mindmaps"("id") ON DELETE CASCADE,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    PRIMARY KEY ("mindmap_id", "user_id")
);

-- 9. HELP_REQUESTS (Dúvidas Acadêmicas / Mural 'Preciso de Ajuda')
CREATE TABLE IF NOT EXISTS "help_requests" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT NOT NULL,
    "subject_id" INT NOT NULL REFERENCES "subjects"("id") ON DELETE CASCADE,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "is_resolved" BOOLEAN NOT NULL DEFAULT false,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 10. MESSAGES (Mensagens do Chat Colaborativo)
CREATE TABLE IF NOT EXISTS "messages" (
    "id" SERIAL PRIMARY KEY,
    "content" TEXT NOT NULL,
    "media_url" TEXT DEFAULT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "author_role" VARCHAR(100) NOT NULL DEFAULT 'aluno', -- Administrador, Moderador, Aluno
    "room_tag" VARCHAR(100) DEFAULT NULL, -- Tag do canal de chat (ex: 'geral', 'psicologia')
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 11. NOTIFICATIONS (Notificações aos usuários)
CREATE TABLE IF NOT EXISTS "notifications" (
    "id" SERIAL PRIMARY KEY,
    "user_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "title" VARCHAR(255) NOT NULL,
    "body" TEXT NOT NULL,
    "read" BOOLEAN NOT NULL DEFAULT false,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 12. STUDY_GROUPS (Grupos de Estudo)
CREATE TABLE IF NOT EXISTS "study_groups" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "created_by" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 13. GROUP_THREADS (Fóruns/Discussões internos de um Grupo de Estudo)
CREATE TABLE IF NOT EXISTS "group_threads" (
    "id" SERIAL PRIMARY KEY,
    "group_id" INT NOT NULL REFERENCES "study_groups"("id") ON DELETE CASCADE,
    "title" VARCHAR(255) NOT NULL,
    "content" TEXT NOT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 14. GROUP_COMMENTS (Comentários nos tópicos de discussão do fórum)
CREATE TABLE IF NOT EXISTS "group_comments" (
    "id" SERIAL PRIMARY KEY,
    "thread_id" INT NOT NULL REFERENCES "group_threads"("id") ON DELETE CASCADE,
    "content" TEXT NOT NULL,
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 15. CALENDAR_EVENTS (Agenda Acadêmica compartilhada / Cronograma)
CREATE TABLE IF NOT EXISTS "calendar_events" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "date" BIGINT NOT NULL, -- Timestamp da data do evento
    "category" VARCHAR(100) NOT NULL, -- 'avaliação', 'aviso', 'evento'
    "created_by" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE
);

-- 16. AUDIT_LOGS (Log de ações administrativas/moderações)
CREATE TABLE IF NOT EXISTS "audit_logs" (
    "id" SERIAL PRIMARY KEY,
    "moderator_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "moderator_name" VARCHAR(255) NOT NULL,
    "action" VARCHAR(255) NOT NULL, -- ex: "Post Deletado", "Evento Criado", "Material Excluído"
    "target" TEXT NOT NULL,
    "timestamp" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);

-- 17. STUDY_MATERIALS (Materiais de Apoio - Biblioteca)
CREATE TABLE IF NOT EXISTS "study_materials" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "description" TEXT DEFAULT '',
    "file_url" TEXT NOT NULL, -- URL do arquivo no bucket 'materials'
    "type" VARCHAR(100) NOT NULL DEFAULT 'material', -- 'material', 'prova', 'audiobook'
    "author_id" VARCHAR(255) NOT NULL REFERENCES "profiles"("id") ON DELETE CASCADE,
    "author_name" VARCHAR(255) NOT NULL,
    "created_at" BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);"""
}

