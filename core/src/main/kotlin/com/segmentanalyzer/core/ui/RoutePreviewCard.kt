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

/**
 * A real map (plain OpenStreetMap raster tiles — no API key/hosted style needed) showing a
 * segment's route, drawn from Strava's decoded polyline when available, else just a straight
 * line between the two endpoint coordinates. Shared between Segment Detail and Compare Rides.
 *
 * [highlightFraction] (0f..1f), when non-null, draws a marker that distance-fraction of the way
 * along the route — used to sync a position scrubbed on a chart (e.g. Compare Rides' Time Gap
 * chart) onto the map.
 */
@Composable
fun RoutePreviewCard(routePoints: List<LatLng>, highlightFraction: Float? = null, modifier: Modifier = Modifier) {
    if (routePoints.size < 2) return

    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val highlightStrokeColor = MaterialTheme.colorScheme.surface.toArgb()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                maplibreMap = map
                map.setStyle(Style.Builder()) { style ->
                    style.addSource(RasterSource(RASTER_SOURCE_ID, TileSet("osm", OSM_TILE_URL), OSM_TILE_SIZE))
                    style.addLayer(RasterLayer(RASTER_LAYER_ID, RASTER_SOURCE_ID))

                    val lineString = LineString.fromLngLats(routePoints.map { Point.fromLngLat(it.longitude, it.latitude) })
                    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, Feature.fromGeometry(lineString)))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(routeColor),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                    )

                    style.addSource(GeoJsonSource(HIGHLIGHT_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
                    style.addLayer(
                        CircleLayer(HIGHLIGHT_LAYER_ID, HIGHLIGHT_SOURCE_ID).withProperties(
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleColor(routeColor),
                            PropertyFactory.circleStrokeColor(highlightStrokeColor),
                            PropertyFactory.circleStrokeWidth(2f),
                        ),
                    )

                    val boundsBuilder = LatLngBounds.Builder()
                    routePoints.forEach { boundsBuilder.include(MapLibreLatLng(it.latitude, it.longitude)) }
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), BOUNDS_PADDING_PX))
                }
            }
        }
    }

    LaunchedEffect(highlightFraction, routePoints) {
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
