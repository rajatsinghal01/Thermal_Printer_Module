package com.agnext.thermalprinter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class Bluetooth_activity : AppCompatActivity(){

    private lateinit var permissionslauncher : ActivityResultLauncher<Array<String>>
    private var isBluetoothPermissionGranted = false
    private var isBluetoothScanPermissionGranted = false
    private var isBluetoothConnectPermissionGranted = false
    private var isBluetoothAdvertisePermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isBackgroundLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    private var isrecieverRegistered = false


    var bluetoothAdapter : BluetoothAdapter?=null
    var REQUEST_ENABLE_BT =1



    var recyclerView : RecyclerView ?=null
    var adapter : BluetoothConnectAdapter ?=null
    var progressBar : ProgressBar ?=null
    var bt : ArrayList<BluetoothDevice> = ArrayList()
    var set : HashSet<BluetoothDevice> ?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        init()
        scan_for_bluetooth()
        bt = ArrayList(bluetoothAdapter!!.bondedDevices)
        CoroutineScope(Dispatchers.Main).launch {
            async {
                scan_for_devices()
            }.await()
        }
        set=HashSet(bt)
        adapter = BluetoothConnectAdapter(this,bt!!)
        recyclerView!!.layoutManager=LinearLayoutManager(this)
        recyclerView!!.adapter=adapter

    }
    var myReceiver : BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            var x = intent!!.action
            if(BluetoothDevice.ACTION_FOUND.equals(x)){
                var device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if(device!=null && device.name!=null && !set!!.contains(device)) {
                    bt!!.add(device)
                    set!!.add(device)
                    adapter!!.notifyDataSetChanged()
                }
                isrecieverRegistered=false
                recyclerView!!.visibility=View.VISIBLE
                progressBar!!.visibility=View.GONE
            }
        }

    }
    private fun requestPermission(){

        isBluetoothPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH)==PackageManager.PERMISSION_GRANTED
        isBluetoothScanPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED
        isBluetoothAdvertisePermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED
        isBluetoothConnectPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
//        isBackgroundLocationPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED
        isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val permissionRequest : MutableList<String> = ArrayList()

        if(!isBluetoothPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH)
        }

        if(!isBluetoothScanPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if(!isBluetoothConnectPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if(!isFineLocationPermissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if(!isCoarseLocationPermissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if(permissionRequest.isNotEmpty()){
            permissionslauncher.launch(permissionRequest.toTypedArray())
        }


    }
    fun init(){
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter()
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        var toolbar : androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        permissionslauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            isBluetoothPermissionGranted = permissions[Manifest.permission.BLUETOOTH] ?:isBluetoothPermissionGranted
            isBluetoothScanPermissionGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?:isBluetoothScanPermissionGranted
            isBluetoothAdvertisePermissionGranted = permissions[Manifest.permission.BLUETOOTH_ADVERTISE] ?:isBluetoothAdvertisePermissionGranted
            isBluetoothConnectPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?:isBluetoothConnectPermissionGranted
//            isBackgroundLocationPermissionGranted =permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?:isBackgroundLocationPermissionGranted
            isFineLocationPermissionGranted =permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?:isFineLocationPermissionGranted
            isCoarseLocationPermissionGranted =permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?:isCoarseLocationPermissionGranted

        }
        requestPermission()

    }
    private fun scan_for_bluetooth() {
        if(bluetoothAdapter==null)
        {
            Toast.makeText(this,"This device doesn't supports Bluetooth", Toast.LENGTH_LONG).show()

        }

        if(!bluetoothAdapter!!.isEnabled){
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),REQUEST_ENABLE_BT)


        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scan_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.scan ->{
                progressBar!!.visibility=View.VISIBLE
                recyclerView!!.visibility=View.GONE
                CoroutineScope(Dispatchers.Main).launch {
                    async {
                        scan_for_devices()
                    }.await()
                }
            }

        }
        return super.onOptionsItemSelected(item)
    }
//    fun relaunchActivity(){
//        finish()
//        startActivity(getIntent())
//    }
    fun statusCheck() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your Location seems to be disabled, Please Enable it!")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            }
            .setNegativeButton(
                "No"
            ) { dialog, id ->
                Toast.makeText(this@Bluetooth_activity,"Can't Scan Bluetooth Devices as Location is disabled!",Toast.LENGTH_SHORT).show()

                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()

    }
//    fun checkPermission() {
//        val t = object : Thread() {
//            override fun run() {
//                if (Build.VERSION.SDK_INT >= 31) {
//                    if (check_bluetooth_connect_permission()) {
//                        if (check_bluetooth_advertise_permission()) {
//                            if (!check_bluetooth_scan_permission()) {
//                                request_bluetooth_scan_permission()
//                            }
//                        } else {
//                            request_bluetooth_advertise_permission()
//                        }
//                    }
//                    else{
//                        request_bluetooth_connect_permission()
//                    }
//                    if(!check_bluetooth_permissions())
//                        request_bluetooth_permissions()
//                }
//            }
//        }
//        t.start()
//        t.join(10000)

//        if (Build.VERSION.SDK_INT >= 23) {
//            if (checkSelfPermission(
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    this, arrayOf(
//                        Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION
//                    ), REQUEST_ENABLE_LOCATION
//                )
//            }
//        }

//    }
//    fun check_bluetooth_scan_permission() : Boolean{
//        return (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
//    }
//    fun check_bluetooth_advertise_permission() : Boolean{
//        return (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED)
//    }
//    fun check_bluetooth_connect_permission() : Boolean{
//        return (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
//    }

//    fun request_bluetooth_scan_permission(){
//
//            ActivityCompat.requestPermissions(
//                this@Bluetooth_activity, arrayOf(
//                    "android.permission.BLUETOOTH_SCAN"
//                ), REQUEST_SCAN_BT
//            )
//        if(check_bluetooth_scan_permission()) {
//            if (Build.VERSION.SDK_INT >= 23) {
//                if (checkSelfPermission(
//                        Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    ActivityCompat.requestPermissions(
//                        this, arrayOf(
//                            Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_FINE_LOCATION
//                        ), REQUEST_ENABLE_LOCATION
//                    )
//                }
//            }
//        }
//
//    }
//    fun request_bluetooth_advertise_permission(){
//        ActivityCompat.requestPermissions(
//            this@Bluetooth_activity, arrayOf(
//                Manifest.permission.BLUETOOTH_ADVERTISE
//            ), REQUEST_ADVERTISE_BT
//        )
//        return
//    }
//    fun request_bluetooth_Enable_permission(){
//        ActivityCompat.requestPermissions(
//            this@Bluetooth_activity, arrayOf(
//                Manifest.permission.BLUETOOTH
//            ), REQUEST_ENABLE_BT
//        )
//        return
//    }
//    fun request_bluetooth_connect_permission(){
//        ActivityCompat.requestPermissions(
//            this@Bluetooth_activity, arrayOf(
//                Manifest.permission.BLUETOOTH_CONNECT
//            ), REQUEST_CONNECT_BT
//        )
//        return
//    }
//    fun request_bluetooth_permissions(){
//        ActivityCompat.requestPermissions(
//            this@Bluetooth_activity, arrayOf(Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE
//            ), REQUEST_BLUETOOTH_PERMISSIONS
//        )
//        return
//    }

//    fun check_bluetooth_permissions() : Boolean{
//        return (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED)
//    }
    private suspend fun scan_for_devices(){
    statusCheck()
    if(bt.size==0)
        bt = ArrayList(bluetoothAdapter!!.bondedDevices)

    var intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    isrecieverRegistered=true
    var c = CoroutineScope(Dispatchers.IO).launch {

            registerReceiver(myReceiver, intentFilter)
            bluetoothAdapter!!.startDiscovery()
            delay(10000)
        }
    c.start()
    c.join()
    when(c.isCompleted) {
        true -> {

            recyclerView!!.visibility = View.VISIBLE
            progressBar!!.visibility = View.GONE
        }
        else ->{
            if (isrecieverRegistered)
                unregisterReceiver(myReceiver)
            Toast.makeText(this@Bluetooth_activity,"No Bluetooth Device Found",Toast.LENGTH_SHORT).show()
            recyclerView!!.visibility = View.VISIBLE
            progressBar!!.visibility = View.GONE
        }

    }

    }
//
//    override fun onRequestPermissionsResult(requestCode: Int,
//                                            permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            REQUEST_BLUETOOTH_PERMISSIONS -> {
//                if ((grantResults.isNotEmpty() &&
//                            grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED)
//                )
//                    bt = ArrayList(bluetoothAdapter!!.bondedDevices)
//
//            }
//            REQUEST_ENABLE_BT -> {
//                if (grantResults.isNotEmpty() &&
//                    grantResults[0] != PackageManager.PERMISSION_GRANTED
//                ) {
//                    Toast.makeText(this, "Please Enable Bluetooth", Toast.LENGTH_SHORT)
//                    startActivityForResult(
//                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
//                        REQUEST_ENABLE_BT
//                    )
//                }
//            }
//            REQUEST_CONNECT_BT -> {
//                if ((grantResults.isNotEmpty() &&
//                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                ) {
//                    bt = ArrayList(bluetoothAdapter!!.bondedDevices)
//
//                } else {
////                    request_bluetooth_connect_permission()
//                }
//            }
//            REQUEST_ENABLE_LOCATION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
////                    requestPermissions(
////                        arrayOf(
////                            Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION
////                        ), REQUEST_ENABLE_LOCATION
////                    )
//                    return
//                }
//            }
//            REQUEST_SCAN_BT -> {
//                if ((grantResults.isNotEmpty() &&
//                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                ) {
//                    bluetoothAdapter!!.startDiscovery()
//                } else {
////                    request_bluetooth_scan_permission()
//                }
//            }
//            REQUEST_ADVERTISE_BT -> {
//                if ((grantResults.isNotEmpty() &&
//                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                )
//            }
//        }
//    }

    override fun onBackPressed() {
        var shpref = getSharedPreferences("MAIN",Context.MODE_PRIVATE)
        unregisterReceiver(myReceiver)
        if(!shpref.getBoolean("flag",false)) {
            var editor = shpref!!.edit()
            editor.clear()
            editor.apply()
        }
        super.onBackPressed()
    }

}