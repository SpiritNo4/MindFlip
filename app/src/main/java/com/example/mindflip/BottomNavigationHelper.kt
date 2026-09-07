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

        fun navigate(itemId: Int): Boolean {
            val destination = when (itemId) {
                R.id.nav_home -> HomeActivity::class.java
                R.id.nav_library -> LibraryActivity::class.java
                R.id.nav_progress -> ProgressActivity::class.java
                R.id.nav_search -> SearchActivity::class.java
                else -> return false
            }

            // Stay here only if the destination Activity is already open.
            if (activity.javaClass == destination) {
                return true
            }

            val intent = Intent(activity, destination).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            activity.startActivity(intent)

            if (activity !is HomeActivity) {
                activity.finish()
            }

            return true
        }

        navigation.setOnItemSelectedListener { item ->
            navigate(item.itemId)
        }

        // Also handle taps on the currently highlighted tab.
        navigation.setOnItemReselectedListener { item ->
            navigate(item.itemId)
        }
    }
}