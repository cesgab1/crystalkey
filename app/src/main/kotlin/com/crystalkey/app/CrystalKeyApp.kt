package com.crystalkey.app

import android.app.Application
import android.content.Context

/**
 * No analytics, no crash reporter, no ad network, no remote config — the
 * privacy claim on the Family Controls screen is only true if nothing is
 * initialised here, and this class exists to make that visible in review.
 *
 * The one exception is deliberate and stays on the device: the last fatal
 * exception is written to local preferences so the next launch can show it on
 * screen. Without it, a crash during startup is just a black rectangle, which
 * tells nobody anything. Nothing is ever transmitted.
 */
class CrystalKeyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, error.stackTraceToString().take(8000))
                    .commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val PREFS = "crystalkey.diagnostics"
        const val KEY_LAST_CRASH = "last_crash"

        fun lastCrash(context: Context): String? =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CRASH, null)

        fun clearLastCrash(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_LAST_CRASH).commit()
        }
    }
}
