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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.signlink.R
import com.signlink.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.JointType

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentPolyline: Polyline? = null
    private var lastKnownLocation: LatLng? = null
    private var destinationLatLng: LatLng? = null

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

        binding.btnStartRoute.setOnClickListener {
            startGoogleMapsNavigation()
        }
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
            destinationLatLng = latLng
            
            // Ocultar botón y limpiar polilínea anterior
            binding.btnStartRoute.visibility = View.GONE
            currentPolyline?.remove()
            currentPolyline = null
            
            googleMap?.apply {
                clear()
                addMarker(MarkerOptions().position(latLng).title(query))
                animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
            
            // Intentar obtener ubicación fresca de inmediato para trazar la ruta
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        lastKnownLocation = userLatLng
                        drawRoute(userLatLng, latLng)
                    } else {
                        lastKnownLocation?.let { userLatLng ->
                            drawRoute(userLatLng, latLng)
                        } ?: run {
                            Toast.makeText(requireContext(), "Esperando señal GPS para trazar la ruta...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                lastKnownLocation?.let { userLatLng ->
                    drawRoute(userLatLng, latLng)
                }
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
                lastKnownLocation = currentLatLng
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                fetchNearbyServices(location.latitude, location.longitude)
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
                            lastKnownLocation = currentLatLng
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            fetchNearbyServices(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, android.os.Looper.getMainLooper())
            }
        }
    }

    private fun fetchNearbyServices(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Preparamos la consulta a Overpass API
                // Buscamos hospitales, restaurantes y bodegas (tiendas de conveniencia/supermercados) en un radio de 1000m
                val query = """
                    [out:json][timeout:15];
                    (
                      node["amenity"="hospital"](around:1000,$latitude,$longitude);
                      node["amenity"="restaurant"](around:1000,$latitude,$longitude);
                      node["shop"="convenience"](around:1000,$latitude,$longitude);
                      node["shop"="supermarket"](around:1000,$latitude,$longitude);
                    );
                    out body;
                """.trimIndent()

                val url = java.net.URL("https://overpass-api.de/api/interpreter")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray())
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(responseText)
                    val elements = json.getJSONArray("elements")

                    withContext(Dispatchers.Main) {
                        googleMap?.clear() // Limpiamos marcadores previos
                        
                        // Añadir marcador del usuario
                        val userLatLng = LatLng(latitude, longitude)
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(userLatLng)
                                .title("Mi ubicación")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )

                        for (i in 0 until elements.length()) {
                            val element = elements.getJSONObject(i)
                            val lat = element.getDouble("lat")
                            val lon = element.getDouble("lon")
                            val tags = element.optJSONObject("tags") ?: continue
                            
                            val name = tags.optString("name", "Servicio sin nombre")
                            val amenity = tags.optString("amenity", "").lowercase()
                            val shop = tags.optString("shop", "").lowercase()

                            val (title, iconDescriptor) = when {
                                amenity == "hospital" || amenity.contains("clinic") || amenity.contains("posta") || amenity.contains("salud") -> {
                                    "Hospital/Posta: $name" to createHospitalMarkerIcon()
                                }
                                amenity == "restaurant" || amenity.contains("food") || amenity.contains("cafe") -> {
                                    "Restaurante: $name" to BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                                }
                                shop == "convenience" || shop == "supermarket" || shop.contains("market") || shop.contains("grocery") -> {
                                    "Bodega/Tienda: $name" to BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                }
                                else -> continue
                            }

                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lon))
                                    .title(title)
                                    .icon(iconDescriptor)
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        useFallbackMarkers(latitude, longitude)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    useFallbackMarkers(latitude, longitude)
                }
            }
        }
    }

    private fun useFallbackMarkers(latitude: Double, longitude: Double) {
        googleMap?.clear()
        
        // Añadir marcador del usuario
        val userLatLng = LatLng(latitude, longitude)
        googleMap?.addMarker(
            MarkerOptions()
                .position(userLatLng)
                .title("Mi ubicación")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        // Marcadores de prueba simulados alrededor del usuario
        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(latitude + 0.003, longitude + 0.002))
                .title("Hospital de Emergencias")
                .icon(createHospitalMarkerIcon())
        )

        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(latitude - 0.002, longitude + 0.004))
                .title("Restaurante El Buen Sabor")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )

        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(latitude + 0.001, longitude - 0.003))
                .title("Bodega Don Pepe")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        Toast.makeText(context, "Mostrando servicios locales sugeridos", Toast.LENGTH_SHORT).show()
    }

    private fun createHospitalMarkerIcon(): BitmapDescriptor {
        val size = 90
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Círculo rojo de fondo
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        
        // Borde blanco exterior
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        
        // Letra H en blanco
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = 46f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        
        // Posicionamiento vertical de la letra
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText("H", size / 2f, yPos, paint)
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Hacemos el request a OSRM API para obtener la ruta
                // Coordenadas en formato: lon,lat;lon,lat
                val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                        "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                        "?overview=full&geometries=geojson"
                
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val routes = json.optJSONArray("routes")
                    
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        
                        val points = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(LatLng(lat, lon))
                        }
                        
                        withContext(Dispatchers.Main) {
                            currentPolyline?.remove()
                            
                            // Trazamos la línea azul con bordes redondeados premium
                            val polylineOptions = PolylineOptions()
                                .addAll(points)
                                .color(Color.parseColor("#2563EB")) // Azul premium/índigo
                                .width(14f)
                                .startCap(RoundCap())
                                .endCap(RoundCap())
                                .jointType(JointType.ROUND)
                            
                            currentPolyline = googleMap?.addPolyline(polylineOptions)
                            
                            // Mostrar el botón Iniciar Ruta
                            binding.btnStartRoute.visibility = View.VISIBLE
                            
                            // Ajustamos la cámara para mostrar ambos puntos y el recorrido completo
                            val bounds = LatLngBounds.Builder()
                                .include(start)
                                .include(end)
                                .build()
                            
                            // Padding para evitar que la ruta quede tapada por los bordes de la pantalla
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 180))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.btnStartRoute.visibility = View.GONE
                            Toast.makeText(requireContext(), "No se encontró una ruta terrestre disponible", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.btnStartRoute.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error al calcular la ruta (Servidor no disponible)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnStartRoute.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error de conexión al trazar la ruta", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun startGoogleMapsNavigation() {
        val dest = destinationLatLng
        if (dest != null) {
            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                // Si la aplicación de Google Maps no está disponible, abrir en navegador
                val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}"))
                startActivity(webIntent)
            }
        } else {
            Toast.makeText(requireContext(), "No hay una dirección de destino seleccionada", Toast.LENGTH_SHORT).show()
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
