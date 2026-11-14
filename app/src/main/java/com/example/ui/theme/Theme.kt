package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

data class AgentSemanticColors(
    val observe: Color,
    val think: Color,
    val act: Color,
    val recover: Color,
    val codeBackground: Color,
    val codeBorder: Color
)

val LocalAgentSemanticColors = staticCompositionLocalOf {
    AgentSemanticColors(
        observe = ElectricCyan,
        think = SoftViolet,
        act = EmeraldMint,
        recover = AmberRecover,
        codeBackground = ObsidianCanvas,
        codeBorder = BorderHairline
    )
}

private val TelemetryDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnCyanDark,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = CrispWhiteText,
    secondary = SoftViolet,
    onSecondary = OnVioletDark,
    secondaryContainer = DeepVioletContainer,
    onSecondaryContainer = CrispWhiteText,
    tertiary = EmeraldMint,
    onTertiary = OnEmeraldDark,
    tertiaryContainer = DeepEmeraldContainer,
    onTertiaryContainer = CrispWhiteText,
    background = ObsidianCanvas,
    onBackground = CrispWhiteText,
    surface = SlateSurface,
    onSurface = CrispWhiteText,
    surfaceVariant = ElevatedPanel,
    onSurfaceVariant = MutedSlateText,
    outline = BorderHairline,
    error = CoralError,
    onError = ObsidianCanvas,
    errorContainer = DeepErrorContainer,
    onErrorContainer = CrispWhiteText
)

private val StudioLightColorScheme = lightColorScheme(
    primary = SkyPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = SkyContainerLight,
    onPrimaryContainer = InkDarkText,
    secondary = VioletSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = VioletContainerLight,
    onSecondaryContainer = InkDarkText,
    tertiary = EmeraldTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = InkDarkText,
    background = StudioCanvas,
    onBackground = InkDarkText,
    surface = StudioSurface,
    onSurface = InkDarkText,
    surfaceVariant = StudioVariant,
    onSurfaceVariant = SlateSecondaryText,
    outline = StudioOutline,
    error = RoseErrorLight,
    onError = Color.White,
    errorContainer = RoseContainerLight,
    onErrorContainer = InkDarkText
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MurshedAgentTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TelemetryDarkColorScheme else StudioLightColorScheme
    val semanticColors = if (darkTheme) {
        AgentSemanticColors(
            observe = ElectricCyan,
            think = SoftViolet,
            act = EmeraldMint,
            recover = AmberRecover,
            codeBackground = ObsidianCanvas,
            codeBorder = BorderHairline
        )
    } else {
        AgentSemanticColors(
            observe = SkyPrimaryLight,
            think = VioletSecondaryLight,
            act = EmeraldTertiaryLight,
            recover = AmberRecover,
            codeBackground = StudioVariant,
            codeBorder = StudioOutline
        )
    }

    CompositionLocalProvider(LocalAgentSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
