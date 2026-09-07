package com.citta.driver.data.shift

import android.content.Context
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.shift.LocalShiftStore

/**
 * [LocalShiftStore] backed by a small plain `SharedPreferences` file. The shift intent is not a
 * secret, so it does not use the encrypted store. Thin Android seam, no unit test — the
 * reconcile logic it feeds is covered by `ShiftReconcilerTest` / `HomeViewModelShiftRearmTest`.
 */
class PrefsLocalShiftStore(context: Context) : LocalShiftStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override fun desiredShiftState(): ShiftState =
        ShiftState.fromWire(prefs.getString(KEY_STATE, null))

    override fun setDesiredShiftState(value: ShiftState) {
        prefs.edit().putString(KEY_STATE, value.wire).apply()
    }

    private companion object {
        const val PREFS_FILE = "citta_shift_state"
        const val KEY_STATE = "desired_shift_state"
    }
}
