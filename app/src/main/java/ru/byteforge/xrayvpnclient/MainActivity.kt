package ru.byteforge.xrayvpnclient


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import ru.byteforge.xrayvpnclient.ui.theme.GoydaTheme
import ru.byteforge.xrayvpnclient.ui.theme.TestVpnRunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoydaTheme {
                RootScreen()
            }
        }
//        NetworkBackgroundGenerator().exportBackground(
//            context = this,
//            lifecycleScope = lifecycleScope,
//            width = 1920,
//            height = 1080
//        )
    }
}

@Composable
fun RootScreen() {
    val context = LocalContext.current

    MainScreen()

}

@Preview(showBackground = true)
@Composable
fun RootScreenPreview() {
    TestVpnRunTheme {
        RootScreen()
    }
}