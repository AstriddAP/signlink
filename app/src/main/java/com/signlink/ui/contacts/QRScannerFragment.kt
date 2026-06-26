package com.signlink.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.signlink.data.model.User
import com.signlink.data.repository.UserRepository
import com.signlink.databinding.FragmentQrScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class QRScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var cameraExecutor: ExecutorService
    private val isScanningActive = java.util.concurrent.atomic.AtomicBoolean(true)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Permiso de cámara requerido para escanear códigos QR", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRAnalyzer { qrValue ->
                        if (isScanningActive.compareAndSet(true, false)) {
                            handleQrCode(qrValue)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("QRScanner", "Error al iniciar cámara: ", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun handleQrCode(qrValue: String) {
        try {
            val uri = Uri.parse(qrValue)
            val uid = uri.getQueryParameter("uid") ?: ""
            val name = uri.getQueryParameter("name") ?: ""
            val email = uri.getQueryParameter("email") ?: ""

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Sesión no activa", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                return
            }

            if (uid == currentUser.uid) {
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "No puedes agregarte a ti mismo como contacto", Toast.LENGTH_SHORT).show()
                    isScanningActive.set(true)
                }
                return
            }

            val contact = User(
                uid = uid,
                displayName = name,
                email = email
            )

            val currentUserModel = User(
                uid = currentUser.uid,
                displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Usuario",
                email = currentUser.email ?: ""
            )

            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                val result = userRepository.addContact(currentUser.uid, contact)
                if (result.isSuccess) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val notificationId = java.util.UUID.randomUUID().toString()
                    val notificationData = mapOf(
                        "type" to "MUTUAL_CONTACT_ADD",
                        "uid" to currentUserModel.uid,
                        "displayName" to currentUserModel.displayName,
                        "email" to currentUserModel.email,
                        "title" to "Contacto agregado",
                        "body" to "${currentUserModel.displayName} te ha agregado como contacto",
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    try {
                        db.collection("users").document(contact.uid)
                            .collection("notifications")
                            .document(notificationId)
                            .set(notificationData)
                            .await()

                        // Obtener el fcmToken del contacto para enviarle un push directo
                        val contactDoc = db.collection("users").document(contact.uid).get().await()
                        val fcmToken = contactDoc.getString("fcmToken") ?: ""
                        if (fcmToken.isNotEmpty()) {
                            val fcmPayload = mapOf(
                                "notificationId" to notificationId,
                                "type" to "MUTUAL_CONTACT_ADD",
                                "uid" to currentUserModel.uid,
                                "displayName" to currentUserModel.displayName,
                                "email" to currentUserModel.email,
                                "title" to "Contacto agregado",
                                "body" to "${currentUserModel.displayName} te ha agregado como contacto"
                            )
                            com.signlink.data.remote.FcmSender.sendNotification(fcmToken, fcmPayload)
                        }
                    } catch (e: Exception) {
                        Log.e("QRScanner", "Error al enviar notificación de contacto mutuo", e)
                    }

                    Toast.makeText(context, "Contacto agregado: $name", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Error desconocido"
                    if (errorMessage.contains("database", ignoreCase = true) && errorMessage.contains("does not exist", ignoreCase = true)) {
                        showDatabaseNotFoundErrorDialog()
                    } else {
                        Toast.makeText(context, "Error al guardar el contacto: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                    isScanningActive.set(true)
                }
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Error al procesar el código QR", e)
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Código QR no válido", Toast.LENGTH_SHORT).show()
                isScanningActive.set(true)
            }
        }
    }

    private fun showDatabaseNotFoundErrorDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Base de Datos No Encontrada")
            .setMessage("La base de datos Firestore (default) no existe en tu proyecto de Firebase (signlink-2acca).\n\nPor favor, ingresa a la consola de Firebase, ve a la sección de 'Firestore Database' y haz clic en 'Crear base de datos' para habilitarla.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}

class QRAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.startsWith("signlink://contact")) {
                            onQrDetected(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignorar errores
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
