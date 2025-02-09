package com.vinio.mapandfog.fragments

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider

class MapOverlayManager(private val mapObjects: MapObjectCollection, private val context: android.content.Context) {

    private val overlays = mutableListOf<PlacemarkMapObject>()
    private val visitedPoints = mutableListOf<Point>()

    fun addOverlay(imageRes: Int, position: Point) {
        val overlay = mapObjects.addPlacemark(position)
        overlay.setIcon(ImageProvider.fromResource(context, imageRes))
        overlays.add(overlay)
    }

    fun updateUserLocation(userLocation: Point) {
        visitedPoints.add(userLocation)
        hideVisitedOverlays(userLocation)
    }

    private fun hideVisitedOverlays(userLocation: Point) {
        overlays.forEach { overlay ->
            val distance = calculateDistance(overlay.geometry, userLocation)
            if (distance < 50) { // Если пользователь ближе 50 метров
                overlay.isVisible = false
            }
        }
    }

    private fun calculateDistance(p1: Point, p2: Point): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            results
        )
        return results[0].toDouble()
    }
}


