package com.jelly.thor.baiduyuyinhecheng

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    protected var appId = "15074520"

    protected var appKey = "xKmvB9K4ls4dWWqKl3zboXVR"

    protected var secretKey = "FA267eB5PPK9xV36vntwyIUGIfxKBEZP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("123===", SystemClock.currentThreadTimeMillis().toString())

        YuYinHeChengUtils.debug = true
        YuYinHeChengUtils.init(
            this,
            10010,
            appId,
            appKey,
            secretKey
        )
        Log.d("123===", SystemClock.currentThreadTimeMillis().toString())

        mTv.setOnClickListener {
            /*if (YuYinHeChengUtils.getInitSuccess()) {
                YuYinHeChengUtils.speak(mTv.text.toString())
            }*/

            YuYinHeChengUtils.onDestroy()
            YuYinHeChengUtils.init(
                this,
                10010,
                appId,
                appKey,
                secretKey
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        YuYinHeChengUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        YuYinHeChengUtils.onDestroy()
    }
}
