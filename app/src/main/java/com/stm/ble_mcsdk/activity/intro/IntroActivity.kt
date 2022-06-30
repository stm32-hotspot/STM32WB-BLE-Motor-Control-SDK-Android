package com.stm.ble_mcsdk.activity.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.stm.ble_mcsdk.R
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment

class IntroActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSystemBackButtonLocked = true
        isColorTransitionsEnabled = true

        addSlide(AppIntroFragment.createInstance(
            title = "Welcome!",
            imageDrawable = R.drawable.ss_01,
            description = "Follow this introduction to learn how to use the app or press skip " +
                    "if you already know how to use it.",
            backgroundColorRes = R.color.st_light_blue
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Scanning & Connecting",
            imageDrawable = R.drawable.ss_02,
            description = "Tap the search icon to scan & connect to a Bluetooth device.",
            backgroundColorRes = R.color.st_yellow
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Bluetooth Permissions",
            imageDrawable = R.drawable.ss_03,
            description = "You may see permission requests, these are necessary for BT functionality.",
            backgroundColorRes = R.color.st_pink
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Turning Motor ON/OFF",
            imageDrawable = R.drawable.ss_04,
            description = "Once connected, tap the large cogwheel icon to turn on " +
                    "the motor. Tap it again to turn it off.",
            backgroundColorRes = R.color.st_light_blue
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Setting Motor Speed",
            imageDrawable = R.drawable.ss_05,
            description = "Use the slider to set the motor speed. Slide it right to turn " +
                    "clockwise or left to turn counter-clockwise.",
            backgroundColorRes = R.color.st_yellow
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Setting Motor Speed",
            imageDrawable = R.drawable.ss_06,
            description = "Tap the Set Speed value to set the speed more precisely.",
            backgroundColorRes = R.color.st_pink
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "MIN/MAX Speed",
            imageDrawable = R.drawable.ss_07,
            description = "The app will retrieve the MIN/MAX speed limits from the motor. You " +
                    "may also tap the speed icon to manually set the limits.",
            backgroundColorRes = R.color.st_light_blue
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Motor Response",
            imageDrawable = R.drawable.ss_08,
            description = "After sending a command, the bell icon will light up indicating a " +
                    "response.\n\n(Blue = Normal | Yellow = Speed | Pink = Error)",
            backgroundColorRes = R.color.st_yellow
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Time Log",
            imageDrawable = R.drawable.ss_09,
            description = "Tap the log icon to see a time log of app and bluetooth operations.",
            backgroundColorRes = R.color.st_pink
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }
}