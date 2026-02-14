package com.smarttoolfactory.composedrawingapp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.composedrawingapp.ui.theme.ComposeDrawingAppTheme
import org.junit.Rule
import org.junit.Test

class DrawingAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun drawingApp_initialState_showsAllMenuIcons() {
        composeTestRule.setContent {
            ComposeDrawingAppTheme {
                DrawingApp(paddingValues = PaddingValues(0.dp))
            }
        }

        // Check if all menu buttons are displayed
        composeTestRule.onNodeWithTag("DrawModeTouch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("DrawModeErase").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ColorSelection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PropertiesDialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("Undo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("Redo").assertIsDisplayed()
    }

    @Test
    fun drawingApp_clickingColorSelection_showsColorDialog() {
        composeTestRule.setContent {
            ComposeDrawingAppTheme {
                DrawingApp(paddingValues = PaddingValues(0.dp))
            }
        }

        // Click on Color Selection
        composeTestRule.onNodeWithTag("ColorSelection").performClick()

        // Check if Color Dialog is shown (assuming it has "Color" text)
        composeTestRule.onNodeWithText("Color").assertIsDisplayed()
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun drawingApp_clickingPropertiesDialog_showsPropertiesDialog() {
        composeTestRule.setContent {
            ComposeDrawingAppTheme {
                DrawingApp(paddingValues = PaddingValues(0.dp))
            }
        }

        // Click on Properties Dialog
        composeTestRule.onNodeWithTag("PropertiesDialog").performClick()

        // Check if Properties Dialog is shown
        composeTestRule.onNodeWithText("Properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stroke Width 10").assertIsDisplayed()
    }
}
