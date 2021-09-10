/*
package com.knesarcreation.playbeat.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.knesarcreation.playbeat.R

class HomePage : AppCompatActivity() {
    lateinit var cvAllSongs: CardView
    lateinit var cvFav: CardView
    lateinit var arrowBackIV: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        initialization()

        cvAllSongs.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }

        arrowBackIV.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initialization() {
        cvAllSongs = findViewById(R.id.cvAllSongs)
        cvFav = findViewById(R.id.cvFav)
        arrowBackIV = findViewById(R.id.arrowBackIV)
    }
}*/
