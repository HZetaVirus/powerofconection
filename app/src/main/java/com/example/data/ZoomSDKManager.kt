package com.example.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import us.zoom.sdk.*

/**
 * Manager class for Zoom Meeting SDK integration.
 * Encapsulates initialization, Host Start, and Participant Join parameters.
 */
object ZoomSDKManager : ZoomSDKInitializeListener, InMeetingServiceListener {
    private const val TAG = "ZoomSDKManager"
    private var isInitialized = false
    private var initCallback: ((Boolean, String?) -> Unit)? = null

    /**
     * Initializes the Zoom Meeting SDK with the short-lived JWT.
     */
    fun inicializarSDK(context: Context, jwtToken: String, onComplete: (Boolean, String?) -> Unit) {
        if (isInitialized) {
            onComplete(true, "Zoom SDK já inicializado")
            return
        }

        val zoomSDK = ZoomSDK.getInstance()
        if (zoomSDK.isInitialized) {
            isInitialized = true
            onComplete(true, "Zoom SDK já estava ativo no processo")
            return
        }

        initCallback = onComplete

        val params = ZoomSDKInitParams().apply {
            this.jwtToken = jwtToken
            this.domain = "zoom.us"
            this.enableLog = true
        }

        try {
            zoomSDK.initialize(context, this, params)
        } catch (t: Throwable) {
            Log.e(TAG, "Erro fatal ao invocar inicialização do Zoom SDK: ${t.message}", t)
            onComplete(false, t.localizedMessage)
        }
    }

    override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
        if (errorCode == ZoomError.ZOOM_ERROR_SUCCESS) {
            Log.i(TAG, "Zoom Meeting SDK inicializado com sucesso!")
            isInitialized = true
            initCallback?.invoke(true, null)
            
            // Add meeting listener to handle lifecycle events
            try {
                ZoomSDK.getInstance().meetingService?.addListener(this)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao adicionar InMeetingServiceListener: ${e.message}")
            }
        } else {
            val errorMsg = "Erro na inicialização do Zoom SDK. Código: $errorCode (Internal: $internalErrorCode)"
            Log.e(TAG, errorMsg)
            isInitialized = false
            initCallback?.invoke(false, errorMsg)
        }
        initCallback = null
    }

    override fun onZoomAuthIdentityExpired() {
        Log.w(TAG, "A identidade/autenticação do SDK do Zoom expirou.")
        isInitialized = false
    }

    /**
     * Starts a videoconference as Host Admin using StartMeetingParamsWithoutLogin.
     */
    fun iniciarReuniaoComoAdmin(
        context: Context,
        meetingId: String,
        password: String,
        zakToken: String,
        displayName: String = "Docente Host"
    ): Boolean {
        if (!isInitialized) {
            Toast.makeText(context, "Erro: SDK do Zoom não foi inicializado ainda.", Toast.LENGTH_LONG).show()
            return false
        }

        val meetingService = ZoomSDK.getInstance().meetingService ?: return false
        if (meetingService.meetingStatus != MeetingStatus.MEETING_STATUS_IDLE) {
            meetingService.leaveCurrentMeeting(true)
        }

        val params = StartMeetingParamsWithoutLogin().apply {
            this.meetingNo = meetingId.trim().replace(" ", "")
            this.password = password
            this.displayName = displayName
            this.zoomToken_ZAK = zakToken
            this.userType = MeetingService.USER_TYPE_APIUser
        }

        val result = meetingService.startMeetingWithParams(context, params)
        Log.d(TAG, "Iniciar reunião como Admin resultado: $result")
        return result == MeetingError.MEETING_ERROR_SUCCESS
    }

    /**
     * Joins an existing videoconference as a regular student Participant.
     */
    fun ingressarReuniaoComoParticipante(
        context: Context,
        meetingId: String,
        password: String,
        displayName: String
    ): Boolean {
        if (!isInitialized) {
            Toast.makeText(context, "Erro: SDK do Zoom não foi inicializado ainda.", Toast.LENGTH_LONG).show()
            return false
        }

        val meetingService = ZoomSDK.getInstance().meetingService ?: return false
        if (meetingService.meetingStatus != MeetingStatus.MEETING_STATUS_IDLE) {
            meetingService.leaveCurrentMeeting(true)
        }

        val params = JoinMeetingParams().apply {
            this.meetingNo = meetingId.trim().replace(" ", "")
            this.password = password
            this.displayName = displayName
        }

        val result = meetingService.joinMeetingWithParams(context, params)
        Log.d(TAG, "Ingressar na reunião como Participante resultado: $result")
        return result == MeetingError.MEETING_ERROR_SUCCESS
    }

    /**
     * Leaves or finishes the current active meeting.
     */
    fun sairDaChamada(endMeeting: Boolean = false) {
        try {
            val meetingService = ZoomSDK.getInstance().meetingService
            if (meetingService != null && meetingService.meetingStatus != MeetingStatus.MEETING_STATUS_IDLE) {
                meetingService.leaveCurrentMeeting(endMeeting)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tentar sair da chamada: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------
    // InMeetingServiceListener Implementation
    // ------------------------------------------------------------------------
    override fun onMeetingStatusChanged(status: MeetingStatus?, errorCode: Int, internalErrorCode: Int) {
        Log.d(TAG, "Zoom status mudou: $status. Erro: $errorCode")
        if (status == MeetingStatus.MEETING_STATUS_FAILED || status == MeetingStatus.MEETING_STATUS_DISCONNECTING) {
            // Clean up and free hardware resources if needed
        }
    }

    override fun onMeetingParameterNotification(p0: MeetingParameter?) {}
    override fun onMeetingUserJoin(p0: MutableList<Long>?) {}
    override fun onMeetingUserLeave(p0: MutableList<Long>?) {}
    override fun onMeetingUserUpdate(p0: Long) {}
    override fun onMeetingHostChange(p0: Long) {}
    override fun onActiveVideoUserChanged(p0: Long) {}
    override fun onActiveSpeakerVideoUserChanged(p0: Long) {}
    override fun onSpotlightVideoUserChange(p0: Boolean) {}
    override fun onUserVideoStatusChanged(p0: Long, p1: VideoStatus?) {}
    override fun onUserAudioStatusChanged(p0: Long, p1: AudioStatus?) {}
    override fun onUserAudioTypeDoubleCheck(p0: Long) {}
    override fun onUserActiveAudioStatusChanged(p0: Long) {}
    override fun onUserAudioTypeDoubleCheckForAllUsers() {}
    override fun onSilentModeChanged(p0: Boolean) {}
    override fun onLowOrPowerStateMode(p0: Boolean) {}
    override fun onMeetingActiveVideo(p0: Long) {}
    override fun onSinkAttendeeChatPrivilegeChanged(p0: Int) {}
    override fun onSinkAllowAttendeeChatStateChanged(p0: Boolean) {}
    override fun onMicrophoneStatusError(p0: InMeetingAudioController.MobileRTCMicrophoneError?) {}
    override fun onJoinMeetingInfoReceived() {}
    override fun onMeetingCoHostChange(p0: Long, p1: Boolean) {}
    override fun onSinkMeetingUserLeft(p0: String?) {}
    override fun onWebinarAttendeeStatusChanged(p0: Long, p1: Boolean) {}
    override fun onMeetingUserJoin(p0: String?) {}
    override fun onMeetingUserLeave(p0: String?) {}
}
