package com.agnext.thermalprinter

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView


class BluetoothConnectAdapter(var context: Context, var list  : ArrayList<BluetoothDevice>) : RecyclerView.Adapter<BluetoothConnectAdapter.ViewHolder>() {
    var shpref : SharedPreferences = context.getSharedPreferences("MAIN", Context.MODE_PRIVATE)
    var editor : SharedPreferences.Editor = shpref.edit()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var printer_name : TextView?=null
        var printer_address : TextView?=null
        var printer_pair_status : TextView?=null
        var llrow : CardView?=null
        init{
            super.itemView
           printer_name = itemView.findViewById(R.id.tv_device_name)
            printer_address = itemView.findViewById(R.id.tv_address)
            printer_pair_status = itemView.findViewById(R.id.tv_pairStatus)
            llrow = itemView.findViewById(R.id.cardView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var itemview : View = LayoutInflater.from(parent.context).inflate(R.layout.device,parent,false)
        return ViewHolder(itemview)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var device =list.get(position)
        holder!!.printer_name!!.setText(device.name)
        holder!!.printer_address!!.setText(device.address)
        var str = "Not-Paired"

        if(device.bondState==BluetoothDevice.BOND_BONDED)
                str = "Paired"

        holder!!.printer_pair_status!!.setText(str)

        holder.llrow!!.setOnClickListener(object : View.OnClickListener{

            override fun onClick(v: View?) {

                val builder = AlertDialog.Builder(context)
                builder.setTitle("Connect")
                builder.setMessage("Do you want to connect to ${device.name.toString()} ?")
                builder.setIcon(R.drawable.baseline_bluetooth_24)
                builder.setPositiveButton("Yes") { dialog, which ->
                    editor.putBoolean("flag",true)
                    editor.putString("Printer_Name",holder.printer_name!!.text.toString())
                    editor.putString("Printer_Address",device.address.toString())
                    Log.d(TAG,"Printer_Name"+holder.printer_name!!.text.toString()+" ")
                    editor.apply()
                    (holder.llrow!!.getContext() as Activity).setResult(Activity.RESULT_OK)
                    (holder.llrow!!.getContext() as Activity).finish()
                    dialog.dismiss()
                }
                builder.setNegativeButton(
                    "No"
                ) { dialog, which ->
                    editor.putBoolean("flag",false)
                    editor.apply()
                    dialog.dismiss()
                }
                builder.show()
            }


        })

    }

}