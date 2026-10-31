package app.ghmobile

import android.content.Context
import android.content.SharedPreferences

object Store {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (!::sp.isInitialized) {
            sp = c.applicationContext.getSharedPreferences("gh", Context.MODE_PRIVATE)
        }
    }

    var token: String
        get() = sp.getString("token", "").orEmpty()
        set(v) {
            sp.edit().putString("token", v).apply()
        }

    var clientId: String
        get() = sp.getString("cid", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.GH_CLIENT_ID
        set(v) {
            sp.edit().putString("cid", v.trim()).apply()
        }
}
