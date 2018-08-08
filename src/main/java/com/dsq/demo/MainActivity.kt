package com.dsq.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.megvii.demo.BankCardScanActivity
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import org.jetbrains.anko.ctx
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            AndPermission.with(ctx).runtime()
                    .permission(Permission.CAMERA)
                    .onGranted { startActivity<BankCardScanActivity>() }
                    .start()
        }
    }
}
