package com.example.personalapp.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentWeatherResponse(
    val name: String?,
    val main: Main?,
    val weather: List<WeatherItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Main(val temp: Double?)

@JsonClass(generateAdapter = true)
data class WeatherItem(val description: String?, val icon: String?)
