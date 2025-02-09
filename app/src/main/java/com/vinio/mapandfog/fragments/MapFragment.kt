package com.vinio.mapandfog.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vinio.mapandfog.R
import com.vinio.mapandfog.databinding.FragmentMapBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding: FragmentMapBinding
        get() = (_binding
            ?: throw RuntimeException("FragmentGalleryBinding == null")) as FragmentMapBinding

    private lateinit var mapView: MapView
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var userLocationLayer: UserLocationLayer
    private val overlays = mutableListOf<PlacemarkMapObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKey = requireContext().packageManager.getApplicationInfo(
            requireContext().packageName,
            PackageManager.GET_META_DATA
        ).metaData?.getString("com.yandex.android.maps.apikey")

        if (apiKey.isNullOrEmpty()) {
            throw RuntimeException("API-ключ Яндекс.Карт не найден в манифесте!")
        }

        MapKitFactory.setApiKey(apiKey)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView
        requestLocationPermission()
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            initializeMap()
        } else {
            Toast.makeText(requireContext(), "Разрешение на местоположение не получено!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeMap() {
        if (::mapView.isInitialized) {
            val targetLocation = Point(56.8584, 35.9006)
            mapView.map.move(CameraPosition(targetLocation, 10.0f, 0.0f, 0.0f))

            mapObjects = mapView.map.mapObjects

            userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            userLocationLayer.isVisible = true
            userLocationLayer.isHeadingEnabled = true
            userLocationLayer.setObjectListener(object : UserLocationObjectListener {
                override fun onObjectAdded(userLocationView: UserLocationView) {}

                override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
                    userLocationView.arrow?.geometry?.let { userLocation ->
                        hideOverlaysNearUser(userLocation)
                    }
                }

                override fun onObjectRemoved(userLocationView: UserLocationView) {}
            })

            addOverlay(Point(56.8585, 35.9010))  // Добавляем изображение в конкретную точку
        } else {
            Toast.makeText(requireContext(), "Ошибка инициализации карты!", Toast.LENGTH_SHORT).show()
        }
    }

    /** Добавляем изображение (PNG) поверх карты */
    private fun addOverlay(position: Point) {
        val overlay = mapObjects.addPlacemark(position)
        overlay.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.fog))
        overlays.add(overlay)
    }

    /** Скрываем изображения, если пользователь подошел к ним слишком близко */
    private fun hideOverlaysNearUser(userLocation: Point) {
        overlays.forEach { overlay ->
            val distance = calculateDistance(overlay.geometry, userLocation)
            if (distance < 50) { // Если ближе 50 метров
                overlay.isVisible = false
            }
        }
    }

    /** Рассчитываем дистанцию между двумя точками */
    private fun calculateDistance(p1: Point, p2: Point): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            results
        )
        return results[0].toDouble()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
