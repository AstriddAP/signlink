package com.signlink.ui.captioning

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.signlink.R
import com.signlink.databinding.FragmentLiveCaptioningBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveCaptioningFragment : Fragment() {

    private var _binding: FragmentLiveCaptioningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveCaptioningViewModel by viewModels()
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var autoCaptureJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true
        ) {
            startCamera()
        } else {
            Toast.makeText(context, "Permisos necesarios denegados", Toast.LENGTH_SHORT).show()
        }
    }

    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveCaptioningBinding.inflate(inflater, container, false)
        
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    CaptioningScreen(viewModel, onTakePhoto = { takePhoto() })
                }
            }
        }
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkPermissions()
        startAutoCaptureLoop()
    }

    private fun startAutoCaptureLoop() {
        autoCaptureJob?.cancel()
        autoCaptureJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            while (isActive) {
                // Si hubo error de cuota, esperamos 20 seg. Si no, 12 seg para no agotar los 15 RPM de Gemini Free
                val delayTime = if (viewModel.errorMessage.value?.contains("Cuota") == true) 20000L else 12000L
                
                if (!viewModel.isProcessingIA.value && isResumed) {
                    takePhoto()
                }
                delay(delayTime)
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Crear archivo temporal
        val photoFile = File(
            requireContext().cacheDir,
            "analysis_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(context, "Error al capturar imagen", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Redimensionar antes de enviar
                    val resizedFile = resizeImageFile(photoFile)
                    
                    // Obtener dimensiones para el escalado de Bounding Boxes
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(resizedFile.absolutePath, options)
                    
                    viewModel.analizarImagenRemota(resizedFile, options.outWidth, options.outHeight)
                }
            }
        )
    }

    private fun resizeImageFile(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val maxSize = 1024
        var width = bitmap.width
        var height = bitmap.height

        if (width > height) {
            if (width > maxSize) {
                height = (height * maxSize / width)
                width = maxSize
            }
        } else {
            if (height > maxSize) {
                width = (width * maxSize / height)
                height = maxSize
            }
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val resizedFile = File(requireContext().cacheDir, "resized_${file.name}")
        FileOutputStream(resizedFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        if (file.exists()) file.delete() // Borrar original grande
        return resizedFile
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.viewFinder.surfaceProvider
            }

            // Configuramos la captura para MÁXIMA CALIDAD en lugar de velocidad
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()

            try {
                cameraProvider.unbindAll()

                // Ligamos a la cámara SOLAMENTE el preview y el capturador de fotos
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("LiveCaptioning", "Error al iniciar cámara", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
        
        viewModel.startRecording()
    }

    override fun onResume() {
        super.onResume()
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPermission) {
            viewModel.startRecording()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopRecording()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoCaptureJob?.cancel()
        cameraExecutor.shutdown()
        viewModel.stopRecording()
        _binding = null
    }

    private fun ImageProxy.toFile(): File {
        val bitmap = this.toBitmap()
        val file = File(requireContext().cacheDir, "ia_analysis_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
        }
        return file
    }
}

@Composable
fun CaptioningScreen(viewModel: LiveCaptioningViewModel, onTakePhoto: () -> Unit = {}) {
    val captions by viewModel.captions.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isProcessingIA by viewModel.isProcessingIA.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    LaunchedEffect(captions.size) {
        if (captions.isNotEmpty()) {
            listState.animateScrollToItem(captions.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // CAPA DE DIBUJO: Rectángulos de IA
        BoundingBoxOverlay(viewModel, predictions)

        // Indicador de estado (Escaneando o Error)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessingIA || errorMessage != null) {
                Surface(
                    color = if (errorMessage != null) ComposeColor.Red.copy(alpha = 0.7f) else ComposeColor.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isProcessingIA && errorMessage == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ComposeColor(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = errorMessage ?: "Analizando entorno...",
                            color = ComposeColor.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Fondo con gradiente para legibilidad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ComposeColor.Transparent, ComposeColor.Black.copy(alpha = 0.5f))
                    )
                )
        )

        // Lista de subtítulos flotantes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(captions) { caption ->
                CaptionCard(caption)
            }
        }
    }
}

@Composable
fun BoundingBoxOverlay(viewModel: LiveCaptioningViewModel, predictions: List<com.signlink.data.remote.Prediction>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val imgWidth = viewModel.lastImageWidth
        val imgHeight = viewModel.lastImageHeight
        
        if (imgWidth > 0 && imgHeight > 0 && predictions.isNotEmpty()) {
            // Factor de escala: Pantalla / Imagen Original
            val scaleX = size.width / imgWidth
            val scaleY = size.height / imgHeight
            
            predictions.forEach { prediction ->
                val box = prediction.boundingBox ?: return@forEach
                
                // Coordenadas escaladas
                val left = (box.xmin ?: 0).toFloat() * scaleX
                val top = (box.ymin ?: 0).toFloat() * scaleY
                val right = (box.xmax ?: 0).toFloat() * scaleX
                val bottom = (box.ymax ?: 0).toFloat() * scaleY
                
                // Dibujar el rectángulo
                drawRect(
                    color = ComposeColor(0xFF00E5FF), // Cyan neón
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun CaptionCard(caption: LiveCaptioningViewModel.Caption) {
    val isLens = caption.speaker == "LENS"
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.White.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icono pequeño de fuente
            Surface(
                color = if (isLens) ComposeColor(0xFF00E5FF).copy(alpha = 0.2f) else ComposeColor(caption.color.toColorInt()).copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isLens) Icons.Default.CameraAlt else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = if (isLens) ComposeColor(0xFF00B8D4) else ComposeColor(caption.color.toColorInt())
                )
            }
            
            Column {
                if (!isLens) {
                    Text(
                        text = caption.speaker,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ComposeColor(caption.color.toColorInt()),
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = caption.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = ComposeColor.Black.copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// Extensión simple para convertir hex string a Color int de Compose
fun String.toColorInt(): Int {
    return android.graphics.Color.parseColor(this)
}
