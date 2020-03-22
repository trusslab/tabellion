package edu.uci.ics.charm.tabellion

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/*
Created Date: 05/31/2019
Created By: Myles Liu
Last Modified: 06/06/2019
Last Modified By: Myles Liu
Notes:

 */

class TakeScreenshotExternally : AppCompatActivity() {

    // Part of this class's credit is given to http://3jigen.net/2018/05/post-720/

    companion object {
        private val TAG = "TakeScreenshotExternal"
        private val PERMISSION_CODE = 1
    }

    private lateinit var rootLayout: RelativeLayout

    internal var context: Context = this
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var screenCapture: ScreenCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_screenshot_externally)

        rootLayout = findViewById<View>(R.id.take_screenshot_externally_layout) as RelativeLayout
        screenCapture = ScreenCapture(this)
        mediaProjectionManager = getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onResume() {
        super.onResume()

        rootLayout.post {
            startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), PERMISSION_CODE)
        }

        OperationsWithNDK.turnOnLedLightForSigning()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this,
                    R.string.denied_permission_for_taking_screenshot, Toast.LENGTH_SHORT).show()
            return
        }
        mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, data!!)

        // Wait for the first time of requesting and releasing permission
        try {
            Thread.sleep(100)
            doCapture()
            Log.d(TAG, "Screenshot is successfully taken!!!")
        } catch (e: InterruptedException) {
            mediaProjection = null
            e.printStackTrace()
            Log.d(TAG, "Problem happened when taking screenshot!!!")
        }
        //finish()
    }

    private fun disableCapture() {
        screenCapture?.stop()
        mediaProjection = null
    }

    private fun doCapture() {
        mediaProjection?.let {
            screenCapture?.run(it) { bitMap ->
                disableCapture()
                Screenshot.storeScreenshot(bitMap, "screenshot_default_externally.png", Contract("Test_Contract"), context)
                //finish()
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        disableCapture()
        OperationsWithNDK.turnOffLedLightForSigning()
    }

}
