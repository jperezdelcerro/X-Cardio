package com.example.x_cardio.fragments

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import com.example.x_cardio.R
import com.google.android.material.snackbar.Snackbar

class SplashFragment : Fragment() {

    private lateinit var v: View
    private val SPLASH_TIME_OUT:Long = 3000 // 3 sec



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v =  inflater.inflate(R.layout.fragment_splash, container, false)

        Handler().postDelayed(

            {

                val action = SplashFragmentDirections.actionSplashFragmentToDisplayMessageActivity()
                v.findNavController().navigate(action)

            }
            , SPLASH_TIME_OUT)


        return v
    }

    private fun bluetoothConfiguration() {

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()


        if (bluetoothAdapter == null) {
            Snackbar.make(v,"Bluetooth is not available for this device", Snackbar.LENGTH_LONG).show()

        } else {
            Snackbar.make(v,"Bluetooth is available for this device", Snackbar.LENGTH_LONG).show()
        }
    }

}