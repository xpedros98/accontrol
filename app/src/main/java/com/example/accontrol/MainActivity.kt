package com.example.accontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textfield.TextInputEditText
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager // Only one is required to manage all sensors.
    private lateinit var cm: ConnectivityManager
    private lateinit var activeNetwork: NetworkInfo

    var connectBtn: Button? = null
    var accVals: TextView? = null
    var fb: TextView? = null
    var inputText: TextInputEditText? = null

    var x_axis = 0F
    var y_axis = 0F

    var mqttConnected = false

    private lateinit var mqttClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Define the GUI objects.
        connectBtn = findViewById(R.id.connectBtn)
        connectBtn?.isEnabled = false
        accVals = findViewById(R.id.accVals)
        fb = findViewById(R.id.feedback)
        inputText = findViewById(R.id.input)

        // Check if the permission are granted. Ask for them if they are not already granted.
        if (ActivityCompat.checkSelfPermission( this,  Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_NETWORK_STATE), 71) // Arbitrary request code.
        } else {
            connectBtn?.isEnabled = true
        }

        if (ActivityCompat.checkSelfPermission( this,  Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 72) // Arbitrary request code.
        } else {
            connectBtn?.isEnabled = true
        }

        if (ActivityCompat.checkSelfPermission( this,  Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WAKE_LOCK), 73) // Arbitrary request code.
        } else {
            connectBtn?.isEnabled = true
        }


        cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo

        setUpSensor()

        connectBtn?.setOnClickListener {
            if (activeNetwork?.isConnectedOrConnecting == true) {
                if (mqttConnected) {
                    publish("test", inputText?.text.toString())
                }
                else {
                    val serverURI = "tcp://postman.cloudmqtt.com:11183"
                    mqttClient = MqttAndroidClient(applicationContext, serverURI, "kotlin_client")
                    mqttClient.setCallback(object : MqttCallback {
                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            fb?.text = "Topic: $topic | Message: $message"
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}

                        override fun connectionLost(cause: Throwable?) {
                            fb?.text = "Connection lost $cause"
                            mqttConnected = false
                            connectBtn?.text = "Connect"
                        }
                    })
                    val mqttConnectOptions = MqttConnectOptions()
                    mqttConnectOptions.userName = "xxsclbis"
                    mqttConnectOptions.password = "ipMfZRSJZk9l".toCharArray()
                    mqttConnectOptions.isAutomaticReconnect = true
                    try {
                        mqttClient.connect(mqttConnectOptions, applicationContext, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                fb?.text = "Connection success."
                                mqttConnected = true
                                connectBtn?.text = "Send"
                            }

                            override fun onFailure(
                                asyncActionToken: IMqttToken?,
                                exception: Throwable?
                            ) {
                                fb?.text = "Connection failure."
                                mqttConnected = false
                                connectBtn?.text = "Connect"
                            }
                        })
                    } catch (e: MqttException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 71 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(baseContext, "Acces netowrk allowed.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                baseContext,
                "PROBLEMS: acces netowrk permission not granted.",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (requestCode == 72 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(baseContext, "Internet enabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                baseContext,
                "PROBLEMS: internet permission not granted.",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (requestCode == 73 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(baseContext, "Wake lock enabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                baseContext,
                "PROBLEMS: wake lock permission not granted.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setUpSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        //
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            x_axis = event.values[0]
            y_axis = event.values[1]

            accVals?.text = "${x_axis.toInt()}, ${y_axis.toInt()}"
            if (mqttConnected) {
                if (x_axis.toInt() > 0 && y_axis.toInt() > 0) {
                    publish("test", "B")
                }

                if (x_axis.toInt() < 0 && y_axis.toInt() > 0) {
                    publish("test", "Y")
                }

                if (x_axis.toInt() < 0 && y_axis.toInt() < 0) {
                    publish("test", "G")
                }

                if (x_axis.toInt() > 0 && y_axis.toInt() < 0) {
                    publish("test", "R")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    fb?.text = "'$msg' published to '$topic'."
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    fb?.text = "Failed to publish '$msg' to '$topic'."
                }
            })
        } catch (e: MqttException) {
            fb?.text = "Failed to publish '$msg' to '$topic'."
        }
    }

}