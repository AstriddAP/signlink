package com.signlink.ui.captioning

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class LensAnalyzer(
    private val onResult: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            // Realizamos reconocimiento de texto
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotBlank()) {
                        onResult("Texto detectado: ${visionText.text.take(50)}...")
                    }
                }
                .addOnCompleteListener {
                    // Procesamos etiquetas de imagen (objetos)
                    labeler.process(image)
                        .addOnSuccessListener { labels ->
                            val topLabel = labels.firstOrNull()?.text
                            if (topLabel != null) {
                                onResult("Veo: $topLabel")
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
        } else {
            imageProxy.close()
        }
    }
}
