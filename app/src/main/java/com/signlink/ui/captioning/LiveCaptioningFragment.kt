package com.signlink.ui.captioning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveCaptioningFragment : Fragment() {

    private var _binding: FragmentLiveCaptioningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveCaptioningViewModel by viewModels()

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
                    CaptioningScreen(viewModel)
                }
            }
        }
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions()
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

            val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), LensAnalyzer { result ->
                        viewModel.onVisualDetection(result)
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (exc: Exception) {
                // Handle errors
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@Composable
fun CaptioningScreen(viewModel: LiveCaptioningViewModel) {
    val captions by viewModel.captions.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    LaunchedEffect(captions.size) {
        if (captions.isNotEmpty()) {
            listState.animateScrollToItem(captions.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                .padding(bottom = 110.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(captions) { caption ->
                CaptionCard(caption)
            }
        }

        // Botones de control
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleRecording() },
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = ComposeColor.White
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
