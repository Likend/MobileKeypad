package indi.likend.mobilekeypad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import indi.likend.mobilekeypad.ui.MobileKeypadApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileKeypadApp()
        }
    }

    override fun onStop() {
        super.onStop()
    }
}
