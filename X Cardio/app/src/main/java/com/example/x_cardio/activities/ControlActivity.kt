package com.example.x_cardio.activities
import com.example.x_cardio.entities.HeartRate
import com.example.x_cardio.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*



const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
const val MESSAGE_CONNECTED: Int = 3
const val MESSAGE_CONNECTION_FAIL: Int = 4

const val COUNT_VALUE: Double = 125e-06
const val POSITION_SECS_VALUE: Double = 40e-03
const val MAX_X_GRAPH_VALUE: Int = 100

private const val TAG = "MY_APP_DEBUG_TAG"


class ControlActivity : AppCompatActivity() {

    companion object {
        //Bluetooth variables
        var MY_UUID:                        UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var m_bluetoothAdapter:    BluetoothAdapter
        var m_isConnected:                  Boolean   = false
        var m_address:                      String?   = null
        var myTextView:                     TextView? = null
        var bluetoothSocket:                MyBluetoothService.ConnectedThread? = null
        var connectionThread:               ConnectThread?      = null

        //GraphView variables
        lateinit var graphView: GraphView
        val series      = LineGraphSeries(arrayOf<DataPoint>())
        var xPosition   = 0
        var maxValue    = MAX_X_GRAPH_VALUE

        //Sample variables
        var staticData = ""
        var hr: HeartRate = HeartRate()
        lateinit var bpmProm: MutableList<Double>



        //Data simulation
        lateinit var simulatedData: Array<Int>
        var pointerSimulator = 0

    }

    val handler = object:  Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            when ( msg.what ) {

                MESSAGE_TOAST -> myTextView?.text = "Error"
                MESSAGE_CONNECTED -> myTextView?.text = "Connected"
                MESSAGE_CONNECTION_FAIL -> {

                    myTextView?.text = "Connection Failed"
                    connectionThread?.cancel()

                }
                MESSAGE_READ -> {
                    val readBuff = msg.obj as ByteArray
                    val tempMsg = String(readBuff)
                    myTextView?.text = "Connected and receiving"
                    if (tempMsg.length < 8)  processEntryData(tempMsg)

                }
            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        //device address from the previous layout
        m_address = intent.getStringExtra(DisplayMessageActivity.EXTRA_ADDRESS)

        //Local var
        var ledOnButton:    Button      = findViewById(R.id.ledOn)
        var ledOffButton:   Button      = findViewById(R.id.ledOff)
        var disconnectBt:   Button      = findViewById(R.id.disconnect)
        var bpmTextView:    TextView    = findViewById(R.id.bpm)


        //Global variables
        graphView           = findViewById(R.id.graph)
        myTextView          = findViewById(R.id.Rx)
        m_bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter()
        bpmProm             = mutableListOf<Double>()



        //Initialize bluetooth thread
        if (m_bluetoothAdapter.isEnabled) {
            connectionThread = ConnectThread(m_bluetoothAdapter.getRemoteDevice(m_address))
            connectionThread!!.start()
        }

        //Button functionality
        ledOffButton.setOnClickListener {
            bluetoothSocket?.write("b".toByteArray())
        }

        ledOnButton.setOnClickListener {
            bluetoothSocket?.write("a".toByteArray())
        }

        disconnectBt.setOnClickListener{
            connectionThread?.cancel()
            bluetoothSocket?.cancel()
        }


        //GraphView configuration
        configureGraphView(graphView)


        Thread() {//Este thread es para el calculo de bpm, cada 50ms, ademas simula datos

            while (true) {
                runOnUiThread { //addEntryData(simulateData(simulatedData)) // esta linea se borra cuando tenga el bt funcionando

                                //Initialize bpm function

                                setBpm(bpmTextView)}

                try {

                    Thread.sleep(25)

                } catch (e: InterruptedException) {
                    // manage error ...
                }
            }
        }.start()

    }

    private fun processEntryData(data: String){

        if( data.endsWith("\n") ){

            var finalData: String

            if( staticData == "" ){

                finalData = data

            }else{

                finalData = staticData + data
                staticData = ""

            }

            //BPM = finalData
            try {

                if(finalData != "" && finalData != null){
                    var n = finalData.removeSuffix("\n").toInt()
                    if(n != 0)  addEntryData(n)
                }


            }catch (e: IOException){

                    android.util.Log.e(TAG, "Could not convert ", e)
            }

        }else{

            staticData = data

        }

    }


    private fun setBpm(bpmTextView: TextView) {

        var BPM = getBpmProm()
        bpmTextView.text  = ""  + BPM

    }

    private fun getBpmProm(): String {

        var prom = 0.0

        for(bpm in bpmProm){

            prom += bpm

        }

        bpmProm.clear()

        return prom.toString()

    }


    private fun configureGraphView(graphView: GraphView){

        //esto esta en java deprecated, pasa q graphview es de java
        graphView.addSeries(series)
        graphView.isHorizontalScrollBarEnabled = true
        graphView.setBackgroundColor(resources.getColor(android.R.color.white))
        val viewport: Viewport = graphView.viewport
        viewport.isScrollable = true


    }


    private fun addEntryData(y: Int) {


       //graphViewData(y) //Procesamiento de grafica
        calculateBPM(y)//Procesamiento de BPM


    }

    private fun graphViewData(y: Int) {

        xPosition += 1


        if (xPosition == maxValue) {

            maxValue += MAX_X_GRAPH_VALUE
            series.resetData(arrayOf<DataPoint>(createPoint(xPosition, y)))

        } else {

            series.appendData(createPoint(xPosition, y), true, 8000)

        }

        graphView.addSeries(series)


    }

    private fun createPoint(x: Int, y: Int): DataPoint {

        return DataPoint(positionToSecs(x), countToVolt(y))

    }

    private fun countToVolt(count: Int): Double {

        return count*COUNT_VALUE

    }

    private fun positionToSecs(position: Int): Double{

        return position*POSITION_SECS_VALUE

    }

    
    private fun calculateBPM(count: Int) {


        var y = countToVolt(count)
        hr.addMeasurement( y )
        bpmProm.add( hr.calculateBPM() )


    }

    inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)

        }

        public override fun run() {

            m_bluetoothAdapter.cancelDiscovery()

            try {
                mmSocket?.let { socket ->
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    m_isConnected = true

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket)
                }
            }
            catch (e: IOException){

                Log.e(TAG, "Could not close the client socket", e)
                val message = Message.obtain()
                message.what = MESSAGE_CONNECTION_FAIL
                handler.sendMessage(message)

            }
        }


        //Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }



    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {

        val message = Message.obtain()
        message.what = MESSAGE_CONNECTED
        handler.sendMessage(message)

        bluetoothSocket = MyBluetoothService(handler).ConnectedThread(socket)
        bluetoothSocket!!.start()

    }



class MyBluetoothService(
        // handler that gets info from Bluetooth service
        private val handler: Handler
) {

    public inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                numBytes = mmInStream.available()
                if (numBytes>0)
                {
                    val b = ByteArray(numBytes)
                    // Read from the InputStream.
                    try {
                        mmInStream.read(b, 0, numBytes)
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        break
                    }

                    //mensaje = String(b, StandardCharsets.UTF_8)

                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(MESSAGE_READ, b)
                    readMsg.sendToTarget()




                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, -1, -1, mmBuffer
            )
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}}



