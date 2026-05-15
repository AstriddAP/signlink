package com.signlink.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.signlink.R
import com.signlink.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            enableMyLocation()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        enableMyLocation()
        
        // Configuración estética del mapa
        map.uiSettings.isZoomControlsEnabled = false // Desactivamos los de Google
        map.uiSettings.isMyLocationButtonEnabled = false 
        map.uiSettings.isMapToolbarEnabled = false
        
        // MOVER CONTROLES DE GOOGLE:
        // Aumentamos el padding inferior significativamente (300px) para que el zoom 
        // y el logo de Google suban y no choquen con nuestros botones ni la barra inferior.
        map.setPadding(0, 250, 0, 280)

        // Aquí podrías cargar un estilo JSON si lo deseas más adelante
        // map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapBinding.bind(view)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        setupSearchAndSuggestions()
        setupFabListeners()
    }

    private fun setupSearchAndSuggestions() {
        // Al darle a "Buscar" en el teclado
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotEmpty()) {
                    hideKeyboard()
                    searchAddress(query)
                }
                true
            } else {
                false
            }
        }

        // Al seleccionar una sugerencia de la lista
        binding.etSearch.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position) as String
            hideKeyboard()
            searchAddress(selection)
        }

        // Lógica de sugerencias mientras escribe
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.length > 3) { // Empezar sugerencias a partir de 4 letras
                    updateSuggestions(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateSuggestions(query: String) {
        val geocoder = Geocoder(requireContext())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    val suggestionList = addresses.mapNotNull { it.getAddressLine(0) }
                    activity?.runOnUiThread {
                        updateSearchAdapter(suggestionList)
                    }
                }
                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                }
            })
        } else {
            @Suppress("DEPRECATION")
            Thread {
                try {
                    val addresses = geocoder.getFromLocationName(query, 5)
                    val suggestionList = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
                    activity?.runOnUiThread {
                        updateSearchAdapter(suggestionList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun updateSearchAdapter(suggestionList: List<String>) {
        if (suggestionList.isNotEmpty() && isAdded && _binding != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestionList)
            binding.etSearch.setAdapter(adapter)
            if (binding.etSearch.hasFocus()) {
                binding.etSearch.showDropDown()
            }
        }
    }

    private fun searchAddress(address: String) {
        val geocoder = Geocoder(requireContext())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    activity?.runOnUiThread {
                        handleGeocodeResult(addresses, address)
                    }
                }
                override fun onError(errorMessage: String?) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Error al buscar: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            @Suppress("DEPRECATION")
            Thread {
                try {
                    val addresses = geocoder.getFromLocationName(address, 1)
                    activity?.runOnUiThread {
                        handleGeocodeResult(addresses, address)
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun handleGeocodeResult(addresses: List<android.location.Address>?, query: String) {
        if (!addresses.isNullOrEmpty()) {
            val foundAddress = addresses[0]
            val latLng = LatLng(foundAddress.latitude, foundAddress.longitude)
            
            googleMap?.apply {
                clear()
                addMarker(MarkerOptions().position(latLng).title(query))
                animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        } else {
            Toast.makeText(requireContext(), "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), android.view.inputmethod.InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun setupFabListeners() {
        binding.fabMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        binding.fabShareLocation.setOnClickListener {
            shareCurrentLocation()
        }

        // Lógica de los nuevos botones de Zoom
        binding.fabZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.fabZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        binding.ivMapType.setOnClickListener {
            val currentType = googleMap?.mapType
            googleMap?.mapType = if (currentType == GoogleMap.MAP_TYPE_NORMAL) {
                GoogleMap.MAP_TYPE_SATELLITE
            } else {
                GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            moveToCurrentLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                // Si la última ubicación es nula, intentamos obtener una actualización fresca
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).setMaxUpdates(1).build()
                
                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val lastLoc = result.lastLocation
                        if (lastLoc != null) {
                            val currentLatLng = LatLng(lastLoc.latitude, lastLoc.longitude)
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        } else {
                            Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, android.os.Looper.getMainLooper())
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun shareCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val uri = "http://maps.google.com/maps?q=${it.latitude},${it.longitude}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Mi ubicación actual")
                    putExtra(Intent.EXTRA_TEXT, "Hola! Esta es mi ubicación actual: $uri")
                }
                startActivity(Intent.createChooser(intent, "Compartir ubicación vía"))
            } ?: run {
                Toast.makeText(requireContext(), "Ubicación no disponible para compartir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
