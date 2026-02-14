package com.smarttoolfactory.composedrawingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.smarttoolfactory.composedrawingapp.ui.theme.ComposeDrawingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ComposeDrawingAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                DrawingAppBar()
            }
        ) { paddingValues: PaddingValues ->
            DrawingApp(paddingValues)
        }
    }
}

@Composable
fun DrawingAppBar() {
    TopAppBar(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        title = {
            Text("DrawIt", color = MaterialTheme.colors.onPrimary)
        },
        actions = {
           
           
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeDrawingAppTheme {
        MainScreen()
    }
}
