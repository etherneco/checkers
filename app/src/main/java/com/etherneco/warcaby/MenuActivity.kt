package com.etherneco.warcaby

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<MaterialButton>(R.id.buttonStart).setOnClickListener {
            startGame(newGame = true)
        }
        findViewById<MaterialButton>(R.id.buttonContinue).setOnClickListener {
            startGame(newGame = false)
        }
        findViewById<MaterialButton>(R.id.buttonHowTo).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }
    }

    private fun startGame(newGame: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        if (newGame) {
            intent.putExtra(MainActivity.EXTRA_NEW_GAME, true)
        } else {
            intent.putExtra(MainActivity.EXTRA_CONTINUE, true)
        }
        startActivity(intent)
    }
}
