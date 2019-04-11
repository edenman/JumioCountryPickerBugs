package co.recharge.jumiocountrypickerbugs

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.jumio.core.enums.JumioDataCenter
import com.jumio.nv.NetverifyInitiateCallback
import com.jumio.nv.NetverifySDK
import com.jumio.nv.data.document.NVDocumentType.DRIVER_LICENSE
import com.jumio.nv.data.document.NVDocumentVariant.PLASTIC
import kotlinx.android.synthetic.main.activity_main.start_id_scan_button
import timber.log.Timber
import java.util.concurrent.TimeUnit.SECONDS

class MainJumioActivity : AppCompatActivity() {
  companion object {
    private const val cameraPermissionRequestId = 2001
    // TODO DON'T PUSH THESE TO GITHUB!!!
    private const val API_TOKEN = "FOO"
    private const val API_SECRET = "FOO"
  }

  private var sdk: NetverifySDK? = null
  private var initialized = false
  private var retrySecondsDelay = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    createSDK()
    start_id_scan_button.setOnClickListener {
      if (alreadyHasCameraPermission(this)) {
        // Already have camera permission.
        startIDCapture()
      } else {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), cameraPermissionRequestId)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (initialized) {
      Timber.d("No longer need Jumio SDK, destroying SDK")
      destroySDKIfCreated()
    } else if (sdk != null) {
      Timber.d("No longer need Jumio SDK but it's mid-initialization, waiting")
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray) {
    if (requestCode != cameraPermissionRequestId) {
      return
    }
    if (grantResults.isEmpty()) {
      Timber.e("cameraPermissionRequestId resulted in zero grantResults?")
      return
    }
    if (grantResults[0] == PERMISSION_GRANTED) {
      startIDCapture()
    } else {
      // TODO show err
    }
  }

  private fun alreadyHasCameraPermission(context: Context): Boolean {
    return checkSelfPermission(context, CAMERA) == PERMISSION_GRANTED
  }

  fun startIDCapture() {
    val localSDK = sdk
    if (localSDK == null || !initialized) {
      // TODO show error
      return
    }
    localSDK.setUserReference("my-user-id")
    localSDK.start()
  }

  private fun createSDK() {
    if (!NetverifySDK.isSupportedPlatform(this)) {
      Timber.e("Not a supported platform: not creating Jumio SDK")
      return
    }
    if (NetverifySDK.isRooted(this)) {
      Timber.w("Device is rooted: not creating Jumio SDK")
      return
    }
    Timber.d("Got created activity and user in need of verification: creating Jumio SDK")
    val newSDK = NetverifySDK.create(this, API_TOKEN, API_SECRET, JumioDataCenter.US)
    newSDK.setPreselectedCountry("USA")
    newSDK.setPreselectedDocumentTypes(arrayListOf(DRIVER_LICENSE))
    newSDK.setPreselectedDocumentVariant(PLASTIC)
    retrySecondsDelay = 1
    sdk = newSDK
    initiateSDK(newSDK)
  }

  private fun initiateSDK(sdk: NetverifySDK) {
    sdk.initiate(object : NetverifyInitiateCallback {
      override fun onNetverifyInitiateError(errCode: String?, errMsg: String?,
          retryPossible: Boolean) {
        if (isDestroyed) {
          Timber.w("onNetverifyInitiateError but activity is destroyed, ignore. $errCode: $errMsg")
          destroySDKIfCreated()
          return
        }
        if (retryPossible) {
          Timber.w("onNetverifyInitiateError (retrying in ${retrySecondsDelay}s) $errCode: $errMsg")
          Async.main(retrySecondsDelay, SECONDS) {
            retrySecondsDelay *= 2 // Exponential backoff.
            initiateSDK(sdk)
          }
        } else {
          Timber.e("onNetverifyInitiateError (not retryable) $errCode: $errMsg")
          destroySDKIfCreated()
        }
      }

      override fun onNetverifyInitiateSuccess() {
        if (isDestroyed) {
          Timber.w("onNetverifyInitiateSuccess but activity is destroyed, ignore")
          destroySDKIfCreated()
          return
        }
        Timber.d("onNetverifyInitiateSuccess")
        initialized = true
      }
    })
  }

  private fun destroySDKIfCreated() {
    val localSDK = sdk
    if (localSDK != null) {
      localSDK.destroy()
      sdk = null
      initialized = false
    }
  }
}
