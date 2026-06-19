package com.signlink.ui.captioning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.signlink.databinding.FragmentLiveCaptioningBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiveCaptioningFragment : Fragment() {

    private var _binding: FragmentLiveCaptioningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveCaptioningViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Permiso de micrófono requerido para subtítulos", Toast.LENGTH_SHORT).show()
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
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
        viewModel.stopTts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopRecording()
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
        // Indicador de estado superior: Escuchando conversación...
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) ComposeColor.Green else ComposeColor.Gray)
            )
            Text(
                text = if (isRecording) "Escuchando conversación..." else "Micrófono inactivo",
                color = ComposeColor.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Lista de subtítulos flotantes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp, top = 60.dp),
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
fun CaptionCard(caption: LiveCaptioningViewModel.Caption) {
    val isSystem = caption.speaker == "Sistema"
    val isProcessing = caption.speaker == "Procesando..." || caption.speaker == "Escuchando..."

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProcessing) ComposeColor.LightGray.copy(alpha = 0.4f) else ComposeColor.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icono pequeño de micrófono
            Surface(
                color = if (isSystem) ComposeColor.Red.copy(alpha = 0.2f) else ComposeColor(caption.color.toColorInt()).copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = if (isSystem) ComposeColor.Red else ComposeColor(caption.color.toColorInt())
                )
            }

            Column {
                if (!isSystem && !isProcessing) {
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
                    fontWeight = if (isProcessing) FontWeight.Normal else FontWeight.Medium,
                    color = if (isProcessing) ComposeColor.DarkGray else ComposeColor.Black.copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

fun String.toColorInt(): Int {
    return android.graphics.Color.parseColor(this)
}
