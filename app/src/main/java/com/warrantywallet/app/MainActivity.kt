package com.warrantywallet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.warrantywallet.app.core.designsystem.WarrantyWalletTheme
import com.warrantywallet.app.core.model.AppThemeMode
import com.warrantywallet.app.ui.navigation.MainAppNavigation

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install modern AndroidX splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WarrantyWalletApp

        setContent {
            val themeMode by app.preferenceManager.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)

            WarrantyWalletTheme(themeMode = themeMode) {
                MainAppNavigation(app = app)
            }
        }
    }
}
