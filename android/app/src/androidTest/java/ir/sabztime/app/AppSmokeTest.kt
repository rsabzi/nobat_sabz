package ir.sabztime.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** دود-تست: اپلیکیشن بدون crash باز می‌شود و صفحه ورود (یا اصلی) رندر می‌شود. */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndRenders() {
        composeRule.onRoot().assertIsDisplayed()
        // یا صفحه ورود، یا (در صورت نشست قبلی) صفحه اصلی؛ فقط مطمئن می‌شویم UI هست.
        val loginButtons = composeRule.onAllNodesWithText("ورود به نوبت‌سبز")
        if (loginButtons.fetchSemanticsNodes().isNotEmpty()) {
            loginButtons[0].performClick()
            composeRule.onRoot().assertIsDisplayed()
        }
    }
}
