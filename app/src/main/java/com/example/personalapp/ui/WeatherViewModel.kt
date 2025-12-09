package com.example.personalapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalapp.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val loading: Boolean = false,
    val temp: Double? = null,
    val description: String? = null,
    val icon: String? = null,
    val city: String? = null,
    val error: String? = null
)

class WeatherViewModel(private val repo: WeatherRepository) : ViewModel() {

    private val _state = MutableStateFlow(WeatherUiState(loading = true))
    val state: StateFlow<WeatherUiState> = _state

    fun loadCity(city: String) {
        viewModelScope.launch {
            _state.value = WeatherUiState(loading = true)
            repo.getWeatherByCity(city).fold(
                onSuccess = { body ->
                    val weather = body?.weather?.firstOrNull()
                    _state.value = WeatherUiState(
                        loading = false,
                        temp = body?.main?.temp,
                        description = weather?.description,
                        icon = weather?.icon,
                        city = body?.name
                    )
                },
                onFailure = { throwable ->
                    _state.value = WeatherUiState(
                        loading = false,
                        error = throwable.localizedMessage ?: "Erreur"
                    )
                }
            )
        }
    }


}
