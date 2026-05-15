package com.signlink.ui.documents

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signlink.R
import com.signlink.data.local.entity.DocumentEntity
import com.signlink.databinding.FragmentAddDocumentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddDocumentFragment : Fragment(R.layout.fragment_add_document) {

    private var _binding: FragmentAddDocumentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentViewModel by viewModels()

    private var frontImageUri: Uri? = null
    private var backImageUri: Uri? = null
    private var isSelectingFront = true
    private var tempImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleImageSelection(uri)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            handleImageSelection(tempImageUri)
        }
    }

    private fun handleImageSelection(uri: Uri?) {
        uri?.let {
            if (isSelectingFront) {
                frontImageUri = it
                binding.ivFront.setImageURI(it)
                binding.ivFront.isVisible = true
                binding.llAddFront.isVisible = false
            } else {
                backImageUri = it
                binding.ivBack.setImageURI(it)
                binding.ivBack.isVisible = true
                binding.llAddBack.isVisible = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddDocumentBinding.bind(view)

        setupListeners()
    }

    private fun setupListeners() {
        binding.cardFront.setOnClickListener {
            isSelectingFront = true
            showImagePickerDialog()
        }

        binding.cardBack.setOnClickListener {
            isSelectingFront = false
            showImagePickerDialog()
        }

        binding.btnSave.setOnClickListener {
            saveDocument()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de galería")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImage.launch("image/*")
                }
            }
            .show()
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_",
            ".jpg",
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePhoto.launch(tempImageUri)
    }

    private fun saveDocument() {
        val name = binding.etName.text.toString()
        val type = when (binding.toggleGroupType.checkedButtonId) {
            R.id.btn_type_dni -> "DNI"
            R.id.btn_type_conadis -> "CONADIS"
            else -> "OTROS"
        }

        if (name.isBlank()) {
            Toast.makeText(context, "Ingresa un nombre para el documento", Toast.LENGTH_SHORT).show()
            return
        }

        if (frontImageUri == null) {
            Toast.makeText(context, "Sube al menos la parte frontal", Toast.LENGTH_SHORT).show()
            return
        }

        val document = DocumentEntity(
            title = name,
            type = type,
            frontImagePath = frontImageUri.toString(),
            backImagePath = backImageUri?.toString()
        )

        viewModel.addDocument(document)
        Toast.makeText(context, "Documento guardado", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
