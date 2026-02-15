package com.smarttoolfactory.composedrawingapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.smarttoolfactory.composedrawingapp.data.DrawingRepository
import com.smarttoolfactory.composedrawingapp.model.DrawingStroke
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.model.SavedDrawing
import com.smarttoolfactory.composedrawingapp.ui.theme.ComposeDrawingAppTheme
import com.smarttoolfactory.composedrawingapp.ui.dialogs.*
import com.smarttoolfactory.composedrawingapp.ui.screens.SavedDrawingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    
    // Navigation and drawing state
    var showSavedDrawingsScreen by remember { mutableStateOf(false) }
    var currentDrawingName by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var savedDrawings by remember { mutableStateOf<List<SavedDrawing>>(emptyList()) }
    var initialStrokeCount by remember { mutableStateOf(0) }
    
    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDrawingExistsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showOpenDiscardDialog by remember { mutableStateOf(false) }

    // Restore
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val savedStrokes = repository.getStrokes()
            val (savedPref, savedDrawingName) = repository.getPreference()
            withContext(Dispatchers.Main) {
                 if (savedPref != null) {
                     currentPathPropertyState.value = savedPref
                 }
                 currentDrawingName = savedDrawingName
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
                             } else {
                                 // Single point - draw a small line to make it visible as a dot
                                 p.lineTo(stroke.points[0].x + 0.1f, stroke.points[0].y + 0.1f)
                             }
                         }
                         paths.add(Pair(p, stroke.pathProperties))
                     }
                 }
                 initialStrokeCount = strokeList.size
                 hasUnsavedChanges = false
            }
        }
    }
    
    // Track changes
    LaunchedEffect(strokeList.size) {
        if (strokeList.size != initialStrokeCount) {
            hasUnsavedChanges = true
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val strokes = strokeList.toList()
                val pref = currentPathPropertyState.value
                val drawingName = currentDrawingName
                scope.launch(Dispatchers.IO) {
                    repository.saveStrokes(strokes)
                    repository.savePreference(pref, drawingName)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Helper functions
    fun loadDrawing(drawing: SavedDrawing) {
        scope.launch(Dispatchers.IO) {
            val strokes = repository.loadDrawing(drawing.id)
            withContext(Dispatchers.Main) {
                currentDrawingName = drawing.name
                hasUnsavedChanges = false
                strokeList.clear()
                strokeList.addAll(strokes)
                paths.clear()
                strokes.forEach { stroke ->
                    val p = Path()
                    if (stroke.points.isNotEmpty()) {
                        p.moveTo(stroke.points[0].x, stroke.points[0].y)
                        if (stroke.points.size > 1) {
                            for (i in 1 until stroke.points.size) {
                                p.lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        } else {
                            // Single point - draw a small line to make it visible as a dot
                            p.lineTo(stroke.points[0].x + 0.1f, stroke.points[0].y + 0.1f)
                        }
                    }
                    paths.add(Pair(p, stroke.pathProperties))
                }
                initialStrokeCount = strokeList.size
                showSavedDrawingsScreen = false
            }
        }
    }
    
    fun saveCurrentDrawing(name: String) {
        scope.launch(Dispatchers.IO) {
            val success = repository.saveDrawing(name, strokeList.toList())
            withContext(Dispatchers.Main) {
                if (success) {
                    currentDrawingName = name
                    hasUnsavedChanges = false
                    initialStrokeCount = strokeList.size
                    Toast.makeText(context, "Drawing saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error saving drawing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun clearCanvas() {
        paths.clear()
        pathsUndone.clear()
        strokeList.clear()
        strokeListUndone.clear()
        currentDrawingName = null
        hasUnsavedChanges = false
        initialStrokeCount = 0
    }

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        if (showSavedDrawingsScreen) {
            SavedDrawingsScreen(
                savedDrawings = savedDrawings,
                onBack = { showSavedDrawingsScreen = false },
                onDrawingClick = { drawing -> loadDrawing(drawing) },
                onDeleteDrawing = { drawing ->
                    scope.launch(Dispatchers.IO) {
                        repository.deleteDrawing(drawing.id)
                        savedDrawings = repository.getAllSavedDrawings()
                    }
                }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                topBar = {
                    DrawingAppBar(
                        drawingName = currentDrawingName,
                        hasUnsavedChanges = hasUnsavedChanges,
                        onSave = {
                            if (currentDrawingName != null) {
                                saveCurrentDrawing(currentDrawingName!!)
                            } else {
                                showSaveDialog = true
                            }
                        },
                        onOpen = {
                            scope.launch(Dispatchers.IO) {
                                val list = repository.getAllSavedDrawings()
                                withContext(Dispatchers.Main) {
                                    savedDrawings = list
                                    if (hasUnsavedChanges) {
                                        showOpenDiscardDialog = true
                                    } else {
                                        showSavedDrawingsScreen = true
                                    }
                                }
                            }
                        },
                        onClear = {
                            if (strokeList.isEmpty() || !hasUnsavedChanges) {
                                clearCanvas()
                            } else {
                                showDiscardDialog = true
                            }
                        }
                    )
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
    
    // Dialogs
    if (showSaveDialog) {
        SaveDrawingDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                saveCurrentDrawing(name)
                showSaveDialog = false
            },
            onNameExists = {
                showSaveDialog = false
                showDrawingExistsDialog = true
            },
            checkNameExists = { name ->
                repository.drawingNameExists(name)
            }
        )
    }
    
    if (showDrawingExistsDialog) {
        DrawingExistsDialog(
            onDismiss = {
                showDrawingExistsDialog = false
                showSaveDialog = true
            }
        )
    }
    
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Clear canvas and start a new drawing?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        clearCanvas()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOpenDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = {
                showOpenDiscardDialog = false
                showSavedDrawingsScreen = true
            },
            onCancel = {
                showOpenDiscardDialog = false
            }
        )
    }
}

@Composable
fun DrawingAppBar(
    drawingName: String?,
    hasUnsavedChanges: Boolean,
    onSave: () -> Unit = {},
    onOpen: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    TopAppBar(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        title = {
            Column {
                Text(
                    text = "DrawIt",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onPrimary
                )
                Text(
                    text = when {
                        drawingName != null && hasUnsavedChanges -> "$drawingName *"
                        drawingName != null -> drawingName
                        else -> "untitled"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        },
        actions = {
           IconButton(onClick = onSave) {
               Icon(Icons.Filled.Save, contentDescription = "Save")
           }
           IconButton(onClick = onOpen) {
               Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
           }
           IconButton(onClick = onClear) {
               Icon(Icons.Filled.Delete, contentDescription = "Clear Canvas")
           }
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