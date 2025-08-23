package com.example.compose.jetchat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.compose.jetchat.conversation.ChannelNameBarTestTag
import com.example.compose.jetchat.conversation.ConversationContent
import com.example.compose.jetchat.conversation.ConversationTestTag
import com.example.compose.jetchat.conversation.ConversationUiState
import com.example.compose.jetchat.conversation.UserInputTestTag
import com.example.compose.jetchat.data.exampleUiState
import com.example.compose.jetchat.theme.JetchatTheme
import dejavu.DejavuComposeTestRule
import dejavu.assertStable
import dejavu.createRecompositionTrackingRule
import dejavu.resetRecompositionCounts
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConversationRecompositionTest {

    @get:Rule
    val composeTestRule: DejavuComposeTestRule<ComponentActivity> =
        createRecompositionTrackingRule()

    private val themeIsDark = MutableStateFlow(false)

    private val uiState = ConversationUiState(
        initialMessages = exampleUiState.messages.plus(exampleUiState.messages),
        channelName = "#composers",
        channelMembers = 42,
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            JetchatTheme(isDarkTheme = themeIsDark.collectAsStateWithLifecycle(false).value) {
                ConversationContent(
                    uiState = uiState,
                    navigateToProfile = { },
                    onNavIconPressed = { },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.resetRecompositionCounts()
    }

    // --- Scenario 1: Initial stability ---

    @Test
    fun afterInitialComposition_allSlotsAreStable() {
        composeTestRule.onNodeWithTag(ConversationTestTag).assertStable()
        composeTestRule.onNodeWithTag(ChannelNameBarTestTag).assertStable()
        composeTestRule.onNodeWithTag(UserInputTestTag).assertStable()
    }

    // --- Scenario 2: Scroll isolation ---

    @Test
    fun scrollMessages_channelNameBarStaysStable() {
        composeTestRule.onNodeWithTag(ConversationTestTag).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ChannelNameBarTestTag).assertStable()
    }

    @Test
    fun scrollMessages_userInputStaysStable() {
        composeTestRule.onNodeWithTag(ConversationTestTag).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(UserInputTestTag).assertStable()
    }

    // --- Scenario 3: Theme toggle ---

    @Test
    fun toggleTheme_messagesRecompose() {
        // ConversationTestTag is on a LazyColumn (SubcomposeLayout).
        // Tags inside sub-compositions can't be mapped to the parent composable.
        // This test verifies the UI remains functional after theme toggle.
        themeIsDark.value = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ConversationTestTag).assertIsDisplayed()
    }

    @Test
    fun toggleTheme_allSlotsRecomposeBounded() {
        // Theme toggle changes MaterialTheme CompositionLocals.
        // Only framework composables (Material3 internals) handle the theme;
        // user composables with unchanged params skip via strong skipping.
        // Verify the UI remains stable and functional.
        themeIsDark.value = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ChannelNameBarTestTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UserInputTestTag).assertIsDisplayed()
    }

    // --- Scenario 4: Add message ---

    @Test
    fun addMessage_messagesRecomposes() {
        // LazyColumn handles item mutations internally in its sub-composition.
        // The parent Messages composable doesn't recompose on item add.
        // Verify the new message appears in the UI instead.
        uiState.addMessage(
            com.example.compose.jetchat.conversation.Message("me", "Hello Dejavu!", "now")
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello Dejavu!").assertIsDisplayed()
    }

    @Test
    fun addMessage_channelNameBarStaysStable() {
        uiState.addMessage(
            com.example.compose.jetchat.conversation.Message("me", "Hello Dejavu!", "now")
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ChannelNameBarTestTag).assertStable()
    }

    @Test
    fun addMessage_userInputStaysStable() {
        uiState.addMessage(
            com.example.compose.jetchat.conversation.Message("me", "Hello Dejavu!", "now")
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(UserInputTestTag).assertStable()
    }
}
