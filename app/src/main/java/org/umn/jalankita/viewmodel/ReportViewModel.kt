package org.umn.jalankita.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.umn.jalankita.data.location.LocationException
import org.umn.jalankita.data.location.LocationFailure
import org.umn.jalankita.data.location.LocationPoint
import org.umn.jalankita.data.location.LocationProvider
import org.umn.jalankita.data.media.PhotoStorage
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.util.ConnectivityMonitor
import org.umn.jalankita.util.toUserMessage
import java.util.UUID

sealed interface LocationUiStatus {
    data object Idle : LocationUiStatus
    data object Loading : LocationUiStatus
    data object Ready : LocationUiStatus
    data class Failed(val failure: LocationFailure) : LocationUiStatus
}

data class ReportDraftState(
    val reportId: String = UUID.randomUUID().toString(),
    val photoPath: String? = null,
    val category: DamageCategory? = null,
    val description: String = "",
    val location: LocationPoint? = null,
    val streetName: String? = null,
    val locationStatus: LocationUiStatus = LocationUiStatus.Idle,
    val isResolvingStreet: Boolean = false,
    val isOnline: Boolean = true,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val validationError: String? = null,
) {
    val canSubmit: Boolean
        get() = photoPath != null && category != null && location != null && !isSubmitting

    /** Why the submit button is disabled, phrased for the user. */
    val missingRequirement: String?
        get() = when {
            location == null -> "Lokasi belum terdeteksi."
            photoPath == null -> "Foto kerusakan wajib diambil."
            category == null -> "Kategori kerusakan wajib dipilih."
            else -> null
        }

    /**
     * What to show next to the disabled submit button.
     *
     * While the fix is still being acquired the location section already says so;
     * repeating "Lokasi belum terdeteksi." there reads as a failure rather than
     * as work in progress.
     */
    val blockingHint: String?
        get() {
            validationError?.let { return it }
            val stillLocating = location == null &&
                (locationStatus == LocationUiStatus.Idle || locationStatus == LocationUiStatus.Loading)
            return if (stillLocating) null else missingRequirement
        }
}

class ReportViewModel(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val locationProvider: LocationProvider,
    val photoStorage: PhotoStorage,
    private val networkMonitor: ConnectivityMonitor,
) : ViewModel() {

    private val _draft = MutableStateFlow(ReportDraftState())
    val draft: StateFlow<ReportDraftState> = _draft.asStateFlow()

    private val _submitted = MutableStateFlow<Boolean?>(null)

    /** Non-null once the report is stored; true when it also reached the server. */
    val submittedSynced: StateFlow<Boolean?> = _submitted.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _draft.value = _draft.value.copy(isOnline = online)
            }
        }
    }

    // --- Location ---

    fun requestLocation() {
        if (_draft.value.locationStatus == LocationUiStatus.Loading) return
        _draft.value = _draft.value.copy(locationStatus = LocationUiStatus.Loading)

        viewModelScope.launch {
            locationProvider.getCurrentLocation()
                .onSuccess { point ->
                    _draft.value = _draft.value.copy(
                        location = point,
                        locationStatus = LocationUiStatus.Ready,
                    )
                    resolveStreetName(point)
                }
                .onFailure { error ->
                    val failure = (error as? LocationException)?.failure ?: LocationFailure.Unavailable
                    _draft.value = _draft.value.copy(locationStatus = LocationUiStatus.Failed(failure))
                }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestLocation()
        } else {
            _draft.value = _draft.value.copy(
                locationStatus = LocationUiStatus.Failed(LocationFailure.PermissionDenied),
            )
        }
    }

    fun setManualLocation(latitude: Double, longitude: Double) {
        val point = LocationPoint(latitude, longitude, isManual = true)
        _draft.value = _draft.value.copy(
            location = point,
            streetName = null,
            locationStatus = LocationUiStatus.Ready,
        )
        viewModelScope.launch { resolveStreetName(point) }
    }

    /**
     * Street names need the network. Failing here is normal and must never block
     * the report — the coordinates are already good enough.
     */
    private suspend fun resolveStreetName(point: LocationPoint) {
        if (!networkMonitor.currentlyOnline()) return
        _draft.value = _draft.value.copy(isResolvingStreet = true)
        val street = locationProvider.reverseGeocode(point.latitude, point.longitude)
        _draft.value = _draft.value.copy(streetName = street, isResolvingStreet = false)
    }

    // --- Form ---

    fun setPhoto(path: String) {
        val previous = _draft.value.photoPath
        if (previous != null && previous != path) photoStorage.delete(previous)
        _draft.value = _draft.value.copy(photoPath = path, validationError = null)
    }

    fun setCategory(category: DamageCategory) {
        _draft.value = _draft.value.copy(category = category, validationError = null)
    }

    fun setDescription(description: String) {
        _draft.value = _draft.value.copy(description = description)
    }

    fun clearError() {
        _draft.value = _draft.value.copy(submitError = null, validationError = null)
    }

    /** Called from the confirm screen's "Edit". */
    fun validateForConfirmation(): Boolean {
        val missing = _draft.value.missingRequirement
        _draft.value = _draft.value.copy(validationError = missing)
        return missing == null
    }

    // --- Submit ---

    fun submit() {
        val state = _draft.value
        if (state.isSubmitting) return

        val missing = state.missingRequirement
        if (missing != null) {
            _draft.value = state.copy(validationError = missing)
            return
        }

        _draft.value = state.copy(isSubmitting = true, submitError = null)
        viewModelScope.launch {
            val user = authRepository.authState
                .map { (it as? AuthState.SignedIn)?.user }
                .first()

            if (user == null) {
                _draft.value = _draft.value.copy(
                    isSubmitting = false,
                    submitError = "Sesi berakhir. Silakan masuk kembali.",
                )
                return@launch
            }

            runCatching {
                reportRepository.submitReport(
                    reportId = state.reportId,
                    userId = user.id,
                    reporterName = user.name,
                    photoPath = state.photoPath,
                    category = state.category!!,
                    description = state.description,
                    latitude = state.location!!.latitude,
                    longitude = state.location.longitude,
                    streetName = state.streetName,
                )
            }.onSuccess { outcome ->
                _draft.value = _draft.value.copy(isSubmitting = false)
                _submitted.value = outcome.synced
            }.onFailure { error ->
                _draft.value = _draft.value.copy(
                    isSubmitting = false,
                    submitError = error.toUserMessage("Laporan gagal disimpan. Silakan coba lagi."),
                )
            }
        }
    }

    /** Drops an abandoned draft's photo so it does not linger in app storage. */
    fun discardDraft() {
        if (_submitted.value == null) photoStorage.delete(_draft.value.photoPath)
    }
}
