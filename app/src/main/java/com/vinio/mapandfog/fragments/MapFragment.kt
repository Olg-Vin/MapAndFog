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
import com.vinio.mapandfog.databinding.FragmentMapBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView


class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding: FragmentMapBinding
        get() = (_binding
            ?: throw RuntimeException("FragmentGalleryBinding == null")) as FragmentMapBinding

    private lateinit var mapView: MapView

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
        // Проверяем, что карта готова
        if (::mapView.isInitialized) {
            // Настроим карту и позицию камеры
            val targetLocation = Point(56.8584, 35.9006)
            mapView.mapWindow.map.move(
                CameraPosition(targetLocation, 10.0f, 0.0f, 0.0f)
            )

            val userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            userLocationLayer.isVisible = true
            userLocationLayer.isHeadingEnabled = true
            userLocationLayer.setObjectListener(object : UserLocationObjectListener {
                override fun onObjectAdded(userLocationView: UserLocationView) {
                    // Настройка внешнего вида метки местоположения пользователя
                }

                override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {
                    // Обработка обновлений местоположения пользователя
                }

                override fun onObjectRemoved(userLocationView: UserLocationView) {
                    // Обработка удаления метки местоположения пользователя
                }
            })

        } else {
            Toast.makeText(requireContext(), "Ошибка инициализации карты!", Toast.LENGTH_SHORT).show()
        }
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
