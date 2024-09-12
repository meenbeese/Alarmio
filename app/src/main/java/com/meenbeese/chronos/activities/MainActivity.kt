package com.meenbeese.chronos.activities

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.WindowManager

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentManager

import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.Chronos.ActivityListener
import com.meenbeese.chronos.Chronos.Companion.THEME_AMOLED
import com.meenbeese.chronos.Chronos.Companion.THEME_DAY
import com.meenbeese.chronos.Chronos.Companion.THEME_NIGHT
import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.PreferenceData
import com.meenbeese.chronos.dialogs.BackgroundPermissionsDialog
import com.meenbeese.chronos.fragments.BaseFragment
import com.meenbeese.chronos.fragments.HomeFragment
import com.meenbeese.chronos.fragments.StopwatchFragment
import com.meenbeese.chronos.fragments.TimerFragment
import com.meenbeese.chronos.receivers.TimerReceiver


class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener,
    ActivityListener {
    private var chronos: Chronos? = null
    private var fragmentRef: BaseFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        applySavedTheme()
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        chronos = applicationContext as Chronos
        chronos?.setListener(this)
        if (savedInstanceState == null) {
            val fragment = createFragmentFor(intent) ?: return
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment, fragment)
                .commit()
            fragmentRef = fragment
        } else {
            var fragment: BaseFragment? = TimerFragment()
            if (fragmentRef == null) fragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment!!)
                .commit()
            fragmentRef = fragment
        }
        supportFragmentManager.addOnBackStackChangedListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = supportFragmentManager
                if (fragmentManager.backStackEntryCount > 0) {
                    fragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        // Background permissions info
        if (!PreferenceData.INFO_BACKGROUND_PERMISSIONS.getValue(this, false)) {
            val backgroundPermissionsDialog = BackgroundPermissionsDialog(this)
            backgroundPermissionsDialog.show()
        }
    }

    private fun applySavedTheme() {
        val theme = chronos?.activityTheme
        Log.d("MAIN", "Theme: $theme")

        when (theme) {
            THEME_DAY -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.AppTheme)
            }
            THEME_NIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.AppTheme_Night)
            }
            THEME_AMOLED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.AppTheme_Amoled)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isActionableIntent(intent)) {
            val manager = supportFragmentManager
            val newFragment = createFragmentFor(intent)
            val fragment = if (fragmentRef != null) fragmentRef else null
            if (newFragment == null || newFragment == fragment) // Check that fragment isn't already displayed
                return
            if (newFragment is HomeFragment && manager.backStackEntryCount > 0) // Clear the back stack
                manager.popBackStack(
                    manager.getBackStackEntryAt(0).id,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            val transaction = manager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_up_sheet,
                    R.anim.slide_out_up_sheet,
                    R.anim.slide_in_down_sheet,
                    R.anim.slide_out_down_sheet
                )
                .replace(R.id.fragment, newFragment)
            if (fragment is HomeFragment && newFragment !is HomeFragment) transaction.addToBackStack(
                null
            )
            fragmentRef = newFragment
            transaction.commit()
        }
    }

    /**
     * Return a fragment to display the content provided by
     * the passed intent.
     *
     * @param intent    The intent passed to the activity.
     * @return          An instantiated fragment corresponding
     * to the passed intent.
     */
    private fun createFragmentFor(intent: Intent): BaseFragment? {
        val fragment = if (fragmentRef != null) fragmentRef else null
        return when (intent.getIntExtra(EXTRA_FRAGMENT, -1)) {
            FRAGMENT_STOPWATCH -> {
                fragment as? StopwatchFragment ?: StopwatchFragment()
            }

            FRAGMENT_TIMER -> {
                if (intent.hasExtra(TimerReceiver.EXTRA_TIMER_ID)) {
                    val id = intent.getIntExtra(TimerReceiver.EXTRA_TIMER_ID, 0)
                    if (chronos!!.timers.size <= id || id < 0) return fragment
                    val args = Bundle()
                    args.putParcelable(TimerFragment.EXTRA_TIMER, chronos!!.timers[id])
                    val newFragment: BaseFragment = TimerFragment()
                    newFragment.arguments = args
                    return newFragment
                }
                fragment
            }

            else -> {
//                if (Intent.ACTION_MAIN == intent.action || intent.action == null) return SplashFragment()
                val args = Bundle()
                args.putString(HomeFragment.INTENT_ACTION, intent.action)
                val newFragment: BaseFragment = HomeFragment()
                newFragment.arguments = args
                newFragment
            }
        }
    }

    /**
     * Determine if something needs to be done as a result
     * of the intent being sent to the activity - which has
     * a higher priority than any fragment that is currently
     * open.
     *
     * @param intent    The intent passed to the activity.
     * @return          True if a fragment should be replaced
     * with the action that this intent entails.
     */
    private fun isActionableIntent(intent: Intent): Boolean {
        return intent.hasExtra(EXTRA_FRAGMENT)
                || AlarmClock.ACTION_SHOW_ALARMS == intent.action
                || AlarmClock.ACTION_SET_TIMER == intent.action
                || AlarmClock.ACTION_SET_ALARM == intent.action
    }

    public override fun onDestroy() {
        super.onDestroy()
        chronos?.setListener(null)
        chronos = null
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    public override fun onPause() {
        super.onPause()
        chronos?.stopCurrentSound()
    }

    override fun onBackStackChanged() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as BaseFragment?
        fragmentRef = fragment
    }

    override fun fetchFragmentManager(): FragmentManager {
        return supportFragmentManager
    }

    override fun getActivity(): AppCompatActivity {
        return this
    }

    companion object {
        const val EXTRA_FRAGMENT = "com.meenbeese.chronos.MainActivity.EXTRA_FRAGMENT"
        const val FRAGMENT_TIMER = 0
        const val FRAGMENT_STOPWATCH = 2
    }
}
