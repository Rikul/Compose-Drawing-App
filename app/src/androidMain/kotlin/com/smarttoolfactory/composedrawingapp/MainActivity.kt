package com.smarttoolfactory.composedrawingapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.smarttoolfactory.composedrawingapp.data.DrawingRepository
import com.smarttoolfactory.composedrawingapp.model.DrawingStroke
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.ui.theme.ComposeDrawingAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

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

    val context = LocalContext.current
    val repository = remember { DrawingRepository(context) }
    val scope = rememberCoroutineScope()
    
    val paths = remember { mutableStateListOf<Pair<Path, PathProperties>>() }
    val pathsUndone = remember { mutableStateListOf<Pair<Path, PathProperties>>() }
    val strokeList = remember { mutableStateListOf<DrawingStroke>() }
    val strokeListUndone = remember { mutableStateListOf<DrawingStroke>() }
    val currentPathPropertyState = remember { mutableStateOf(PathProperties()) }

    // Restore
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val savedStrokes = repository.getStrokes()
            val savedPref = repository.getPreference()
            withContext(Dispatchers.Main) {
                 if (savedPref != null) {
                     currentPathPropertyState.value = savedPref
                 }
                 if (savedStrokes.isNotEmpty()) {
                     strokeList.clear()
                     strokeList.addAll(savedStrokes)
                     paths.clear()
                     savedStrokes.forEach { stroke ->
                         val p = Path()
                         if (stroke.points.isNotEmpty()) {
                             p.moveTo(stroke.points[0].x, stroke.points[0].y)
                             if (stroke.points.size > 1) {
                                 for (i in 1 until stroke.points.size) {
                                     p.lineTo(stroke.points[i].x, stroke.points[i].y)
                                 }
                             }
                         }
                         paths.add(Pair(p, stroke.pathProperties))
                     }
                 }
            }
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val strokes = strokeList.toList()
                val pref = currentPathPropertyState.value
                scope.launch(Dispatchers.IO) {
                    repository.saveStrokes(strokes)
                    repository.savePreference(pref)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                    DrawingAppBar(onExport = {
                        val widthFn = with(density) { configuration.screenWidthDp.dp.toPx().toInt() }
                        val heightFn = with(density) { configuration.screenHeightDp.dp.toPx().toInt() }
                        saveBitmap(context, paths.toList(), widthFn, heightFn)
                    })
                }
            ) { paddingValues: PaddingValues ->
                DrawingApp(
                    paddingValues,
                    paths,
                    pathsUndone,
                    strokeList,
                    strokeListUndone,
                    currentPathPropertyState
                )
            }
    }
}

@Composable
fun DrawingAppBar(onExport: () -> Unit = {}) {
    TopAppBar(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        title = {
            Text("DrawIt", color = MaterialTheme.colors.onPrimary)
        },
        actions = {
           IconButton(onClick = onExport) {
               Icon(Icons.Filled.Save, contentDescription = "Export PNG")
           }
        }
    )
}

fun saveBitmap(context: Context, paths: List<Pair<Path, PathProperties>>, width: Int, height: Int) {
    try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        paths.forEach { (path, props) ->
             val paint = android.graphics.Paint().apply {
                 color = props.color.toArgb()
                 strokeWidth = props.strokeWidth
                 alpha = (props.alpha * 255).toInt()
                 style = android.graphics.Paint.Style.STROKE
                 strokeCap = when(props.strokeCap) { 
                     StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
                     StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
                     else -> android.graphics.Paint.Cap.SQUARE
                 }
                 strokeJoin = when(props.strokeJoin) {
                     StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
                     StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
                     else -> android.graphics.Paint.Join.BEVEL
                 }
                 isAntiAlias = true
             }
             if (props.eraseMode) {
                 paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
             }
             canvas.drawPath(path.asAndroidPath(), paint)
        }

        val filename = "Drawing_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeDrawingAppTheme {
        MainScreen()
    }
}
