package acr.browser.presearch.settings.fragment

import acr.browser.presearch.R
import acr.browser.presearch.di.injector
import acr.browser.presearch.extensions.snackbar
import acr.browser.presearch.preference.DeveloperPreferences
import android.os.Bundle
import javax.inject.Inject

class DebugSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var developerPreferences: DeveloperPreferences

    override fun providePreferencesXmlResource() = R.xml.preference_debug

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        togglePreference(
            preference = LEAK_CANARY,
            isChecked = developerPreferences.useLeakCanary,
            onCheckChange = { change ->
                activity?.snackbar(R.string.app_restart)
                developerPreferences.useLeakCanary = change
            }
        )
    }

    companion object {
        private const val LEAK_CANARY = "leak_canary_enabled"
    }
}
