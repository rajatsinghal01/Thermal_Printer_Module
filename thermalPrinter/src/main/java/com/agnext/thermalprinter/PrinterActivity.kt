package com.agnext.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.agnext.thermalprinter.PrintData.Companion.CENTER
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class PrinterActivity : AppCompatActivity() {


    private var printer_name : TextView?=null
    private var printer_connection_status: TextView?=null
    var commodity_name : TextInputEditText?=null
    var commodity_moisture : TextInputEditText?=null
    var commodity_starch : TextInputEditText?=null
    var commodity_gluten: TextInputEditText?=null
    var commodity_dryGluten : TextInputEditText?=null
    var clientName : TextInputEditText?=null
    var deviceName: TextInputEditText?=null
    var deviceID : TextInputEditText?=null
    var sampleID : TextInputEditText?=null
    var button_print : AppCompatButton?=null
    var button_connect : AppCompatButton ?=null
    var connected : Boolean = false;
    var REQUEST_CONNECT=10
    var shpref : SharedPreferences ?=null
    var editor : SharedPreferences.Editor ?=null
    var bluetoothAdapter : BluetoothAdapter ?=null
    var device : BluetoothDevice ?=null
    var mBluetoothSocket : BluetoothSocket ?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)

        init()
        onclick()

    }
    companion object{
        @JvmStatic
        private var uuid : UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    }

    fun init(){
        printer_name = findViewById(R.id.tv_printer_name)
        printer_connection_status = findViewById(R.id.tv_printer_status)
        commodity_name = findViewById(R.id.TextInputEditText_Commodity)
        commodity_moisture = findViewById(R.id.TextInputEditText_moisture)
        commodity_gluten = findViewById(R.id.TextInputEditText_gluten)
        commodity_dryGluten = findViewById(R.id.TextInputEditText_dryGluten)
        commodity_starch = findViewById(R.id.TextInputEditText_starch)
        deviceName = findViewById(R.id.TextInputEditText_deviceName)
        deviceID = findViewById(R.id.TextInputEditText_deviceID)
        sampleID = findViewById(R.id.TextInputEditText_sampleID)
        deviceID = findViewById(R.id.TextInputEditText_deviceID)
        clientName = findViewById(R.id.TextInputEditText_clientName)
        button_print = findViewById(R.id.btn_Print)
        button_connect = findViewById(R.id.btn_connect)
        shpref = getSharedPreferences("MAIN",MODE_PRIVATE)
        editor = shpref!!.edit()
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter()
        button_print!!.isEnabled=false
    }
    fun onclick(){

        button_connect!!.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                if(connected){
                    button_connect!!.text="Connect"
                    printer_connection_status!!.text="Not connected"
                    printer_name!!.text="Not connected"
                    connected=false
                    if (ActivityCompat.checkSelfPermission(
                            this@PrinterActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothAdapter!!.disable()
                    }
                    button_print!!.isEnabled=false
                    editor!!.clear()
                    editor!!.apply()

                    if (ActivityCompat.checkSelfPermission(
                            this@PrinterActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothAdapter!!.disable()
                    }

                }
                else{
                    startActivityForResult(Intent(this@PrinterActivity,Bluetooth_activity::class.java),REQUEST_CONNECT)
                }
            }

        })
        button_print!!.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                printData()
            }

        })
    }
    fun printData() {
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    val date = Date()
                    date.time = System.currentTimeMillis()
                    val simpleTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val format = simpleTimeFormat.format(date)
                    val formatter = SimpleDateFormat("dd-MM-yyyy")
                    val Date_format = formatter.format(date)

                    var logo =PrintData.getBitmapFromVector(this@PrinterActivity,R.drawable.agnext_print_logoresized)
                    PrintData.with(this@PrinterActivity)!!.connect(object : PrintData.OnConnected {
                        override fun onConnected(printData: PrintData?) {
                            printData!!.setNormalText()
                            printData!!.printImage(logo,270, CENTER)
                            printData.addNewLine(1)
                            printData.printTextln("Analysis Report", CENTER);
                            printData.addNewLine(1)
                            printData.printTextJustify("   Device :","","${if (deviceName!!.text!!.isNotEmpty()) deviceName!!.text.toString() else "Not Available"} ")
                            printData.printTextJustify("   Device Id :","","${if (deviceID!!.text!!.isNotEmpty()) deviceID!!.text.toString() else "Not Available"} ")
                            printData.printTextJustify(
                                "   Client Name :","",
                                "${if (clientName!!.text!!.isNotEmpty()) clientName!!.text.toString() else "Not Available"} "
                            )
                            printData.printTextJustify("   Commodity :","","${if(commodity_name!!.text!!.isNotEmpty())commodity_name!!.text.toString() else "Not Available"} ")
                            printData.printTextJustify("   Sample Id :","","${if (sampleID!!.text!!.isNotEmpty())sampleID!!.text.toString() else "Not Available"} ")
                            printData.printTextJustify("   Scan Date :","","${Date_format} ")
                            printData.printTextJustify("   Scan Time :","","${format} ")
                            printData.addNewLine(1)
                            printData.printDashedLine()
                            printData.printTextJustify("","Parameters","","Result")
                            printData.printDashedLine()
                            printData.addNewLine(1)
                            printData.printTextJustify("   Starch"," ","${if(commodity_starch!!.text!!.isNotEmpty())commodity_starch!!.text.toString() else "Not Available"} %")
                            printData.printTextJustify("   Moisture"," ","${if(commodity_moisture!!.text!!.isNotEmpty())commodity_moisture!!.text.toString() else "Not Available"} %")
                            printData.printTextJustify("   Gluten"," ","${if(commodity_gluten!!.text!!.isNotEmpty())commodity_gluten!!.text.toString() else "Not Available"} %")
                            printData.printTextJustify("   Dry Gluten","","${if(commodity_dryGluten!!.text!!.isNotEmpty())commodity_dryGluten!!.text.toString() else "Not Available"} %")
                            printData.printDashedLine()
                            printData.printTextln("Agnext Technologies Pvt Ltd", CENTER)
                            printData.setNormalText();
                            printData.feedPaper();
                            printData.close();
                        }
                    },null)
                    } catch (e: Exception) {
                    Log.e("PrintActivity", "Exe ", e)
                }
            }
        }
        t.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== RESULT_OK && requestCode==REQUEST_CONNECT && shpref!!.getBoolean("flag",false)==true ) {
            device = bluetoothAdapter!!.getRemoteDevice(shpref!!.getString("Printer_Address","")!!)
            printer_name!!.text = device!!.name.toString()
            printer_connection_status!!.setText("Connecting")
            connecttoBluetooth()

        }

            else{
                connected=false
                printer_connection_status!!.text="Not connected"
                printer_name!!.text="Not connected"
                Toast.makeText(this@PrinterActivity,"Connection Failed!",Toast.LENGTH_SHORT).show()
            }

    }

    private fun connecttoBluetooth() {

            Thread(
                Runnable {
                    mBluetoothSocket = device!!.createRfcommSocketToServiceRecord(uuid)
                    try {
                        bluetoothAdapter!!.cancelDiscovery()

                        mBluetoothSocket!!.connect()
                        mHandler.sendEmptyMessage(0)

                    } catch (eConnectException: IOException) {
                        Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
                        try {
                            mBluetoothSocket!!.close()
                            Log.d(TAG, "SocketClosed")
                        } catch (ex: IOException) {
                            Log.d(TAG, "CouldNotCloseSocket")
                        }
                    }
                }).start()
    }
    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            printer_connection_status!!.setText("")
            printer_connection_status!!.setText("Connected")
            connected = true
            button_connect!!.text = "Disconnect"
            button_print!!.isEnabled=true
            Toast.makeText(this@PrinterActivity,"Printer Connected",Toast.LENGTH_SHORT)
        }
    }


    override fun onBackPressed() {
        BluetoothAdapter.getDefaultAdapter()!!.disable()
        super.onBackPressed()
    }
}

// THermal Printer Print Function from : https://github.com/anggastudio/Printama
