package com.fennecstero

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.Toolbar


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setTitle("Settings");
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true); // Show the back button

        window.statusBarColor = resources.getColor(android.R.color.black)
        // Set custom back button icon with white color
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_back_arrow, null)
        toolbar.setNavigationOnClickListener { onBackPressed() } // Handle back button click
        borderToggle(true);
        separatePhotosToggle(true);
        flashToggle(true);
        shutterSoundToggle(true);
        gridToggle(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() // Handle back button click here
        return true
    }

    public fun borderToggleView(view: View) {
        borderToggle(false)
    }
    public fun flashToggleView(view: View) {
        flashToggle(false)
    }
    public fun shutterSoundToggleView(view: View) {
        shutterSoundToggle(false)
    }
    public fun separatePhotosToggleView(view: View) {
        separatePhotosToggle(false)
    }

    public fun gridToggleView(view: View) {
        gridToggle(false)
    }



    public fun borderToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isBorderOn = sharedPreferences.getBoolean("borderOn", false)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isBorderOn = !isBorderOn
            editor.putBoolean("borderOn", isBorderOn)
            editor.apply()
        }
        findViewById<Switch>(R.id.border_btn).setChecked(isBorderOn);
    }

    public fun flashToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isBorderOn = sharedPreferences.getBoolean("flashEnabled", true)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isBorderOn = !isBorderOn
            editor.putBoolean("flashEnabled", isBorderOn)
            editor.apply()
        }
        findViewById<Switch>(R.id.flashOnBtn).setChecked(isBorderOn);
    }

    public fun shutterSoundToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isBorderOn = sharedPreferences.getBoolean("shutterSound", false)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isBorderOn = !isBorderOn
            editor.putBoolean("shutterSound", isBorderOn)
            editor.apply()
        }
        findViewById<Switch>(R.id.shutterSoundBtn).setChecked(isBorderOn);
    }

    public fun separatePhotosToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isBorderOn = sharedPreferences.getBoolean("separatePhotos", false)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isBorderOn = !isBorderOn
            editor.putBoolean("separatePhotos", isBorderOn)
            editor.apply()
        }
        findViewById<Switch>(R.id.separateBtn).setChecked(isBorderOn);
    }

    public fun gridToggle(readOnly: Boolean) {
        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        var isGridOn = sharedPreferences.getBoolean("grid", false)
        // Save the toggle state to SharedPreferences
        if (!readOnly) {
            isGridOn = !isGridOn
            editor.putBoolean("grid", isGridOn)
            editor.apply()
        }
        findViewById<Switch>(R.id.grid_btn).setChecked(isGridOn);
    }

}