package com.coblax.examlock.ui.geofence

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.text.style.CharacterStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.Places

import java.util.Locale

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal data class MapSearchResult(
    val placeId: String,
    val title: String,
    val subtitle: String,
    val latLng: LatLng? = null
)

internal data class MapSearchLookupResult(
    val results: List<MapSearchResult>,
    val usedFallback: Boolean,
    val failure: Throwable? = null
)

internal enum class GeofenceMapType(val googleMapType: Int) {
    Default(GoogleMap.MAP_TYPE_NORMAL),
    Satellite(GoogleMap.MAP_TYPE_SATELLITE),
    Terrain(GoogleMap.MAP_TYPE_TERRAIN)
}

// Legacy Places init is intentionally kept to preserve existing editor/search behavior.
@Suppress("DEPRECATION")
internal fun initializePlacesLegacy(context: Context, mapsApiKey: String) {
    if (mapsApiKey.isBlank()) {
        return
    }
    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, mapsApiKey)
    }
    MapsInitializer.initialize(context.applicationContext)
}

@Suppress("DEPRECATION")
internal fun ensurePlacesSdkReady(
    context: Context,
    mapsApiKey: String
): PlacesClient? {
    if (mapsApiKey.isBlank()) {
        return null
    }
    return runCatching {
        initializePlacesLegacy(context, mapsApiKey)
        Places.createClient(context.applicationContext)
    }.getOrNull()
}

internal fun buildMapSearchTitle(prediction: AutocompletePrediction): String {
    return prediction.getPrimaryText(null as CharacterStyle?).toString().trim().ifBlank {
        prediction.getFullText(null as CharacterStyle?).toString().trim().ifBlank { "-" }
    }
}

internal fun buildMapSearchSubtitle(prediction: AutocompletePrediction): String {
    return prediction.getSecondaryText(null as CharacterStyle?).toString().trim()
}

internal suspend fun searchPlacesInline(
    placesClient: PlacesClient,
    query: String,
    sessionToken: AutocompleteSessionToken
): List<MapSearchResult> {
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery(query.trim())
        .setSessionToken(sessionToken)
        .build()
    val response = placesClient.findAutocompletePredictions(request).awaitPlacesTask()
    return response.autocompletePredictions.map { prediction ->
        MapSearchResult(
            placeId = prediction.placeId,
            title = buildMapSearchTitle(prediction),
            subtitle = buildMapSearchSubtitle(prediction)
        )
    }
}

internal fun buildGeocoderResultTitle(address: Address, index: Int): String {
    return sequenceOf(
        address.featureName,
        address.subLocality,
        address.thoroughfare,
        address.locality,
        address.subAdminArea,
        address.adminArea
    ).mapNotNull { value -> value?.trim()?.takeIf { it.isNotBlank() } }
        .firstOrNull()
        ?: "Result ${index + 1}"
}

internal fun buildGeocoderResultSubtitle(address: Address): String {
    val lineParts = buildList {
        val maxIndex = address.maxAddressLineIndex
        if (maxIndex >= 0) {
            for (index in 0..maxIndex) {
                address.getAddressLine(index)?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
    if (lineParts.isNotEmpty()) {
        return lineParts.joinToString(", ")
    }
    return sequenceOf(
        address.locality,
        address.subAdminArea,
        address.adminArea,
        address.countryName
    ).mapNotNull { value -> value?.trim()?.takeIf { it.isNotBlank() } }
        .distinct()
        .joinToString(", ")
}

internal fun Address.toMapSearchResult(index: Int): MapSearchResult {
    return MapSearchResult(
        placeId = "geocoder:$index:${latitude}:${longitude}",
        title = buildGeocoderResultTitle(this, index),
        subtitle = buildGeocoderResultSubtitle(this),
        latLng = LatLng(latitude, longitude)
    )
}

internal suspend fun geocoderSearchInline(
    context: Context,
    query: String,
    maxResults: Int = 5
): List<MapSearchResult> {
    if (!Geocoder.isPresent()) {
        return emptyList()
    }
    val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
    val trimmedQuery = query.trim()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                trimmedQuery,
                maxResults,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) {
                            continuation.resume(addresses.mapIndexed { index, address ->
                                address.toMapSearchResult(index)
                            })
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
                }
            )
        }
    } else {
        withContext(Dispatchers.IO) {
            legacyGeocoderLookup(geocoder, trimmedQuery, maxResults).mapIndexed { index, address ->
                address.toMapSearchResult(index)
            }
        }
    }
}

// Synchronous Geocoder fallback remains only for pre-Tiramisu devices.
@Suppress("DEPRECATION")
internal fun legacyGeocoderLookup(
    geocoder: Geocoder,
    query: String,
    maxResults: Int
): List<Address> {
    return runCatching {
        geocoder.getFromLocationName(query, maxResults).orEmpty()
    }.getOrDefault(emptyList())
}

internal suspend fun searchMapLocations(
    context: Context,
    placesClient: PlacesClient?,
    query: String,
    sessionToken: AutocompleteSessionToken
): MapSearchLookupResult {
    var placesFailure: Throwable? = null
    if (placesClient != null) {
        val placesResults = runCatching {
            searchPlacesInline(
                placesClient = placesClient,
                query = query,
                sessionToken = sessionToken
            )
        }.onFailure { throwable ->
            placesFailure = throwable
        }.getOrNull()
        if (!placesResults.isNullOrEmpty()) {
            return MapSearchLookupResult(
                results = placesResults,
                usedFallback = false,
                failure = null
            )
        }
    }

    val fallbackResults = runCatching {
        geocoderSearchInline(
            context = context,
            query = query
        )
    }.getOrDefault(emptyList())
    return MapSearchLookupResult(
        results = fallbackResults,
        usedFallback = fallbackResults.isNotEmpty(),
        failure = placesFailure
    )
}

internal fun mapSearchFailureMessage(
    throwable: Throwable?,
    defaultMessage: String,
    configMessage: String
): String {
    val normalizedMessage = throwable?.message?.trim().orEmpty()
    if (throwable is ApiException) {
        if (
            normalizedMessage.contains("REQUEST_DENIED", ignoreCase = true) ||
            normalizedMessage.contains("API key", ignoreCase = true) ||
            normalizedMessage.contains("billing", ignoreCase = true) ||
            normalizedMessage.contains("not authorized", ignoreCase = true)
        ) {
            return configMessage
        }
    }
    if (
        normalizedMessage.contains("REQUEST_DENIED", ignoreCase = true) ||
        normalizedMessage.contains("API key", ignoreCase = true) ||
        normalizedMessage.contains("billing", ignoreCase = true) ||
        normalizedMessage.contains("not authorized", ignoreCase = true)
    ) {
        return configMessage
    }
    return defaultMessage
}

internal suspend fun resolvePlaceSearchResult(
    placesClient: PlacesClient,
    result: MapSearchResult,
    sessionToken: AutocompleteSessionToken
): MapSearchResult {
    if (result.latLng != null || result.placeId.isBlank() || result.placeId.startsWith("geocoder:")) {
        return result
    }
    val request = FetchPlaceRequest.builder(
        result.placeId,
        listOf(
            Place.Field.LOCATION,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS
        )
    ).setSessionToken(sessionToken).build()
    val response = placesClient.fetchPlace(request).awaitPlacesTask()
    val place = response.place
    return result.copy(
        title = place.displayName?.trim().takeUnless { it.isNullOrBlank() } ?: result.title,
        subtitle = place.formattedAddress?.trim().takeUnless { it.isNullOrBlank() } ?: result.subtitle,
        latLng = place.location
    )
}

internal suspend fun <T> Task<T>.awaitPlacesTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) {
                continuation.resume(value)
            }
        }
        addOnFailureListener { throwable ->
            if (continuation.isActive) {
                continuation.resumeWithException(throwable)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.cancel()
            }
        }
    }
}
