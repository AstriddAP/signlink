package com.signlink.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R

/**
 * Fragmento de configuración de perfil (Obsoleto).
 * Se mantiene temporalmente para evitar errores de compilación si hay referencias residuales,
 * pero ya no se utiliza en el flujo principal (Splash -> Login -> Home).
 */
class SetupFragment : Fragment(R.layout.fragment_setup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Este fragmento ya no es parte del flujo activo.
    }
}
