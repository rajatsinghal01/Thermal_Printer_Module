package com.agnext.thermalprinter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import com.agnext.thermalprinter.PrintData.Companion.CENTER
import com.agnext.thermalprinter.PrintData.Companion.FULL_WIDTH
import com.agnext.thermalprinter.PrintData.Companion.ORIGINAL_WIDTH
import com.agnext.thermalprinter.PrintData.Companion.RIGHT

import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.experimental.or


internal class PrinterUtil(private val printer: BluetoothDevice) {
    private var btSocket: BluetoothSocket? = null
    private var btOutputStream: OutputStream? = null
    fun connectPrinter(successListener: PrinterConnected, failedListener: PrinterConnectFailed) {
        ConnectAsyncTask(object : ConnectAsyncTask.ConnectionListener {
            override fun onConnected(socket: BluetoothSocket?) {
                btSocket = socket
                try {
                    btOutputStream = socket!!.outputStream
                    successListener.onConnected()
                } catch (e: IOException) {
                    failedListener.onFailed()
                }
            }

            override fun onFailed() {
                failedListener.onFailed()
            }
        }).execute(printer)
    }

    val isConnected: Boolean
        get() = btSocket != null && btSocket!!.isConnected

    fun finish() {
        if (btSocket != null) {
            try {
                btOutputStream!!.close()
                btSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            btSocket = null
        }
    }

    private fun printUnicode(data: ByteArray): Boolean {
        return try {
            btOutputStream!!.write(data)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT
    //----------------------------------------------------------------------------------------------
    fun printText(text: String?): Boolean {
        return try {
            val s: String = StrUtil.encodeNonAscii(text!!)
            btOutputStream!!.write(s.toByteArray())
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun setNormalText() {
        printUnicode(NORMAL)
    }

    fun setSmallText() {
        printUnicode(SMALL)
    }

    fun setBold() {
        printUnicode(BOLD)
    }

    fun setUnderline() {
        printUnicode(UNDERLINE)
    }

    fun setDeleteLine() {
        printUnicode(DELETE_LINE)
    }

    fun setTall() {
        printUnicode(TALL)
    }

    fun setWide() {
        printUnicode(WIDE)
    }

    fun setWideBold() {
        printUnicode(WIDE_BOLD)
    }

    fun setTallBold() {
        printUnicode(TALL_BOLD)
    }

    fun setWideTall() {
        printUnicode(WIDE_TALL)
    }

    fun setWideTallBold() {
        printUnicode(WIDE_TALL_BOLD)
    }

    fun printEndPaper() {
        printUnicode(FEED_PAPER_AND_CUT)
    }

    fun addNewLine(): Boolean {
        return printUnicode(NEW_LINE)
    }

    fun addNewLine(count: Int): Int {
        var success = 0
        for (i in 0 until count) {
            if (addNewLine()) success++
        }
        return success
    }

    fun setAlign(alignType: Int) {
        val d: ByteArray
        d = when (alignType) {
            CENTER -> ESC_ALIGN_CENTER
            RIGHT -> ESC_ALIGN_RIGHT
            else -> ESC_ALIGN_LEFT
        }
        try {
            btOutputStream!!.write(d)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setLineSpacing(lineSpacing: Int) {
        val cmd = byteArrayOf(0x1B, 0x33, lineSpacing.toByte())
        printUnicode(cmd)
    }

    //----------------------------------------------------------------------------------------------
    // PRINT IMAGE
    //----------------------------------------------------------------------------------------------
    fun printImage(bitmap: Bitmap): Boolean {
        return try {
            val width: Int = if (bitmap.width > PRINTER_WIDTH) FULL_WIDTH else ORIGINAL_WIDTH
            printImage(PrintData.CENTER, bitmap, width)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Maybe resource is vector or mipmap?")
            false
        }
    }

    fun printImage(bitmap: Bitmap, width: Int): Boolean {
        return printImage(PrintData.CENTER, bitmap, width)
    }

    fun printImage(alignment: Int, bitmap: Bitmap, width: Int): Boolean {
        var width = width
        if (width == FULL_WIDTH) width = PRINTER_WIDTH
        val scaledBitmap = scaledBitmap(bitmap, width)
        return if (scaledBitmap != null) {
            var marginLeft = INITIAL_MARGIN_LEFT
            if (alignment == CENTER) {
                marginLeft = marginLeft + (PRINTER_WIDTH - scaledBitmap.width) / 2
            } else if (alignment == RIGHT) {
                marginLeft = marginLeft + PRINTER_WIDTH - scaledBitmap.width
            }
            val command = autoGrayScale(scaledBitmap, marginLeft, 5)
            val lines = (command.size - HEAD) / WIDTH
            System.arraycopy(
                byteArrayOf(
                    0x1D,
                    0x76,
                    0x30,
                    0x00,
                    0x30,
                    0x00,
                    (lines and 0xff).toByte(),
                    (lines shr 8 and 0xff).toByte()
                ), 0, command, 0, HEAD
            )
            printUnicode(command)
        } else {
            false
        }
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int): Bitmap? {
        return try {
            var desiredWidth =
                if (width == 0 || bitmap.width <= PRINTER_WIDTH) bitmap.width else PRINTER_WIDTH
            if (width > 0 && width <= PRINTER_WIDTH) {
                desiredWidth = width
            }
            val height: Int
            val scale = desiredWidth.toFloat() / bitmap.width.toFloat()
            height = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, desiredWidth, height, true)
        } catch (e: NullPointerException) {
            Log.e(TAG, "Maybe resource is vector or mipmap?")
            null
        }
    }

    fun feedPaper() {
        addNewLine()
        addNewLine()
        addNewLine()
        addNewLine()
    }

    private class ConnectAsyncTask(listener: ConnectionListener) :
        AsyncTask<BluetoothDevice?, Void?, BluetoothSocket?>() {
        private val listener: ConnectionListener?

        init {
            this.listener = listener
        }

        protected override fun doInBackground(vararg bluetoothDevices: BluetoothDevice?): BluetoothSocket? {
            val device = bluetoothDevices[0]
            val uuid: UUID
            uuid = if (device != null) {
                val uuids = device.uuids
                if (uuids != null && uuids.size > 0) uuids[0].uuid else UUID.randomUUID()
            } else {
                return null
            }
            var socket: BluetoothSocket? = null
            var connected = true
            try {
                socket = device!!.createRfcommSocketToServiceRecord(uuid)
                socket!!.connect()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e2: Exception) {
                connected = false
            }
            return if (connected) socket else null
        }

        override fun onPostExecute(bluetoothSocket: BluetoothSocket?) {
            if (listener != null) {
                if (bluetoothSocket != null) listener.onConnected(bluetoothSocket) else listener.onFailed()
            }
        }

        interface ConnectionListener {
            fun onConnected(socket: BluetoothSocket?)
            fun onFailed()
        }
    }

    interface PrinterConnected {
        fun onConnected()
    }

    interface PrinterConnectFailed {
        fun onFailed()
    }

    companion object {
        private const val TAG = "PRINTAMA"
        private const val PRINTER_WIDTH = 384
        private const val INITIAL_MARGIN_LEFT = -4
        private const val BIT_WIDTH = 384
        private const val WIDTH = 48
        private const val HEAD = 8

        // printer commands
        private val NEW_LINE = byteArrayOf(10)
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1b, 'a'.code.toByte(), 0x01)
        private val ESC_ALIGN_RIGHT = byteArrayOf(0x1b, 'a'.code.toByte(), 0x02)
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1b, 'a'.code.toByte(), 0x00)
        private val FEED_PAPER_AND_CUT = byteArrayOf(0x1D, 0x56, 66, 0x00)
        private val SMALL = byteArrayOf(0x1B, 0x21, 0x01)
        private val NORMAL = byteArrayOf(0x1B, 0x21, 0x00)
        private val BOLD = byteArrayOf(0x1B, 0x21, 0x08)
        private val WIDE = byteArrayOf(0x1B, 0x21, 0x20)
        private val TALL = byteArrayOf(0x1B, 0x21, 0x10)
        private val UNDERLINE = byteArrayOf(0x1B, 0x21, 0x80.toByte())
        private val DELETE_LINE = byteArrayOf(0x1B, 0x21, 0x40.toByte())
        private val WIDE_BOLD = byteArrayOf(0x1B, 0x21, (0x20 or 0x08).toByte())
        private val TALL_BOLD = byteArrayOf(0x1B, 0x21, (0x10 or 0x08).toByte())
        private val WIDE_TALL = byteArrayOf(0x1B, 0x21, (0x20 or 0x10).toByte())
        private val WIDE_TALL_BOLD = byteArrayOf(0x1B, 0x21, (0x20 or 0x10 or 0x08).toByte())
        private fun autoGrayScale(bm: Bitmap, bitMarginLeft: Int, bitMarginTop: Int): ByteArray {
            val result: ByteArray
            val n = bm.height + bitMarginTop
            val offset = HEAD
            result = ByteArray(n * WIDTH + offset)
            for (y in 0 until bm.height) {
                for (x in 0 until bm.width) {
                    if (x + bitMarginLeft < BIT_WIDTH) {
                        val color = bm.getPixel(x, y)
                        val alpha = Color.alpha(color)
                        val red = Color.red(color)
                        val green = Color.green(color)
                        val blue = Color.blue(color)
                        if (alpha > 128 && (red < 128 || green < 128 || blue < 128)) {
                            // set the color black
                            val bitX = bitMarginLeft + x
                            val byteX = bitX / 8
                            val byteY = y + bitMarginTop
                            result[offset + byteY * WIDTH + byteX] =
                                ( result[offset + byteY * WIDTH + byteX] or ((0x80 shr( bitX - byteX * 8)).toByte()))
                        }
                    } else {
                        // ignore the rest data of this line
                        break
                    }
                }
            }
            return result
        }
    }
}
