package com.etherneco.warcaby

import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.etherneco.warcaby.game.PlayerColor

class SettingsActivity : AppCompatActivity() {

    private lateinit var startPlayerGroup: RadioGroup
    private lateinit var vsComputerSwitch: SwitchCompat
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startPlayerGroup = findViewById(R.id.startPlayerGroup)
        vsComputerSwitch = findViewById(R.id.vsComputerSwitch)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val startColor = prefs.getString(KEY_START_COLOR, PlayerColor.WHITE.name) ?: PlayerColor.WHITE.name
        val vsComputer = prefs.getBoolean(KEY_VS_COMPUTER, false)

        startPlayerGroup.check(
            if (startColor == PlayerColor.BLACK.name) {
                R.id.startBlackRadio
            } else {
                R.id.startWhiteRadio
            }
        )
        vsComputerSwitch.isChecked = vsComputer

        startPlayerGroup.setOnCheckedChangeListener { _, checkedId ->
            val color = if (checkedId == R.id.startBlackRadio) {
                PlayerColor.BLACK.name
            } else {
                PlayerColor.WHITE.name
            }
            prefs.edit().putString(KEY_START_COLOR, color).apply()
        }

        vsComputerSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_VS_COMPUTER, isChecked).apply()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val PREFS_NAME = "warcaby_settings"
        const val KEY_START_COLOR = "start_color"
        const val KEY_VS_COMPUTER = "vs_computer"
    }
}
