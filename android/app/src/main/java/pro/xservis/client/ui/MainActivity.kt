package pro.xservis.client.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import pro.xservis.client.ui.nav.RootNavGraph
import pro.xservis.client.ui.theme.XservisTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restore the regular (non-splash) theme
        setTheme(pro.xservis.client.R.style.Theme_Xservis)
        setContent {
            App()
        }
    }
}

@Composable
private fun App() {
    XservisTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            RootNavGraph()
        }
    }
}
