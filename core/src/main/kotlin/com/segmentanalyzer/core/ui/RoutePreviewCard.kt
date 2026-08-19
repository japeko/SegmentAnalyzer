package com.segmentanalyzer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.util.pointAtFraction
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val OSM_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
private const val OSM_TILE_SIZE = 256
private const val RASTER_SOURCE_ID = "osm-raster-source"
private const val RASTER_LAYER_ID = "osm-raster-layer"
private const val ROUTE_SOURCE_ID = "segment-route-source"
private const val ROUTE_LAYER_ID = "segment-route-layer"
private const val HIGHLIGHT_SOURCE_ID = "segment-highlight-source"
private const val HIGHLIGHT_LAYER_ID = "segment-highlight-layer"
private const val BOUNDS_PADDING_PX = 48
private const val ROUTE_COLOR_PROPERTY = "color"

// Standard climbing-gradient palette: descent/flat reads as easy, steep grades read as hot.
private const val GRADIENT_COLOR_DESCENT = "#2E7D32"
private const val GRADIENT_COLOR_FLAT = "#66BB6A"
private const val GRADIENT_COLOR_MODERATE = "#FFC107"
private const val GRADIENT_COLOR_HARD = "#FF9800"
private const val GRADIENT_COLOR_STEEP = "#E53935"

/**
 * A real map (plain OpenStreetMap raster tiles — no API key/hosted style needed) showing a
 * segment's route, drawn from Strava's decoded polyline when available, else just a straight
 * line between the two endpoint coordinates. Shared between Segment Detail and Compare Rides.
 *
 * [gradientPercents], when provided (size = routePoints.size - 1, one value per line segment),
 * colors the route by climbing steepness instead of a single flat color — used on Compare Rides
 * when the anchor ride has a real GPS track with elevation.
 *
 * [highlightFraction] (0f..1f), when non-null, draws a marker that distance-fraction of the way
 * along the route — used to sync a position scrubbed on a chart (e.g. Compare Rides' Time Gap
 * chart) onto the map.
 */
@Composable
fun RoutePreviewCard(
    routePoints: List<LatLng>,
    gradientPercents: List<Double>? = null,
    highlightFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    if (routePoints.size < 2) return

    val routeColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val flatColorHex = hexColor(routeColorArgb)
    val highlightStrokeColor = MaterialTheme.colorScheme.surface.toArgb()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(Style.Builder()) { style ->
                    style.addSource(RasterSource(RASTER_SOURCE_ID, TileSet("osm", OSM_TILE_URL), OSM_TILE_SIZE))
                    style.addLayer(RasterLayer(RASTER_LAYER_ID, RASTER_SOURCE_ID))

                    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(Expression.get(ROUTE_COLOR_PROPERTY)),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                    )

                    style.addSource(GeoJsonSource(HIGHLIGHT_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
                    style.addLayer(
                        CircleLayer(HIGHLIGHT_LAYER_ID, HIGHLIGHT_SOURCE_ID).withProperties(
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleColor(routeColorArgb),
                            PropertyFactory.circleStrokeColor(highlightStrokeColor),
                            PropertyFactory.circleStrokeWidth(2f),
                        ),
                    )
                }
                // The style callback above only builds empty sources/layers; maplibreMap becoming
                // non-null is what actually triggers the route-drawing LaunchedEffect below — the
                // single place that populates them, so there's one codepath for both the first
                // draw and any later update (e.g. once a real GPS track finishes loading).
                maplibreMap = map
            }
        }
    }

    LaunchedEffect(routePoints, gradientPercents, maplibreMap) {
        val map = maplibreMap ?: return@LaunchedEffect
        val routeFeatures = buildRouteFeatureCollection(routePoints, gradientPercents, flatColorHex)
        map.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(routeFeatures)
        }
        val boundsBuilder = LatLngBounds.Builder()
        routePoints.forEach { boundsBuilder.include(MapLibreLatLng(it.latitude, it.longitude)) }
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), BOUNDS_PADDING_PX))
    }

    LaunchedEffect(highlightFraction, routePoints, maplibreMap) {
        val map = maplibreMap ?: return@LaunchedEffect
        val point = highlightFraction?.let { pointAtFraction(routePoints, it) }
        map.getStyle { style ->
            val source = style.getSourceAs<GeoJsonSource>(HIGHLIGHT_SOURCE_ID) ?: return@getStyle
            if (point != null) {
                source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)))
            } else {
                source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
        Text(
            text = "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/**
 * One feature per line segment when [gradientPercents] lines up with [routePoints] (colored by
 * climbing steepness), else a single feature for the whole route in [flatColorHex].
 */
private fun buildRouteFeatureCollection(routePoints: List<LatLng>, gradientPercents: List<Double>?, flatColorHex: String): FeatureCollection {
    if (gradientPercents == null || gradientPercents.size != routePoints.size - 1) {
        val lineString = LineString.fromLngLats(routePoints.map { Point.fromLngLat(it.longitude, it.latitude) })
        val feature = Feature.fromGeometry(lineString)
        feature.addStringProperty(ROUTE_COLOR_PROPERTY, flatColorHex)
        return FeatureCollection.fromFeatures(arrayOf(feature))
    }

    val features = routePoints.zipWithNext().mapIndexed { index, (a, b) ->
        val lineString = LineString.fromLngLats(listOf(Point.fromLngLat(a.longitude, a.latitude), Point.fromLngLat(b.longitude, b.latitude)))
        Feature.fromGeometry(lineString).apply { addStringProperty(ROUTE_COLOR_PROPERTY, colorForGradientPercent(gradientPercents[index])) }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun colorForGradientPercent(percent: Double): String = when {
    percent < 0.0 -> GRADIENT_COLOR_DESCENT
    percent < 3.0 -> GRADIENT_COLOR_FLAT
    percent < 6.0 -> GRADIENT_COLOR_MODERATE
    percent < 9.0 -> GRADIENT_COLOR_HARD
    else -> GRADIENT_COLOR_STEEP
}

private fun hexColor(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
