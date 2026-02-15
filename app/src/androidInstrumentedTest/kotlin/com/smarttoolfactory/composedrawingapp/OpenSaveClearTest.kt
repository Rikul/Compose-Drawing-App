package com.smarttoolfactory.composedrawingapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smarttoolfactory.composedrawingapp.data.DrawingRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Instrumented tests for Open, Save, and Clear functionality
 */
@RunWith(AndroidJUnit4::class)
class OpenSaveClearTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setUp() {
        // Wait for app to be ready
        composeTestRule.waitForIdle()
        
        // Clear database before each test to ensure clean state
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DrawingRepository(context)
        repository.clearAllData()
        
        // Clear canvas to reset in-memory app state (e.g., currentDrawingName)
        // This is important because previous tests might have loaded a drawing
        composeTestRule
            .onNodeWithContentDescription("Clear", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // If a discard dialog appears (when there are unsaved changes), click OK
        try {
            composeTestRule
                .onNodeWithText("Discard changes?")
                .assertExists()
            
            composeTestRule
                .onNodeWithText("OK")
                .performClick()
            
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // No dialog appeared, canvas was already clear
        }
    }
    
    /**
     * Helper function to create and save a test drawing
     * @param drawingName The name to save the drawing with
     */
    private fun createAndSaveDrawing(drawingName: String) {
        // Make a mark on canvas
        composeTestRule
            .onNodeWithTag("DrawingCanvas")
            .performTouchInput {
                val tapPosition = center
                down(tapPosition)
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Click Save button
        composeTestRule
            .onNodeWithContentDescription("Save")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Enter drawing name
        composeTestRule
            .onNodeWithText("Drawing name")
            .performTextInput(drawingName)
        
        composeTestRule.waitForIdle()
        
        // Click Save button in dialog
        composeTestRule
            .onNodeWithText("Save")
            .performClick()
        
        // Wait for async save operation to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Name your drawing")
                .fetchSemanticsNodes().isEmpty()
        }
        
        composeTestRule.waitForIdle()
    }
    
    // Helper function to click Open button and wait for saved drawings screen to appear
    private fun clickOpen() {
        composeTestRule
            .onNodeWithContentDescription("Open")
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    private fun clickClear() {
        composeTestRule
            .onNodeWithContentDescription("Clear", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    private fun clickSave() {
        composeTestRule
            .onNodeWithContentDescription("Save")
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun testOpenSaveClearButtonsAreVisible() {
        composeTestRule.waitForIdle()
        
        // Verify Save button is visible
        composeTestRule
            .onNodeWithContentDescription("Save")
            .assertExists("Save button should exist")
            .assertIsDisplayed()
        
        // Verify Open button is visible
        composeTestRule
            .onNodeWithContentDescription("Open")
            .assertExists("Open button should exist")
            .assertIsDisplayed()
        
        // Verify Clear button is visible
        composeTestRule
            .onNodeWithContentDescription("Clear", substring = true, ignoreCase = true)
            .assertExists("Clear Canvas button should exist")
            .assertIsDisplayed()
    }
    
    @Test
    fun testSaveDialogAppearsWithCorrectElements() {
        composeTestRule.waitForIdle()
        
        // Click Save menu button
        clickSave()

        composeTestRule.waitForIdle()
        
        // Verify "Name your drawing" dialog appears
        composeTestRule
            .onNodeWithText("Name your drawing")
            .assertExists("Dialog should display 'Name your drawing' title")
            .assertIsDisplayed()
        
        // Verify text input field exists
        composeTestRule
            .onNodeWithText("Drawing name")
            .assertExists("Text input field should exist")
            .assertIsDisplayed()
        
        // Verify Cancel button exists
        composeTestRule
            .onNodeWithText("Cancel")
            .assertExists("Cancel button should exist")
            .assertIsDisplayed()
        
        // Verify Save button exists
        composeTestRule
            .onNodeWithText("Save")
            .assertExists("Save button should exist")
            .assertIsDisplayed()
        
        // Verify Save button is disabled when name is empty
        composeTestRule
            .onNodeWithText("Save")
            .assertIsNotEnabled()
    }
    
    @Test
    fun testSubsequentSaveAutoSavesWithoutDialog() {
        composeTestRule.waitForIdle()
        
        val testDrawingName = "AutoSaveTest_${System.currentTimeMillis()}"
        
        createAndSaveDrawing(testDrawingName)

        Thread.sleep(3000)

        //  Make another mark on canvas
        composeTestRule
            .onNodeWithTag("DrawingCanvas")
            .performTouchInput {
                val tapPosition = centerLeft
                down(tapPosition)
                up()
            }
        
        composeTestRule.waitForIdle()
        
        //  Click Save again - should NOT show dialog (auto-save)
        clickSave()
        
        // Wait a bit for async save to complete
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        
        // Verify no dialog appears
        composeTestRule
            .onNodeWithText("Name your drawing")
            .assertDoesNotExist()
        
        //  Clear the canvas
        clickClear()
        
        composeTestRule.waitForIdle()

        //  Open saved drawings
        clickOpen()
        
        // Wait for saved drawings screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Saved Drawings")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.waitForIdle()
        
        //  Click on the saved drawing to load it
        composeTestRule
            .onNodeWithText(testDrawingName)
            .performClick()

        // Wait for drawing to load and saved drawings screen to close
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Saved Drawings")
                .fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.waitForIdle()

        //  Verify we're back to main screen with the drawing loaded
        composeTestRule
            .onNodeWithText("Your Saved Drawings")
            .assertDoesNotExist()

    }
    
    @Test
    fun testDuplicateDrawingNameShowsErrorDialog() {
        composeTestRule.waitForIdle()
        
        val drawingName = "DuplicateTest_${System.currentTimeMillis()}"
        
        // Step 1 & 2: Create and save a drawing
        createAndSaveDrawing(drawingName)
        
        // Step 3: Clear the canvas
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Step 4: Make another mark
        composeTestRule
            .onNodeWithTag("DrawingCanvas")
            .performTouchInput {
                val tapPosition = centerRight
                down(tapPosition)
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Step 5: Try to save with the same name
        clickSave()
        
        composeTestRule.waitForIdle()
        
        // Enter the same drawing name
        composeTestRule
            .onNodeWithText("Drawing name")
            .performTextInput(drawingName)
        
        composeTestRule.waitForIdle()
        
        // Click Save button
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        composeTestRule.waitForIdle()
        
        // Step 6: Verify "Drawing with this name already exists" dialog appears
        composeTestRule
            .onNodeWithText("Drawing with this name already exists")
            .assertExists("Error dialog should show when duplicate name is used")
            .assertIsDisplayed()
        
        // Verify OK button exists
        composeTestRule
            .onNodeWithText("OK")
            .assertExists("OK button should be present in error dialog")
            .assertIsDisplayed()
        
        // Click OK to dismiss
        composeTestRule
            .onNodeWithText("OK")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed
        composeTestRule
            .onNodeWithText("Drawing with this name already exists")
            .assertDoesNotExist()
    }
    
    @Test
    fun testOpenWithUnsavedChangesShowsDiscardDialog() {
        composeTestRule.waitForIdle()
        
        // Step 1: Make a mark on canvas (unsaved change)
        composeTestRule
            .onNodeWithTag("DrawingCanvas")
            .performTouchInput {
                val tapPosition = center
                down(tapPosition)
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Step 2: Click Open button
        clickOpen()
        
        composeTestRule.waitForIdle()
        
        // Step 3: Verify "Discard current drawing?" dialog appears
        composeTestRule
            .onNodeWithText("Discard current drawing?")
            .assertExists("Discard dialog should appear when opening with unsaved changes")
            .assertIsDisplayed()
        
        // Verify OK button exists
        composeTestRule
            .onNodeWithText("OK")
            .assertExists("OK button should be present")
            .assertIsDisplayed()
        
        // Verify Cancel button exists
        composeTestRule
            .onNodeWithText("Cancel")
            .assertExists("Cancel button should be present")
            .assertIsDisplayed()
        
        // Step 4: Click Cancel
        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()
            
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed and still on main screen
        composeTestRule
            .onNodeWithText("Discard current drawing?")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("Your Saved Drawings")
            .assertDoesNotExist()
    }
    
    @Test
    fun testOpenShowsEmptyState() {
        composeTestRule.waitForIdle()
        
        // Click Open button (no unsaved changes, no saved drawings)
        clickOpen()
        
        // Wait for saved drawings screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Saved Drawings")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify "Your Saved Drawings" screen appears
        composeTestRule
            .onNodeWithText("Your Saved Drawings")
            .assertExists("Saved drawings screen should appear")
            .assertIsDisplayed()
        
        // Verify empty message is displayed
        composeTestRule
            .onNodeWithText("There are no saved drawings")
            .assertExists("Empty message should be displayed when no drawings exist")
            .assertIsDisplayed()
    }

    @Test
    fun testOpenShowsSavedDrawingsAndLoadsDrawing() {
        composeTestRule.waitForIdle()

        val drawingName = "OpenTest_${System.currentTimeMillis()}"
        
        // Create and save a drawing
        createAndSaveDrawing(drawingName)
        
        // Clear canvas
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Click Open again
        clickOpen()

        // Wait for screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Saved Drawings")
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify saved drawings screen appears
        composeTestRule
            .onNodeWithText("Your Saved Drawings")
            .assertExists("Saved drawings screen should appear")
            .assertIsDisplayed()
        
        // Verify drawing appears in list with its name
        composeTestRule
            .onNodeWithText(drawingName)
            .assertExists("Saved drawing should appear in list")
            .assertIsDisplayed()
        
        // Verify date appears in the expected format (e.g., "Feb 14, 2026")
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val expectedDateString = dateFormat.format(Date())
        composeTestRule
            .onNodeWithText(expectedDateString, substring = true)
            .assertExists("Date should appear in format '$expectedDateString'")
            .assertIsDisplayed()
        
        // Verify empty message is NOT displayed
        composeTestRule
            .onNodeWithText("There are no saved drawings")
            .assertDoesNotExist()
        
        // Click on the drawing to load it
        composeTestRule
            .onNodeWithText(drawingName)
            .performClick()
        
        // Wait for drawing to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Your Saved Drawings")
                .fetchSemanticsNodes().isEmpty()
        }
        
        composeTestRule.waitForIdle()
        
        // Verify we're back to main screen with drawing loaded
        composeTestRule
            .onNodeWithText("Your Saved Drawings")
            .assertDoesNotExist()
        
    }
    
    @Test
    fun testDeleteDrawingShowsConfirmationAndDeletes() {
        composeTestRule.waitForIdle()
        
        val drawingName = "DeleteTest_${System.currentTimeMillis()}"
        
        //  Create and save a drawing
        createAndSaveDrawing(drawingName)
        
        // Open saved drawings screen
        clickOpen()
        
        composeTestRule.waitForIdle()
        
        // Verify drawing appears in list
        composeTestRule
            .onNodeWithText(drawingName)
            .assertExists("Drawing should appear in list")
        
        //  Click delete icon
        composeTestRule
            .onNodeWithContentDescription("Delete", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        //  Verify "Delete drawing?" confirmation dialog appears
        composeTestRule
            .onNodeWithText("Delete drawing?")
            .assertExists("Delete confirmation dialog should appear")
            .assertIsDisplayed()
        
        // Verify Yes button exists
        composeTestRule
            .onNodeWithText("Yes")
            .assertExists("Yes button should be present")
            .assertIsDisplayed()
        
        // Verify Cancel button exists
        composeTestRule
            .onNodeWithText("Cancel")
            .assertExists("Cancel button should be present")
            .assertIsDisplayed()
        
        // Click Cancel
        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed
        composeTestRule
            .onNodeWithText("Delete drawing?")
            .assertDoesNotExist()
        
        //  Verify drawing is still in the list (not deleted)
        composeTestRule
            .onNodeWithText(drawingName)
            .assertExists("Drawing should still exist after canceling delete")
        
        // Click delete icon again
        composeTestRule
            .onNodeWithContentDescription("Delete", substring = true, ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        //  This time click Yes
        composeTestRule
            .onNodeWithText("Yes")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed
        composeTestRule
            .onNodeWithText("Delete drawing?")
            .assertDoesNotExist()
        
        //  Verify drawing is no longer in the list
        composeTestRule
            .onNodeWithText(drawingName)
            .assertDoesNotExist()
        
        //  Verify empty message appears
        composeTestRule
            .onNodeWithText("There are no saved drawings")
            .assertExists("Empty message should appear after deleting last drawing")
    }
    
    @Test
    fun testClearButtonWithConfirmationDialog() {
        composeTestRule.waitForIdle()
        
        // Part 1: Test clearing with unsaved changes - should show confirmation
        
        // Make a mark on canvas (unsaved change)
        composeTestRule
            .onNodeWithTag("DrawingCanvas")
            .performTouchInput {
                val tapPosition = center
                down(tapPosition)
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Click Clear button
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Verify "Discard changes?" confirmation dialog appears
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertExists("Discard confirmation dialog should appear when clearing with unsaved changes")
            .assertIsDisplayed()
        
        // Verify text message
        composeTestRule
            .onNodeWithText("Clear canvas and start a new drawing?")
            .assertExists("Dialog message should be present")
            .assertIsDisplayed()
        
        // Verify OK button exists
        composeTestRule
            .onNodeWithText("OK")
            .assertExists("OK button should be present")
            .assertIsDisplayed()
        
        // Verify Cancel button exists
        composeTestRule
            .onNodeWithText("Cancel")
            .assertExists("Cancel button should be present")
            .assertIsDisplayed()
        
        // Click Cancel - should NOT clear canvas
        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertDoesNotExist()
        
        // Canvas should still have content (we can't directly verify the drawing,
        // but we can verify that clicking Save still shows the dialog because
        // there are still unsaved changes)
        clickSave()
        
        composeTestRule.waitForIdle()
        
        // Should show save dialog (meaning canvas is not empty)
        composeTestRule
            .onNodeWithText("Name your drawing")
            .assertExists("Save dialog should appear, meaning canvas was not cleared")
        
        // Cancel the save dialog
        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Part 2: Now click Clear again and confirm with OK
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog appears again
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertExists()
        
        // This time click OK
        composeTestRule
            .onNodeWithText("OK")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify dialog is dismissed
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertDoesNotExist()
        
        // Part 3: Test clearing empty canvas - should NOT show dialog
        
        // Canvas is now empty, clicking Clear should not show dialog
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Verify NO dialog appears
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertDoesNotExist()
        
        // Part 4: Test clearing when there are saved changes (no unsaved changes)
        
        val drawingName = "ClearTest_${System.currentTimeMillis()}"
        
        // Create and save a drawing
        createAndSaveDrawing(drawingName)
        
        // Now the drawing is saved, so there are no unsaved changes
        // Clicking Clear should not show dialog
        clickClear()
        
        composeTestRule.waitForIdle()
        
        // Verify NO dialog appears (clears immediately)
        composeTestRule
            .onNodeWithText("Discard changes?")
            .assertDoesNotExist()
    }
}
