package com.meenbeese.chronos.activities

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings

import androidx.fragment.app.FragmentManager

import com.afollestad.aesthetic.AestheticActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.Chronos.ActivityListener
import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.PreferenceData
import com.meenbeese.chronos.fragments.BaseFragment
import com.meenbeese.chronos.fragments.HomeFragment
import com.meenbeese.chronos.fragments.SplashFragment
import com.meenbeese.chronos.fragments.StopwatchFragment
import com.meenbeese.chronos.fragments.TimerFragment
import com.meenbeese.chronos.receivers.TimerReceiver

import java.lang.ref.WeakReference


class MainActivity : AestheticActivity(), FragmentManager.OnBackStackChangedListener,
    ActivityListener {
    private var chronos: Chronos? = null
    private var fragmentRef: WeakReference<BaseFragment?>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chronos = applicationContext as Chronos
        chronos?.setListener(this)
        if (savedInstanceState == null) {
            val fragment = createFragmentFor(intent) ?: return
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment, fragment)
                .commit()
            fragmentRef = WeakReference(fragment)
        } else {
            var fragment: BaseFragment? = TimerFragment()
            if (fragmentRef == null || fragmentRef?.get()
                    .also { fragment = it } == null
            ) fragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment!!)
                .commit()
            fragmentRef = WeakReference(fragment)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)

        // Background permissions info
        if (!PreferenceData.INFO_BACKGROUND_PERMISSIONS.getValue(this, false)) {
            val alert = MaterialAlertDialogBuilder(this, if(chronos!!.isDarkTheme()) com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert else com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert)
            alert.setTitle(getString(R.string.info_background_permissions_title))
            alert.setMessage(getString(R.string.info_background_permissions_body))
            alert.setPositiveButton(applicationContext.getString(android.R.string.ok)){_, _ ->  PreferenceData.INFO_BACKGROUND_PERMISSIONS.setValue(this@MainActivity, true)
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))}
            alert.setNegativeButton(applicationContext.getString(android.R.string.cancel),  null)
            alert.show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isActionableIntent(intent)) {
            val manager = supportFragmentManager
            val newFragment = createFragmentFor(intent)
            val fragment = if (fragmentRef != null) fragmentRef?.get() else null
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
            fragmentRef = WeakReference(newFragment)
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
        val fragment = if (fragmentRef != null) fragmentRef?.get() else null
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
                if (Intent.ACTION_MAIN == intent.action || intent.action == null) return SplashFragment()
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
        fragmentRef = WeakReference(fragment)
    }

    override fun fetchFragmentManager(): FragmentManager {
        return supportFragmentManager
    }

    companion object {
        const val EXTRA_FRAGMENT = "com.meenbeese.chronos.MainActivity.EXTRA_FRAGMENT"
        const val FRAGMENT_TIMER = 0
        const val FRAGMENT_STOPWATCH = 2
    }
}
