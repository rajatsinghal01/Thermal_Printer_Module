package com.agnext.thermalprinter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class PrintData {
    private val util: PrinterUtil
    private val printer: BluetoothDevice?


    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------
    constructor(context: Context?) {
       shpref = context!!.getSharedPreferences("MAIN",MODE_PRIVATE)
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter()
        printer = bluetoothAdapter!!.getRemoteDevice(shpref!!.getString("Printer_Address","")!!)
        util = PrinterUtil(printer)
    }

    constructor(context: Context?, printerName: String) {
        shpref = context!!.getSharedPreferences("MAIN",MODE_PRIVATE)
        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter()
        printer = bluetoothAdapter!!.getRemoteDevice(shpref!!.getString("Printer_Address","")!!)
        util = PrinterUtil(printer)
    }

    val connectedPrinter: BluetoothDevice?
        get() = getPrinter()

    fun connect(onConnected: OnConnected?, onFailed: OnFailed?) {
        util.connectPrinter( object : PrinterUtil.PrinterConnected{
            override fun onConnected() {
                if(onConnected!=null){
                    onConnected.onConnected(this@PrintData)
                }
            }

        },
            object : PrinterUtil.PrinterConnectFailed {
                override fun onFailed() {
                    if(onFailed!=null)
                        onFailed?.onFailed("Failed to connect printer")
                }

            }
        )
    }

    val isConnected: Boolean
        get() = util.isConnected

    fun close() {
        setNormalText()
        Handler().postDelayed(util::finish, 2000)
    }

//    //----------------------------------------------------------------------------------------------
//    // PRINT TEST
//    //----------------------------------------------------------------------------------------------
    fun printTest() {
        printData!!.connect(object : OnConnected {
            override fun onConnected(printData: PrintData?) {
                printData!!.setNormalText()
                printData.printTextln("------------------", CENTER)
                printData.printTextln("Print Test", CENTER)
                printData.printTextln("------------------", CENTER)
                printData.feedPaper()
                printData.close()
            }
        },null)
    }

    //----------------------------------------------------------------------------------------------
    // PRINTER COMMANDS
    //----------------------------------------------------------------------------------------------
    fun setLineSpacing(lineSpacing: Int) {
        util.setLineSpacing(lineSpacing)
    }

    fun feedPaper() {
        util.feedPaper()
    }

    fun printDashedLine() {
        util.setAlign(LEFT)
        util.printText("--------------------------------")
    }

    fun printLine() {
        util.setAlign(LEFT)
        util.printText("________________________________")
    }

    fun printDoubleDashedLine() {
        util.setAlign(LEFT)
        util.printText("================================")
    }

    fun addNewLine() {
        util.addNewLine()
    }

    fun addNewLine(count: Int) {
        util.addNewLine(count)
    }

    //----------------------------------------------------------------------------------------------
    // PRINT IMAGE BITMAP
    //----------------------------------------------------------------------------------------------
    fun printImage(bitmap: Bitmap?): Boolean {
        return util.printImage(bitmap!!)
    }

    fun printImage(alignment: Int, bitmap: Bitmap?, width: Int): Boolean {
        return util.printImage(alignment, bitmap!!, width)
    }

    fun printImage(bitmap: Bitmap?, width: Int, alignment: Int): Boolean {
        return util.printImage(alignment, bitmap!!, width)
    }

    fun printImage(bitmap: Bitmap?, width: Int): Boolean {
        return util.printImage(bitmap!!, width)
    }

    fun printFromView(view: View) {
        val vto = view.viewTreeObserver
        val viewWidth = AtomicInteger(view.measuredWidth)
        val viewHeight = AtomicInteger(view.measuredHeight)
        vto.addOnGlobalLayoutListener {
            viewWidth.set(view.measuredWidth)
            viewHeight.set(view.measuredHeight)
        }
        Handler().postDelayed({
            loadBitmapAndPrint(
                view,
                viewWidth.get(),
                viewHeight.get()
            )
        }, 500)
    }

    private fun loadBitmapAndPrint(view: View, viewWidth: Int, viewHeight: Int) {
        val b = loadBitmapFromView(view, viewWidth, viewHeight)
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute { printData!!.printImage(b) }
    }

    fun loadBitmapFromView(view: View, viewWidth: Int, viewHeight: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        val ma = ColorMatrix()
        ma.setSaturation(0f)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(ma)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bitmap
    }

    fun printText(align: Int, text: String?) {
        util.setAlign(align)
        util.printText(text)
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT
    //----------------------------------------------------------------------------------------------
    @JvmOverloads
    fun printText(text: String?, align: Int = LEFT) {
        util.setAlign(align)
        util.printText(text)
    }

    fun printTextln(align: Int, text: String) {
        util.setAlign(align)
        printTextln(text)
    }

    fun printTextln(text: String, align: Int) {
        util.setAlign(align)
        printTextln(text)
    }

    fun printTextln(text: String) {
        var text = text
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT JUSTIFY ALIGNMENT
    //----------------------------------------------------------------------------------------------
    fun printTextJustify(text1: String, text2: String) {
        val justifiedText = getJustifiedText(text1, text2)
        printText(justifiedText)
    }

    fun printTextJustify(text1: String, text2: String, text3: String) {
        val justifiedText = getJustifiedText(text1, text2, text3)
        printText(justifiedText)
    }

    fun printTextJustify(text1: String, text2: String, text3: String, text4: String) {
        val justifiedText = getJustifiedText(text1, text2, text3, text4)
        printText(justifiedText)
    }

    fun printTextJustifyBold(text1: String, text2: String) {
        val justifiedText = getJustifiedText(text1, text2)
        printTextBold(justifiedText)
    }

    fun printTextJustifyBold(text1: String, text2: String, text3: String) {
        val justifiedText = getJustifiedText(text1, text2, text3)
        printTextBold(justifiedText)
    }

    fun printTextJustifyBold(text1: String, text2: String, text3: String, text4: String) {
        val justifiedText = getJustifiedText(text1, text2, text3, text4)
        printTextBold(justifiedText)
    }

    private fun getJustifiedText(text1: String, text2: String): String {
        var justifiedText = ""
        justifiedText = text1 + getSpaces(text1, text2) + text2
        return justifiedText
    }

    private fun getJustifiedText(text1: String, text2: String, text3: String): String {
        var justifiedText = ""
        val text12 = text1 + getSpaces(text1, text2, text3) + text2
        justifiedText = text12 + getSpaces(text12, text3) + text3
        return justifiedText
    }

    private fun getJustifiedText(
        text1: String,
        text2: String,
        text3: String,
        text4: String
    ): String {
        var justifiedText = ""
        val text12 = text1 + getSpaces(text1, text2, text3, text4) + text2
        val text123 = text12 + getSpaces(text12, text3, text4) + text3
        justifiedText = text123 + getSpaces(text123, text4) + text4
        return justifiedText
    }

    private fun getSpaces(text1: String, text2: String): String {
        val text1Length = text1.length
        val text2Length = text2.length
        val spacesCount = MAX_CHAR - text1Length - text2Length
        val spaces = StringBuilder()
        for (i in 0 until spacesCount) {
            spaces.append(" ")
        }
        return spaces.toString()
    }

    private fun getSpaces(text1: String, text2: String, text3: String): String {
        val text1Length = text1.length
        val text2Length = text2.length
        val text3Length = text3.length
        val spacesCount = (MAX_CHAR - text1Length - text2Length - text3Length) / 2
        val spaces = StringBuilder()
        for (i in 0 until spacesCount) {
            spaces.append(" ")
        }
        return spaces.toString()
    }

    private fun getSpaces(text1: String, text2: String, text3: String, text4: String): String {
        val text1Length = text1.length
        val text2Length = text2.length
        val text3Length = text3.length
        val text4Length = text4.length
        val spacesCount = (MAX_CHAR - text1Length - text2Length - text3Length - text4Length) / 3
        val spaces = StringBuilder()
        for (i in 0 until spacesCount) {
            spaces.append(" ")
        }
        return spaces.toString()
    }

    //----------------------------------------------------------------------------------------------
    // PRINT TEXT WITH FORMATTING
    //----------------------------------------------------------------------------------------------
    // Normal
    fun printTextNormal(text: String?) {
        setNormalText()
        printText(text, LEFT)
    }

    fun printTextNormal(align: Int, text: String?) {
        setNormalText()
        util.setAlign(align)
        util.printText(text)
    }

    fun printTextNormal(text: String?, align: Int) {
        setNormalText()
        util.setAlign(align)
        util.printText(text)
    }

    fun printTextlnNormal(align: Int, text: String) {
        setNormalText()
        util.setAlign(align)
        printTextln(text)
    }

    fun printTextlnNormal(text: String, align: Int) {
        setNormalText()
        util.setAlign(align)
        printTextln(text)
    }

    fun printTextlnNormal(text: String) {
        var text = text
        setNormalText()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
    }

    // Bold
    fun printTextBold(text: String?) {
        setBold()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextBold(align: Int, text: String?) {
        setBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextBold(text: String?, align: Int) {
        setBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }


    fun printTextlnBold(align: Int, text: String) {
        setBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnBold(text: String, align: Int) {
        setBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnBold(text: String) {
        var text = text
        setBold()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // Tall
    fun printTextTall(text: String?) {
        setTall()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextTall(align: Int, text: String?) {
        setTall()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextTall(text: String?, align: Int) {
        setTall()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnTall(align: Int, text: String) {
        setTall()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnTall(text: String, align: Int) {
        setTall()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnTall(text: String) {
        var text = text
        setTall()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // TallBold
    fun printTextTallBold(text: String?) {
        setTallBold()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextTallBold(align: Int, text: String?) {
        setTallBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextTallBold(text: String?, align: Int) {
        setTallBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnTallBold(align: Int, text: String) {
        setTallBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnTallBold(text: String, align: Int) {
        setTallBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnTallBold(text: String) {
        var text = text
        setTallBold()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // Wide
    fun printTextWide(text: String?) {
        setWide()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextWide(align: Int, text: String?) {
        setWide()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextWide(text: String?, align: Int) {
        setWide()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnWide(align: Int, text: String) {
        setWide()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWide(text: String, align: Int) {
        setWide()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWide(text: String) {
        var text = text
        setWide()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // WideBold
    fun printTextWideBold(text: String?) {
        setWideBold()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextWideBold(align: Int, text: String?) {
        setWideBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextWideBold(text: String?, align: Int) {
        setWideBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnWideBold(align: Int, text: String) {
        setWideBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideBold(text: String, align: Int) {
        setWideBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideBold(text: String) {
        var text = text
        setWideBold()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // WideTall
    fun printTextWideTall(text: String?) {
        setWideTall()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextWideTall(align: Int, text: String?) {
        setWideTall()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextWideTall(text: String?, align: Int) {
        setWideTall()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnWideTall(align: Int, text: String) {
        setWideTall()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideTall(text: String, align: Int) {
        setWideTall()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideTall(text: String) {
        var text = text
        setWideTall()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    // WideTallBold
    fun printTextWideTallBold(text: String?) {
        setWideTallBold()
        printText(text, LEFT)
        setNormalText()
    }

    fun printTextWideTallBold(align: Int, text: String?) {
        setWideTallBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextWideTallBold(text: String?, align: Int) {
        setWideTallBold()
        util.setAlign(align)
        util.printText(text)
        setNormalText()
    }

    fun printTextlnWideTallBold(align: Int, text: String) {
        setWideTallBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideTallBold(text: String, align: Int) {
        setWideTallBold()
        util.setAlign(align)
        printTextln(text)
        setNormalText()
    }

    fun printTextlnWideTallBold(text: String) {
        var text = text
        setWideTallBold()
        text = """
               $text
               
               """.trimIndent()
        util.printText(text)
        setNormalText()
    }

    //----------------------------------------------------------------------------------------------
    // TEXT FORMAT
    //----------------------------------------------------------------------------------------------
    fun setNormalText() {
        util.setNormalText()
    }

    fun setSmallText() {
        util.setSmallText()
    }

    fun setBold() {
        util.setBold()
    }

    fun setUnderline() {
        util.setUnderline()
    }

    fun setDeleteLine() {
        util.setDeleteLine()
    }

    fun setTall() {
        util.setTall()
    }

    fun setWide() {
        util.setWide()
    }

    fun setWideBold() {
        util.setWideBold()
    }

    fun setTallBold() {
        util.setTallBold()
    }

    fun setWideTall() {
        util.setWideTall()
    }

    fun setWideTallBold() {
        util.setWideTallBold()
    }

    //----------------------------------------------------------------------------------------------
    // INTERFACES
    //----------------------------------------------------------------------------------------------
    interface OnConnected {
        fun onConnected(printData: PrintData?)
    }

    interface OnFailed {
        fun onFailed(message: String?)
    }

    interface OnConnectPrinter {
        fun onConnectPrinter(printerName: String?)
    }

    interface Callback {
        fun printama(printData: PrintData?)
    }

    companion object {
        const val CENTER = -1
        const val RIGHT = -2
        const val LEFT = 0
        const val FULL_WIDTH = -1
        const val ORIGINAL_WIDTH = 0
        const val GET_PRINTER_CODE = 921
        private const val MAX_CHAR = 32
        private const val MAX_CHAR_WIDE = MAX_CHAR / 2
        private var printData: PrintData? = null
        var shpref : SharedPreferences ?=null
        var bluetoothAdapter : BluetoothAdapter?=null
        fun with(context: Context?, callback: Callback): PrintData {

            val printData = PrintData(context)
            callback.printama(printData)
            return printData
        }

        fun with(context: Context?): PrintData? {
            shpref = context!!.getSharedPreferences("MAIN",MODE_PRIVATE)
            bluetoothAdapter= BluetoothAdapter.getDefaultAdapter()
            printData = PrintData(context)
            return printData
        }

        fun with(context: Context?, printerName: String): PrintData? {
            printData = PrintData(context, printerName)
            return printData
        }

        private fun getPrinter(): BluetoothDevice? {
            return getPrinter(bluetoothAdapter!!.getRemoteDevice(shpref!!.getString("Printer_Address","")!!).name.toString())
        }

        private fun getPrinter(printerName: String): BluetoothDevice? {
            val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
            var printer: BluetoothDevice? = null
            if (defaultAdapter == null) return null
            for (device in defaultAdapter.bondedDevices) {
                if (device.name.equals(printerName, ignoreCase = true)) {
                    printer = device
                }
            }
            return printer
        }

        fun getBitmapFromVector(context: Context?, drawableId: Int): Bitmap? {
            val drawable = ContextCompat.getDrawable(context!!, drawableId)
            return getBitmapFromVector(drawable)
        }

        fun getBitmapFromVector(drawable: Drawable?): Bitmap? {
            var drawable = drawable
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = if (drawable != null) DrawableCompat.wrap(drawable).mutate() else null
            }
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                return bitmap
            }
            return null
        }
    }
}
