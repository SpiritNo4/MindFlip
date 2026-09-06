package com.example.mindflip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavigationHelper {

    fun setup(activity: AppCompatActivity, selectedItemId: Int) {
        val navigation = activity.findViewById<BottomNavigationView>(
            R.id.bottomNavigationView
        )

        navigation.selectedItemId = selectedItemId

        navigation.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> HomeActivity::class.java
                R.id.nav_library -> LibraryActivity::class.java
                R.id.nav_progress -> ProgressActivity::class.java
                R.id.nav_search -> SearchActivity::class.java
                else -> null
            }

            if (destination == null) {
                false
            } else if (item.itemId == selectedItemId) {
                true
            } else {
                val intent = Intent(activity, destination).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }

                activity.startActivity(intent)

                // Keep Home as the return screen for the other tabs.
                if (activity !is HomeActivity) {
                    activity.finish()
                }

                true
            }
        }
    }
}