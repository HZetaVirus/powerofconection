package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Power of Connection", appName)
  }

  @Test
  fun testViewModelInitialization() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.MainViewModel(app)
    org.junit.Assert.assertNotNull(vm)
  }

  @Test
  fun testMainAppComposition() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.MainViewModel(app)
    val darkState = androidx.compose.runtime.mutableStateOf(false)
    composeTestRule.setContent {
      com.example.ui.MainApp(viewModel = vm, darkThemeState = darkState)
    }
    composeTestRule.waitForIdle()
  }
}
