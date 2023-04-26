package com.agnext.module_testing_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.agnext.login.LoginActivity
import com.agnext.thermalprinter.PrinterActivity

class MainActivity : AppCompatActivity() {
    lateinit var btn_module : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_module = findViewById(R.id.btn_submit)
        btn_module.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                startActivity(Intent(this@MainActivity,PrinterActivity::class.java))
            }

        })
    }
}