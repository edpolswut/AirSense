package br.airsense.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.airsense.R

/**
 * Logo do AirSense. As folhas e o texto são pintados com a cor primária do tema
 * (verde escuro no claro, verde claro no escuro); o vaso mantém o marrom original.
 */
@Composable
fun AirSenseLogo(modifier: Modifier = Modifier) {
    // Proporção da imagem recortada (565 x 469 px), para a altura acompanhar a largura.
    Box(modifier = modifier.aspectRatio(565f / 469f)) {
        Image(
            painter = painterResource(R.drawable.logo_airsense_shape),
            contentDescription = "AirSense",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.logo_airsense_pot),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
