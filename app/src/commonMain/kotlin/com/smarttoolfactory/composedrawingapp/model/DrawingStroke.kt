package com.smarttoolfactory.composedrawingapp.model

import androidx.compose.ui.geometry.Offset

data class DrawingStroke(
    val pathProperties: PathProperties,
    val points: List<Offset>
)
