package pro.xservis.client.ui.theme

import android.app.Activity
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val XservisDarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = BgBlack,
    primaryContainer = TealDim,
    onPrimaryContainer = TextPrimary,
    secondary = TealBright,
    onSecondary = BgBlack,
    secondaryContainer = SurfaceGreyElev,
    onSecondaryContainer = TextPrimary,
    tertiary = TealBright,
    background = BgBlack,
    onBackground = TextPrimary,
    surface = BgCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGrey,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceGrey,
    surfaceContainerHigh = SurfaceGreyElev,
    surfaceContainerHighest = SurfaceGreyElev,
    outline = DividerGrey,
    outlineVariant = DividerGrey,
    error = DangerRed,
    onError = TextPrimary,
)

@Composable
fun XservisTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Always dark — by design
    val colorScheme = XservisDarkColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity
            if (activity != null) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(BgBlack.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(BgBlack.toArgb()),
                )
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = XservisTypography,
        content = content,
    )
}
