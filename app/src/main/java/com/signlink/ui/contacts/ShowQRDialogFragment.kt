package com.signlink.ui.contacts

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.signlink.databinding.DialogShowQrBinding

class ShowQRDialogFragment : DialogFragment() {

    private var _binding: DialogShowQrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogShowQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hacer el fondo del diálogo transparente para que respete las esquinas redondeadas
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val name = currentUser.displayName ?: "Usuario"
            val email = currentUser.email ?: ""
            val uid = currentUser.uid

            binding.tvUserName.text = name
            binding.tvUserEmail.text = email

            // Contenido del QR parseable: signlink://contact?uid={uid}&name={name}&email={email}
            val encodedName = android.net.Uri.encode(name)
            val encodedEmail = android.net.Uri.encode(email)
            val qrContent = "signlink://contact?uid=$uid&name=$encodedName&email=$encodedEmail"
            try {
                val bitmap = generateQrCode(qrContent, 512)
                binding.ivQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun generateQrCode(text: String, size: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ShowQRDialogFragment"
        fun newInstance() = ShowQRDialogFragment()
    }
}
