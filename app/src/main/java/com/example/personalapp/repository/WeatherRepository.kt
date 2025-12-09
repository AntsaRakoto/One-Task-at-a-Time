package com.example.personalapp.repository

import com.example.personalapp.models.CurrentWeatherResponse
import com.example.personalapp.network.WeatherService

class WeatherRepository(private val service: WeatherService, private val apiKey: String) {

    suspend fun getWeatherByCity(city: String): Result<CurrentWeatherResponse> {
        return try {
            val resp = service.getCurrentByCity(city, "metric", apiKey)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
