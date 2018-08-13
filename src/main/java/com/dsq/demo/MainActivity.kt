package com.dsq.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import com.megvii.demo.BankCardScanActivity
import com.megvii.idcardlib.IDCardScanActivity
import com.megvii.idcardlib.util.Util
import com.megvii.idcardquality.IDCardQualityLicenseManager
import com.megvii.licensemanager.Manager
import com.megvii.livenessdetection.LivenessLicenseManager
import com.megvii.livenesslib.LivenessActivity
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import org.jetbrains.anko.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnClick(R.id.btnFaceBankOcr) { request(arrayOf(Permission.CAMERA)) { startActivity<BankCardScanActivity>() } }

        btnClick(R.id.btnFaceIdOcr) { request(arrayOf(Permission.CAMERA)) { startFaceIDCard(0) } }

        btnClick(R.id.btnFaceIdOcrBack) { request(arrayOf(Permission.CAMERA)) { startFaceIDCard(1) } }

        btnClick(R.id.btnFaceLiveness) {
            request(arrayOf(Permission.CAMERA, Permission.READ_PHONE_STATE)) { startFaceLiveness() }
        }
    }

    /**
     * @param sideType 0：默认值，身份证正面；1：身份证反面
     */
    private fun startFaceIDCard(sideType: Int) {
        doAsync {
            val manager = Manager(act)
            val licenseManager = IDCardQualityLicenseManager(act)
            manager.registerLicenseManager(licenseManager)
            //请求网络，需要在子线程中进行
            manager.takeLicenseFromNetwork(Util.getUUIDString(act))
            if (licenseManager.checkCachedLicense() > 0) {
                startActivityForResult<IDCardScanActivity>(if (sideType == 0) REQ_IDCARD_FRONT else REQ_IDCARD_BACK, if (sideType == 0) "side" to 0 else "side" to 1)
            } else {
                //切回UI线程中
                runOnUiThread { toast("FaceID联网授权失败，请退出重试") }
            }
        }
    }

    private fun startFaceLiveness() {
        doAsync {
            val manager = Manager(act)
            val licenseManager = LivenessLicenseManager(act)
            manager.registerLicenseManager(licenseManager)
            //请求网络，需要在子线程中进行
            manager.takeLicenseFromNetwork(Util.getUUIDString(act))
            if (licenseManager.checkCachedLicense() > 0) {
                startActivityForResult<LivenessActivity>(REQ_LIVENESS)
            } else {
                //切回UI线程中
                runOnUiThread { toast("FaceID联网授权失败，请退出重试") }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_LIVENESS -> handleFaceLiveness(data)
                REQ_IDCARD_FRONT, REQ_IDCARD_BACK -> {
                    toast("识别成功")
                    Log.i(TAG, "the Face++ ID Card Scan return ::: ${data?.extras}")
                }
            }
        }
    }

    private fun handleFaceLiveness(data: Intent?) {
        if (data == null) return
        val result = data.getStringExtra("result")
        Log.i(TAG, "the Face++ liveness result::: ${data.extras}")
        if (JSONObject(result).getString("result") == getString(com.megvii.livenesslib.R.string.verify_success)) {
            toast("活体验证成功")
        } else {
            toast("活体验证失败")
        }
    }

    private fun btnClick(@IdRes btnId: Int, callback: () -> Unit) {
        findViewById<Button>(btnId).setOnClickListener { callback.invoke() }
    }


    private fun request(permissions: Array<out String>, callback: () -> Unit) {
        AndPermission.with(act).runtime()
                .permission(permissions)
                .onDenied {
                    if (AndPermission.hasAlwaysDeniedPermission(act, it)) {
                        showAlert("因缺少相应的权限，下一步的功能无法使用，请去设置中授权权限: \n${Permission.transformText(ctx, it).joinToString("，\n")}") { AndPermission.with(act).runtime().setting().start() }
                    }
                }
                .onGranted { callback.invoke() }
                .rationale { ctx, pList, executor ->
                    Log.d(TAG, "the rational list ::: ${pList.joinToString(", \n")}")
                    showAlert("为保证功能的正常使用，请允许应用访问您的权限：\n${Permission.transformText(ctx, pList).joinToString("，\n")}") { executor.execute() }
                }.start()
    }

    private fun showAlert(msg: String, callback: () -> Unit) {
        AlertDialog.Builder(act).setMessage(msg)
                .setTitle("权限提示：")
                .setPositiveButton("确定") { _, _ -> callback.invoke() }
                .setNegativeButton("取消", null)
                .show()
    }

    companion object {
        const val TAG = "MainActivity"
        const val REQ_LIVENESS = 0x10
        const val REQ_IDCARD_FRONT = 0x11
        const val REQ_IDCARD_BACK = 0x12
    }
}
