package cu.uci.cisol.monitoreoambiental.ui.atencion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.Organization
import cu.uci.cisol.monitoreoambiental.domain.repository.OrganizationRepository
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AtencionDirectaUiState(
    val organizations: List<Organization> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val syncError: String? = null,
)

class AtencionDirectaViewModel(
    private val organizationRepository: OrganizationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AtencionDirectaUiState())
    val uiState: StateFlow<AtencionDirectaUiState> = _uiState.asStateFlow()

    init {
        observeOrganizations()
        syncOrganizationsInBackground()
    }

    private fun observeOrganizations() {
        viewModelScope.launch {
            organizationRepository.observeOrganizations()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { organizations ->
                    _uiState.value = _uiState.value.copy(
                        organizations = organizations,
                        isLoading = false,
                    )
                }
        }
    }

    private fun syncOrganizationsInBackground() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null)
            when (val result = organizationRepository.syncOrganizations()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = "No se pudo actualizar la lista. Mostrando datos guardados."
                    )
                }
            }
        }
    }

    fun refresh() {
        syncOrganizationsInBackground()
    }
}
