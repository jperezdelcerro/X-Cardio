package com.example.x_cardio.activities



import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.example.x_cardio.R

const val REQUEST_ENABLE_BT: Int = 1

class DisplayMessageActivity : AppCompatActivity() {


    companion object {
        const val EXTRA_ADDRESS: String = "Device_address"
        lateinit var bluetoothAdapter: BluetoothAdapter
        lateinit var imageBt: ImageView
        lateinit var switchBt: Switch
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_message)//seteo en que layout estoy

        imageBt = findViewById(R.id.btImage)
        switchBt = findViewById<Switch>(R.id.switchBt)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val refresh: Button = findViewById(R.id.ReloadBt)



        if ( !bluetoothAdapter?.isEnabled ) {

            val enableBtIntent = Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE )
            startActivityForResult( enableBtIntent, REQUEST_ENABLE_BT )

        }

        bluetoothSetUp()


        refresh.setOnClickListener{ pairedDeviceList() }

        Thread() {//este thread va a tener que quedar para actualizar la cuenta del BPM

            while (true) {
                runOnUiThread {

                    when(switchBt.isChecked){
                        true -> {imageBt.setImageResource(R.drawable.bluetoothison)}
                        false -> {imageBt.setImageResource(R.drawable.bluetoothisoff)}
                    }


                }

                // sleep to slow down the add of entries
                try {

                    Thread.sleep(100)

                } catch (e: InterruptedException) {
                    // manage error ...
                }
            }
        }.start()
    }





    private fun enableBluetooth(){


        if ( !bluetoothAdapter?.isEnabled ) {

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            Toast.makeText(this, "Enabling bluetooth", Toast.LENGTH_LONG).show()

        }
    }


    private fun pairedDeviceList() {
        Toast.makeText(this, "Loading available devices", Toast.LENGTH_LONG).show()
        val deviceList: ListView = findViewById(R.id.devicesList)

        val pairedDevices: MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            device.name
            device.address // MAC address
        }

        val pairedDevicesStringList : ArrayList<String> = ArrayList()
        val pairedDevicesList : ArrayList<BluetoothDevice> = ArrayList()

        if (pairedDevices != null) {
            for (device in pairedDevices) {
                pairedDevicesStringList.add( device.name +"\n" + device.address )
            }
        }


        if (pairedDevices != null) {

            pairedDevicesList.addAll(pairedDevices)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,pairedDevicesStringList )

        deviceList.adapter = adapter
        deviceList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = pairedDevicesList[position]
            val address: String = device.address
            ControlActivity.m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)


        }


    }



    private fun bluetoothSetUp() {

        when(bluetoothAdapter?.isEnabled){
            true -> {switchBt.isChecked = true
                    pairedDeviceList()}
            false -> {switchBt.isChecked = false}
        }


        switchBt?.setOnCheckedChangeListener { _, isChecked ->

            if(isChecked) {
                if (!bluetoothAdapter?.isEnabled) {
                    enableBluetooth()

                    if (!bluetoothAdapter.isDiscovering) {
                        Toast.makeText(this, "Making your device discoverable", Toast.LENGTH_LONG).show()
                        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                        }
                        startActivity(discoverableIntent)
                    }
                    pairedDeviceList()
            }else{
                bluetoothAdapter?.disable()
            }
    }
}}}

