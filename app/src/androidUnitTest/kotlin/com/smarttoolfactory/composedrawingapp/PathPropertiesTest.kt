package com.smarttoolfactory.composedrawingapp

import com.google.common.truth.Truth.assertThat
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import org.junit.Test
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

class PathPropertiesTest {

    @Test
    fun `when copying PathProperties, values should match`() {
        val original = PathProperties(
            strokeWidth = 20f,
            color = Color.Red,
            alpha = 0.5f,
            strokeCap = StrokeCap.Square,
            strokeJoin = StrokeJoin.Bevel,
            eraseMode = true
        )

        val copied = original.copy()

        assertThat(copied.strokeWidth).isEqualTo(20f)
        assertThat(copied.color).isEqualTo(Color.Red)
        assertThat(copied.alpha).isEqualTo(0.5f)
        assertThat(copied.strokeCap).isEqualTo(StrokeCap.Square)
        assertThat(copied.strokeJoin).isEqualTo(StrokeJoin.Bevel)
        assertThat(copied.eraseMode).isTrue()
    }

    @Test
    fun `when copying from another PathProperties, values should be updated`() {
        val original = PathProperties(strokeWidth = 10f)
        val other = PathProperties(strokeWidth = 50f, eraseMode = true)

        original.copyFrom(other)

        assertThat(original.strokeWidth).isEqualTo(50f)
        assertThat(original.eraseMode).isTrue()
    }
}
