package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.BackupViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HomeVault Photos", appName)
  }

  @Test
  fun `instantiate backup view model`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val viewModel = BackupViewModel(context)
    assertNotNull(viewModel)
  }
}
