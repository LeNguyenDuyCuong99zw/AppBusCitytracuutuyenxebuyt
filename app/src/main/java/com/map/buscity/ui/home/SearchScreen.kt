package com.map.buscity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.withContext
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory
import com.map.buscity.data.sample.SampleBusStopData
import com.map.buscity.data.BusStop
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
// single NavController import above
import com.map.buscity.R

private enum class SuggestionKind { STOP, PLACE, ADDRESS, ROUTE }

private data class SuggestionItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val kind: SuggestionKind,
    val lat: Double? = null,
    val lng: Double? = null,
    // optional POI type/category reported by MapTiler/OSM (e.g. "education", "office", "restaurant")
    val poiType: String? = null,
    // source of suggestion: "maptiler", "opencage", "local" etc.
    val source: String? = null
)

// Helper: call MapTiler Geocoding API ("/geocoding/") to fetch autocomplete suggestions.
// Requires API key in resources `R.string.maptiler_api_key` or absent => function returns empty list.
/**
 * Fetch MapTiler suggestions restricted by country (default "VN") and optional HCM-only bbox.
 * @param countryCode ISO country code filter (e.g. "VN"). If null, do not filter by country.
 * @param hcmOnly if true, further restrict results to a bounding box roughly covering Vietnam.
 */
private suspend fun fetchMapTilerSuggestions(
    context: android.content.Context,
    q: String,
    limit: Int = 40,
    countryCode: String? = "VN",
    hcmOnly: Boolean = false,
    // optional proximity to bias results: Pair(lon, lat)
    proximity: Pair<Double, Double>? = null
): List<SuggestionItem> {
    if (q.isBlank()) return emptyList()

    // Try to get API key from resources: string name `maptiler_api_key`
    val resId = context.resources.getIdentifier("maptiler_api_key", "string", context.packageName)
    val apiKey = if (resId != 0) context.getString(resId).takeUnless { it.isBlank() } else null
    if (apiKey.isNullOrBlank()) return emptyList()

    return try {
        val urlQuery = java.net.URLEncoder.encode(q, "UTF-8")
        // add server-side filters to reduce payload: use boundary.country and autocomplete
        val base = StringBuilder("https://api.maptiler.com/geocoding/$urlQuery.json?key=$apiKey&limit=$limit&lang=vi&autocomplete=true")
        if (!countryCode.isNullOrBlank()) {
            // prefer MapTiler's boundary.country param when available
            base.append("&boundary.country=${countryCode}")
        }
        // if hcmOnly requested, add a bbox around Ho Chi Minh City to restrict server results
        if (hcmOnly) {
            // more precise HCMC bbox (approximate administrative boundary)
            // values chosen to reasonably cover the city area while excluding distant provinces
            // Format: minLon,minLat,maxLon,maxLat
            val hcmMinLon = 106.4500
            val hcmMinLat = 10.5060
            val hcmMaxLon = 106.9420
            val hcmMaxLat = 10.9600
            base.append("&bbox=$hcmMinLon,$hcmMinLat,$hcmMaxLon,$hcmMaxLat")
        }
        // add proximity if provided (format: lon,lat)
        if (proximity != null) {
            val (lon, lat) = proximity
            base.append("&proximity=$lon,$lat")
        }
        val url = base.toString()

        val client = okhttp3.OkHttpClient()
        val req = okhttp3.Request.Builder().url(url).get().build()

        val resp = withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(req).execute()
        }

        if (!resp.isSuccessful) return emptyList()

        val body = resp.body?.string().orEmpty()
        val root = org.json.JSONObject(body)
        val feats = root.optJSONArray("features") ?: return emptyList()

        val rawItems = mutableListOf<Pair<SuggestionItem, org.json.JSONObject?>>()
        // approximate Vietnam bbox: lat 8.0..23.5, lon 102.0..109.6
        // This covers mainland Vietnam from the southern islands up to the northern border.
        val vnMinLat = 8.0
        val vnMaxLat = 23.5
        val vnMinLon = 102.0
        val vnMaxLon = 109.6

        for (i in 0 until feats.length()) {
            val f = feats.optJSONObject(i) ?: continue
            val id = f.optString("id", "mt_$i")
            val props = f.optJSONObject("properties")
            // prefer the human-readable label from properties when available
            val text = props?.optString("label")?.takeIf { it.isNotBlank() } ?: f.optString("text", "")
            val placeName = f.optString("place_name", "")
            // Extract coordinates robustly: try geometry.coordinates, then top-level center, then properties fallback
            var lat: Double? = null
            var lng: Double? = null
            try {
                val geomObj = f.opt("geometry")
                if (geomObj is org.json.JSONObject) {
                    val coords = geomObj.optJSONArray("coordinates")
                    if (coords != null && coords.length() >= 2) {
                        lng = coords.optDouble(0)
                        lat = coords.optDouble(1)
                    }
                }

                // If geometry didn't contain coords, MapTiler sometimes provides a top-level "center" array [lon,lat]
                if (lat == null || lng == null) {
                    val center = f.optJSONArray("center")
                    if (center != null && center.length() >= 2) {
                        lng = center.optDouble(0)
                        lat = center.optDouble(1)
                    }
                }

                // Fallback: some responses may include lat/lon in properties (rare) or different keys
                if (lat == null || lng == null) {
                    val maybeLat = props?.optDouble("lat", Double.NaN) ?: Double.NaN
                    val maybeLng = props?.optDouble("lon", Double.NaN) ?: Double.NaN
                    if (!maybeLat.isNaN() && !maybeLng.isNaN()) {
                        lat = maybeLat
                        lng = maybeLng
                    }
                }
            } catch (_: Exception) {
                // ignore parsing errors and leave lat/lng as null
            }

            // Filter by countryCode if provided (MapTiler properties/context often include country info)
            // Try properties.country_code, properties.country, or context[*].short_code
            var featureCountry: String? = props?.optString("country_code")?.ifBlank { null } ?: props?.optString("country")?.ifBlank { null }
            if (featureCountry.isNullOrBlank()) {
                val ctx = f.optJSONArray("context")
                if (ctx != null) {
                    for (ci in 0 until ctx.length()) {
                        val cobj = ctx.optJSONObject(ci) ?: continue
                        val short = cobj.optString("short_code")
                        if (short.isNotBlank()) {
                            // short_code often like "vn" or "vn:xx" - take first part
                            featureCountry = short.substringBefore(':')
                            break
                        }
                    }
                }
            }

            if (countryCode != null) {
                if (!featureCountry.isNullOrBlank()) {
                    if (!featureCountry.equals(countryCode, ignoreCase = true)) continue
                } else {
                    // if we couldn't detect country from feature, be conservative and allow it
                }
            }

            // If hcmOnly requested, require lat/lng inside the Vietnam bbox
            if (hcmOnly) {
                if (lat == null || lng == null) continue
                if (!(lat >= vnMinLat && lat <= vnMaxLat && lng >= vnMinLon && lng <= vnMaxLon)) continue
            }

            // keep the original properties object for later scoring
            // extract category/place_type if present so we can display/filter POI types
            val category = props?.optString("category")?.takeIf { it.isNotBlank() }
            var placeType: String? = null
            val ptArr = f.optJSONArray("place_type")
            if (ptArr != null && ptArr.length() > 0) {
                placeType = ptArr.optString(0)
            } else {
                placeType = f.optString("place_type", "").ifEmpty { null }
            }

            val kind = if (!category.isNullOrBlank() || !placeType.isNullOrBlank()) SuggestionKind.PLACE else SuggestionKind.ADDRESS
            // subtitle: show the full place_name/address but trim duplicate title prefix
            val subtitleRaw = if (placeName.isNotBlank()) placeName else null
            val subtitle = subtitleRaw?.let {
                if (it.startsWith(text, ignoreCase = true)) {
                    // remove title prefix and leading punctuation/space
                    it.substring(text.length).trimStart(' ', ',', '·', '-')
                } else it
            }

            // Normalize a raw poi/type token and map it to a friendly Vietnamese label
            val poiRaw = (category ?: placeType ?: props?.optString("type")?.ifBlank { null })?.lowercase()
            val poiLabel = when {
                poiRaw == null -> null
                poiRaw.contains("education") || poiRaw.contains("school") || poiRaw.contains("university") || poiRaw.contains("trường") -> "Trường học"
                poiRaw.contains("hospital") || poiRaw.contains("clinic") || poiRaw.contains("bệnh viện") -> "Bệnh viện"
                poiRaw.contains("restaurant") || poiRaw.contains("cafe") || poiRaw.contains("quán") || poiRaw.contains("nhà hàng") -> "Quán ăn / Nhà hàng"
                poiRaw.contains("supermarket") || poiRaw.contains("shop") || poiRaw.contains("retail") || poiRaw.contains("market") || poiRaw.contains("siêu thị") || poiRaw.contains("chợ") -> "Siêu thị / Chợ"
                poiRaw.contains("park") || poiRaw.contains("công viên") -> "Công viên"
                poiRaw.contains("building") || poiRaw.contains("apartment") || poiRaw.contains("chung cư") || poiRaw.contains("tòa nhà") -> "Tòa nhà / Chung cư"
                poiRaw.contains("office") || poiRaw.contains("company") || poiRaw.contains("văn phòng") || poiRaw.contains("cơ quan") -> "Cơ quan / Văn phòng"
                poiRaw.contains("administrative") || poiRaw.contains("government") -> "Địa điểm hành chính"
                poiRaw.contains("road") || poiRaw.contains("street") || poiRaw.contains("way") || poiRaw.contains("hẻm") -> "Đường / Phố / Hẻm"
                poiRaw.contains("bus_stop") || poiRaw.contains("stop") -> "Điểm dừng / Bến"
                else -> poiRaw.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            val typeLabel = (category ?: placeType)?.let { it.replace('_', ' ') } ?: ""
            val combinedSubtitle = listOfNotNull(subtitle, poiLabel?.let { "· $it" }, typeLabel.takeIf { it.isNotBlank() }).joinToString(" ")

            rawItems.add(SuggestionItem(id = "mt:$id", title = text, subtitle = combinedSubtitle, kind = kind, lat = lat, lng = lng, poiType = category ?: placeType, source = "maptiler") to props)
        }

        // Scoring / boosting: prefer items that look like POI (education/company/hospital/etc.)
        val boostKeywords = listOf("trường", "đại học", "fpt", "công ty", "office", "văn phòng", "bệnh viện", "clinic", "nhà hàng", "quán", "mall", "chợ", "siêu thị")

        val scored = rawItems.map { (item, props) ->
            var score = 0
            val textLower = (item.title + " " + (item.subtitle ?: "")).lowercase()

            // give strong boost for keyword matches in title/place_name (Vietnamese keywords)
            if (boostKeywords.any { textLower.contains(it) }) score += 60

            // try category property from MapTiler (often in English like 'education','office','hospital')
            val category = props?.optString("category")?.lowercase() ?: ""
            val ptype = (item.poiType ?: "").lowercase()
            val combinedType = (category + " " + ptype).trim()
            if (combinedType.contains("education") || combinedType.contains("school") || combinedType.contains("university") || combinedType.contains("trường") ) score += 60
            if (combinedType.contains("office") || combinedType.contains("company") || combinedType.contains("công ty") || combinedType.contains("văn phòng")) score += 50
            if (combinedType.contains("hospital") || combinedType.contains("clinic") || combinedType.contains("bệnh viện")) score += 50
            if (combinedType.contains("shop") || combinedType.contains("retail") || combinedType.contains("mall") || combinedType.contains("market") || combinedType.contains("siêu thị") || combinedType.contains("chợ")) score += 40
            if (combinedType.contains("restaurant") || combinedType.contains("quán") || combinedType.contains("nhà hàng")) score += 40
            if (combinedType.contains("park") || combinedType.contains("công viên")) score += 30
            if (combinedType.contains("building") || combinedType.contains("tòa nhà") || combinedType.contains("chung cư") || combinedType.contains("apartment")) score += 20

            // prefer results that have coordinates
            if (item.lat != null && item.lng != null) score += 10

            // small boost for proximity if present (the API already biases, this is extra)
            // final score used to sort; return pair
            item to score
        }

        val out = scored.sortedByDescending { it.second }.map { it.first }.distinctBy { it.id }.take(limit)
        out
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Fetch OpenCage geocoding suggestions as a fallback/extra source.
 * Expects string resource `opencage_api_key` if available; otherwise returns empty list.
 */
private suspend fun fetchOpenCageSuggestions(
    context: android.content.Context,
    q: String,
    limit: Int = 20,
    countryCode: String? = "VN",
    proximity: Pair<Double, Double>? = null
): List<SuggestionItem> {
    if (q.isBlank()) return emptyList()

    val resId = context.resources.getIdentifier("opencage_api_key", "string", context.packageName)
    val apiKey = if (resId != 0) context.getString(resId).takeUnless { it.isBlank() } else null
    if (apiKey.isNullOrBlank()) return emptyList()

    return try {
        val urlQuery = java.net.URLEncoder.encode(q, "UTF-8")
        val sb = StringBuilder("https://api.opencagedata.com/geocode/v1/json?q=$urlQuery&key=$apiKey&limit=$limit&language=vi")
        // OpenCage supports 'countrycode' param to restrict results
        if (!countryCode.isNullOrBlank()) sb.append("&countrycode=${countryCode.lowercase()}")
        // OpenCage supports proximity via 'proximity' (lon,lat) in some paid tiers; skip if not provided
        val url = sb.toString()

        val client = okhttp3.OkHttpClient()
        val req = okhttp3.Request.Builder().url(url).get().build()

        val resp = withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(req).execute()
        }

        if (!resp.isSuccessful) return emptyList()

        val body = resp.body?.string().orEmpty()
        val root = org.json.JSONObject(body)
        val results = root.optJSONArray("results") ?: return emptyList()

        val out = mutableListOf<SuggestionItem>()
        for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            val formatted = r.optString("formatted").ifBlank { null }
            val components = r.optJSONObject("components")
            val geometry = r.optJSONObject("geometry")
            val lat = geometry?.optDouble("lat")?.let { if (!it.isNaN()) it else null }
            val lng = geometry?.optDouble("lng")?.let { if (!it.isNaN()) it else null }

            val title = formatted ?: components?.optString("road") ?: components?.optString("city") ?: components?.optString("state") ?: ""
            if (title.isBlank()) continue

            // attempt to determine a small subtitle from components
            val subtitleParts = mutableListOf<String>()
            components?.optString("suburb")?.takeIf { it.isNotBlank() }?.let { subtitleParts.add(it) }
            components?.optString("city")?.takeIf { it.isNotBlank() }?.let { subtitleParts.add(it) }
            components?.optString("county")?.takeIf { it.isNotBlank() }?.let { subtitleParts.add(it) }

            val subtitle = if (subtitleParts.isEmpty()) null else subtitleParts.joinToString(", ")

            // determine kind (address vs place) naively
            val kind = if ((components?.optString("amenity") ?: "").isNotBlank() || (components?.optString("building") ?: "").isNotBlank()) SuggestionKind.PLACE else SuggestionKind.ADDRESS

            out.add(SuggestionItem(id = "oc:$i:${title.hashCode()}", title = title, subtitle = subtitle, kind = kind, lat = lat, lng = lng, source = "opencage"))
        }

        out
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Call MapTiler and OpenCage concurrently, merge, dedupe and score results.
 */
private suspend fun fetchCombinedSuggestions(
    context: android.content.Context,
    q: String,
    limit: Int = 40,
    countryCode: String? = "VN",
    hcmOnly: Boolean = false,
    proximity: Pair<Double, Double>? = null
): List<SuggestionItem> {
    if (q.isBlank()) return emptyList()

    return try {
        coroutineScope {
            val mtDeferred = async { fetchMapTilerSuggestions(context, q, limit = limit / 2, countryCode = countryCode, hcmOnly = hcmOnly, proximity = proximity) }
            val ocDeferred = async { fetchOpenCageSuggestions(context, q, limit = limit / 2, countryCode = countryCode, proximity = proximity) }

            val mt = try { mtDeferred.await() } catch (_: Exception) { emptyList() }
            val oc = try { ocDeferred.await() } catch (_: Exception) { emptyList() }

            // merge with improved dedupe: use coordinate proximity (haversine < 30m) and token overlap for title similarity
            val combined = mutableListOf<SuggestionItem>()

            fun haversineKm(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
                val R = 6371.0 // km
                val dLat = Math.toRadians(bLat - aLat)
                val dLon = Math.toRadians(bLng - aLng)
                val lat1 = Math.toRadians(aLat)
                val lat2 = Math.toRadians(bLat)
                val sinDlat = kotlin.math.sin(dLat / 2.0)
                val sinDlon = kotlin.math.sin(dLon / 2.0)
                val aa = sinDlat * sinDlat + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinDlon * sinDlon
                val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(aa), kotlin.math.sqrt(1 - aa))
                return R * c
            }

            fun tokenOverlap(a: String, b: String): Double {
                val atoks = a.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
                val btoks = b.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
                if (atoks.isEmpty() || btoks.isEmpty()) return 0.0
                val inter = atoks.intersect(btoks).size.toDouble()
                val denom = kotlin.math.min(atoks.size, btoks.size).toDouble()
                return inter / denom
            }

            fun isDuplicate(existing: SuggestionItem, candidate: SuggestionItem): Boolean {
                // if both have coords, use distance
                if (existing.lat != null && existing.lng != null && candidate.lat != null && candidate.lng != null) {
                    val dKm = haversineKm(existing.lat, existing.lng, candidate.lat, candidate.lng)
                    if (dKm <= 0.03) return true // <=30m
                }
                // token overlap on titles
                val overlap = tokenOverlap(existing.title, candidate.title)
                if (overlap >= 0.6) return true
                // fallback: identical ids
                if (existing.id.isNotBlank() && candidate.id.isNotBlank() && existing.id == candidate.id) return true
                return false
            }

            // prefer MapTiler items first (they often include poi/category info). When duplicates detected,
            // keep the one with coordinates or the MapTiler one.
            mt.forEach { m ->
                combined.add(m)
            }

            for (o in oc) {
                var merged = false
                for (i in combined.indices) {
                    val ex = combined[i]
                    if (isDuplicate(ex, o)) {
                        // decide which to keep: prefer one with coords, otherwise keep existing (MapTiler)
                        if ((ex.lat == null || ex.lng == null) && (o.lat != null && o.lng != null)) {
                            combined[i] = o
                        }
                        merged = true
                        break
                    }
                }
                if (!merged) combined.add(o)
            }

            // scoring: boost POIs and items with coords
            val boostKeywords = listOf("trường", "đại học", "công ty", "văn phòng", "bệnh viện", "quán", "nhà hàng", "mall", "chợ", "siêu thị")
            val scored = combined.map { item ->
                var score = 0
                val txt = (item.title + " " + (item.subtitle ?: "")).lowercase()
                if (boostKeywords.any { txt.contains(it) }) score += 60
                if (item.poiType != null) score += 30
                if (item.lat != null && item.lng != null) score += 20
                item to score
            }

            scored.sortedByDescending { it.second }.map { it.first }.distinctBy { it.id }.take(limit)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val query = rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // load sample stops to serve as suggestion source
    val allStops = remember { SampleBusStopData.getSampleStops() }

    // SharedPreferences for simple history (comma separated)
    val prefs: SharedPreferences = context.getSharedPreferences("search_prefs", 0)
    fun saveHistoryItem(item: String) {
        val existing = prefs.getString("history", "") ?: ""
        val parts = if (existing.isBlank()) mutableListOf() else existing.split("||").toMutableList()
        // keep unique and recent-first
        parts.remove(item)
        parts.add(0, item)
        if (parts.size > 20) parts.subList(20, parts.size).clear()
        prefs.edit().putString("history", parts.joinToString("||")).apply()
    }

    fun loadHistory(): List<String> {
        val raw = prefs.getString("history", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("||")
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top row: back + search box (search box styled as white rounded surface with shadow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Search text field with leading search icon and trailing clear button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.padding(start = 6.dp).size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = query.value,
                                    onValueChange = { query.value = it },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF263238))
                                )

                                if (query.value.isEmpty()) {
                                    Text(text = "Tìm kiếm địa điểm", color = Color(0xFF9E9E9E), fontSize = 16.sp)
                                }
                            }

                            if (query.value.isNotBlank()) {
                                IconButton(onClick = { query.value = "" }, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF7D8A94))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chips row
            var nearMe by remember { mutableStateOf(false) }
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StyledChip(icon = Icons.Filled.Home, label = "Nhà")
                StyledChip(icon = Icons.Filled.Work, label = "Cơ quan")
                StyledChip(icon = Icons.Filled.School, label = "Trường")
                StyledChip(icon = Icons.Filled.Add, label = "+")

                // Nearby toggle: when active, bias MapTiler suggestions around HCMC center
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (nearMe) Color(0xFFD9F3E6) else Color(0xFFF7F7F7),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .height(36.dp)
                        .wrapContentWidth()
                        .clickable { nearMe = !nearMe }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = if (nearMe) Color(0xFF2E7148) else Color(0xFF7D8A94), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (nearMe) "Xung quanh: Bật" else "Xung quanh", color = if (nearMe) Color(0xFF2E7148) else Color(0xFF5B6B70))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map picker card
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { /* open map picker */ }
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Place, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Chọn trên bản đồ", fontWeight = FontWeight.SemiBold, color = Color(0xFF263238))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggestion / History area
            val trimmed = query.value.trim()
            if (trimmed.isEmpty()) {
                // show history
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFD9F3E6), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(start = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text(text = "Lịch sử tìm kiếm", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7148), fontSize = 16.sp)
                }

                val history = loadHistory()
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                ) {
                    if (history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Chưa có lịch sử tìm kiếm", color = Color(0xFF9E9E9E))
                        }
                    } else {
                        history.forEachIndexed { idx, item ->
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query.value = item
                                }
                                .padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF222222), modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item, fontWeight = FontWeight.SemiBold, color = Color(0xFF263238), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                                }
                            }

                            if (idx < history.lastIndex) Divider(color = Color(0xFFEEF0F1), thickness = 0.8.dp)
                        }
                    }
                }
            } else {
                // show live suggestions filtered from sample stops plus places/addresses/routes
                var remoteSuggestions by remember { mutableStateOf<List<SuggestionItem>>(emptyList()) }

                // Launch a coroutine to fetch from MapTiler when query length >= 2
                // Fetch remote suggestions (MapTiler) for Vietnam only. To restrict further to HCM,
                // set hcmOnly = true below.
                LaunchedEffect(trimmed, nearMe) {
                    if (trimmed.length >= 2) {
                        // debounce to avoid hammering APIs while user types
                        isLoading = true
                        delay(300)
                        // If query changed during debounce, skip
                        if (trimmed != query.value.trim()) {
                            isLoading = false
                            return@LaunchedEffect
                        }

                        try {
                            // If nearMe is enabled, bias results toward HCMC center (approx lon,lat)
                            val hcmCenter = if (nearMe) Pair(106.6297, 10.8231) else null
                            // Use combined suggestions from MapTiler + OpenCage for broader coverage
                            val fetched = fetchCombinedSuggestions(context, trimmed, limit = 60, countryCode = "VN", hcmOnly = nearMe, proximity = hcmCenter)
                            remoteSuggestions = fetched
                        } catch (_: Exception) {
                            remoteSuggestions = emptyList()
                        } finally {
                            isLoading = false
                        }
                    } else {
                        remoteSuggestions = emptyList()
                    }
                }

                val suggestions = remember(trimmed, remoteSuggestions) {
                    val stopMatches = allStops.filter { s ->
                        s.stopName.contains(trimmed, ignoreCase = true) || s.routeNumber.contains(trimmed, ignoreCase = true)
                    }.distinctBy { it.stopName }.map { s ->
                        // include lat/lng from local BusStop so selecting a stop navigates to directions
                        // do not show route number as subtitle to avoid clutter
                        SuggestionItem(
                            id = "stop:${s.id}",
                            title = s.stopName,
                            subtitle = null,
                            kind = SuggestionKind.STOP,
                            lat = s.lat,
                            lng = s.lng,
                            source = "local"
                        )
                    }

                    // static place/address suggestions (mocked) - you can replace with real API later
                    val staticPlaces = listOf(
                        SuggestionItem(id = "place:home", title = "Nhà của bạn", subtitle = "Địa chỉ cá nhân", kind = SuggestionKind.PLACE, source = "local"),
                        SuggestionItem(id = "place:work", title = "Cơ quan", subtitle = "Địa chỉ cơ quan", kind = SuggestionKind.PLACE, source = "local"),
                        SuggestionItem(id = "addr:cholon", title = "Cholon Bus Terminal (Bến Xe Chợ Lớn)", subtitle = "Lê Quang Sung, Quận 5", kind = SuggestionKind.ADDRESS, source = "local")
                    )

                    // combine: prioritize remote suggestions first (places/addresses), then stopMatches, then static place/address
                    val placeMatches = staticPlaces.filter { p -> (p.title.contains(trimmed, ignoreCase = true) || (p.subtitle?.contains(trimmed, ignoreCase = true) ?: false)) }
                    (remoteSuggestions + stopMatches + placeMatches).distinctBy { it.id }.take(60)
                }

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                ) {
                    if (suggestions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Không tìm thấy gợi ý", color = Color(0xFF9E9E9E))
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isLoading) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Đang tải...", color = Color(0xFF9E9E9E))
                                }
                                Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                            }

                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(suggestions) { item ->
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // on suggestion click: navigate to directions screen when coordinates available
                                            saveHistoryItem(item.title)
                                            // read nav args robustly: try current entry first, then previous entry as fallback
                                            val backArgs = navController.currentBackStackEntry?.arguments ?: navController.previousBackStackEntry?.arguments
                                            val target = backArgs?.getString("target") ?: ""
                                            val destTitleArg = backArgs?.getString("destTitle") ?: ""
                                            val destLatArg = backArgs?.getString("destLat")?.toDoubleOrNull()
                                            val destLngArg = backArgs?.getString("destLng")?.toDoubleOrNull()
                                            val destKindArg = backArgs?.getString("destKind") ?: ""

                                            if (item.lat != null && item.lng != null) {
                                                try {
                                                    val titleEnc = java.net.URLEncoder.encode(item.title, "UTF-8")
                                                    val kind = item.kind.name
                                                    if (target == "origin") {
                                                        // user is selecting an origin; navigate back to directions preserving destination context
                                                        val encDestTitle = try { java.net.URLEncoder.encode(destTitleArg, "UTF-8") } catch (_: Exception) { "" }
                                                        val destLatStr = destLatArg?.toString() ?: ""
                                                        val destLngStr = destLngArg?.toString() ?: ""
                                                        val destKindStr = destKindArg
                                                        // include origin title/kind so RouteScreen can display stops properly
                                                        val originTitleEnc = try { java.net.URLEncoder.encode(item.title, "UTF-8") } catch (_: Exception) { "" }
                                                        val originKindStr = kind
                                                        navController.navigate("directions?title=$encDestTitle&lat=$destLatStr&lng=$destLngStr&kind=$destKindStr&originLat=${item.lat}&originLng=${item.lng}&originTitle=$originTitleEnc&originKind=$originKindStr")
                                                    } else {
                                                        // target == dest: include any origin context passed into search so directions keep origin
                                                        val originTitleFromArgs = backArgs?.getString("originTitle") ?: ""
                                                        val originLatFromArgs = backArgs?.getString("originLat") ?: ""
                                                        val originLngFromArgs = backArgs?.getString("originLng") ?: ""
                                                        val originKindFromArgs = backArgs?.getString("originKind") ?: ""
                                                        navController.navigate("directions?title=$titleEnc&lat=${item.lat}&lng=${item.lng}&kind=$kind&originLat=$originLatFromArgs&originLng=$originLngFromArgs&originTitle=$originTitleFromArgs&originKind=$originKindFromArgs")
                                                    }
                                                } catch (e: Exception) {
                                                    query.value = item.title
                                                    Toast.makeText(context, "Chọn: ${item.title}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                // item has no coords: set text and suggest user to pick on map or refine
                                                query.value = item.title
                                                Toast.makeText(context, "Chọn: ${item.title}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                when (item.kind) {
                                                    SuggestionKind.STOP, SuggestionKind.ROUTE -> Icon(painter = painterResource(R.drawable.ic_bus_stop), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                                                    else -> Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF222222), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF263238), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            if (!item.subtitle.isNullOrBlank()) {
                                                Text(text = item.subtitle, color = Color(0xFF8F9AA0), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    Divider(color = Color(0xFFEEF0F1), thickness = 0.8.dp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { /* show full history */ }, modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)) {
                    Text(text = "TOÀN BỘ LỊCH SỬ TÌM KIẾM", fontWeight = FontWeight.Bold, color = Color(0xFF616161))
                }
            }
        }
    }
}

/**
 * Compute simple route matches where both stops exist on the same route (forward direction preferred)
 */
private fun computeRoutesBetween(allStops: List<BusStop>, allRoutes: List<com.map.buscity.data.BusRoute>, start: BusStop, end: BusStop): List<com.map.buscity.data.BusRoute> {
    val routeNumbersWithBoth = allStops.groupBy { it.routeNumber }
        .filter { entry ->
            val stops = entry.value
            val hasStart = stops.any { it.stopName == start.stopName }
            val hasEnd = stops.any { it.stopName == end.stopName }
            hasStart && hasEnd
        }
        .keys

    // prefer routes where start order < end order (same direction)
    val preferred = mutableListOf<com.map.buscity.data.BusRoute>()
    val others = mutableListOf<com.map.buscity.data.BusRoute>()

    allRoutes.forEach { r ->
        if (routeNumbersWithBoth.contains(r.routeNumber)) {
            // find stops for this route to determine order
            val stops = allStops.filter { it.routeNumber == r.routeNumber }
            val sIdx = stops.indexOfFirst { it.stopName == start.stopName }
            val eIdx = stops.indexOfFirst { it.stopName == end.stopName }
            if (sIdx >= 0 && eIdx >= 0 && sIdx < eIdx) preferred.add(r) else others.add(r)
        }
    }

    return (preferred + others).distinctBy { it.routeNumber }
}
@Composable
private fun SmallChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF6F6F6),
        shadowElevation = 2.dp,
        modifier = Modifier
            .height(36.dp)
            .padding(end = 4.dp)
            .clickable { }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = text, color = Color(0xFF424242))
        }
    }
}

@Composable
private fun StyledChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF6F9F6),
        shadowElevation = 2.dp,
        modifier = Modifier
            .height(36.dp)
            .wrapContentWidth()
            .padding(end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF4B5A57), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = Color(0xFF3B4A48), fontSize = 14.sp)
        }
    }
}
