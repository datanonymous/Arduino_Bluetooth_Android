package ko.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Controlling extends Activity {
    private static final String TAG = "BlueTest5-Controlling";
    private int mMaxChars = 50000;//Default//change this to string..........
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;


    private Button mBtnDisconnect;
    private BluetoothDevice mDevice;

    final static String on="92";//on
    final static String off="79";//off


    private ProgressDialog progressDialog;
    Button btnOn,btnOff;

    String receivedDataFromBluetooth;
    String readMessage;

    String strInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlling);

        ActivityHelper.initialize(this);
        // mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnOn = findViewById(R.id.on);
        btnOff = findViewById(R.id.off);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);

        Log.d(TAG, "Ready");

        btnOn.setOnClickListener(v -> {
            try {
                mBTSocket.getOutputStream().write(on.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        btnOff.setOnClickListener(v -> {
            try {
                mBTSocket.getOutputStream().write(off.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



        Button bluetoothButton = findViewById(R.id.bluetoothButton);
        TextView readBluetoothTV = findViewById(R.id.readBluetoothTV);
        bluetoothButton.setOnClickListener((View v) -> {
            try {
                mBTSocket.getInputStream();
                readBluetoothTV.setText(strInput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



//        Thread thread = new Thread(){
//            @Override
//            public void run(){
//                while(mBTSocket != null){
//                    try {
//                        Log.i("asdf", "asdfasdfasdf");
//                        mBTSocket.connect();
//                        mBTSocket.getInputStream();
//                        readBluetoothTV.setText(strInput);
//                        Thread.sleep(500);
//                    } catch (IOException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };



    } //end onCreate



    private class ReadInput implements Runnable {

        TextView readBluetoothTV = findViewById(R.id.readBluetoothTV);
        TextView humidityTV = findViewById(R.id.humidityTV);
        TextView temperatureTV = findViewById(R.id.temperatureTV);
        TextView heatindexTV = findViewById(R.id.heatindexTV);

        private boolean bStop = false;
        private Thread t;
        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }
        public boolean isRunning() {
            return t.isAlive();
        }
        @Override
        public void run() {
            InputStream inputStream;
            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                        int i = 0;
                        /*
                         * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
                         */
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
//                        final String strInput = new String(buffer, 0, i);
                        strInput = new String(buffer, 0, i);
                        Log.i("asdf", strInput);
                        /*
                         * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
                         */

                        //https://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
                        ///
                        runOnUiThread(() -> {
                            readBluetoothTV.setText(strInput);
                            String[]humidityTempHeat = strInput.split(",");
                            humidityTV.setText("Humidity (%): " + humidityTempHeat[0]);
                            temperatureTV.setText("Temperature (f): " + humidityTempHeat[1]);
                            heatindexTV.setText("Heat index (f): " + humidityTempHeat[2]);
                        });
                        ///

                    }
                    Thread.sleep(500);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        public void stop() {
            bStop = true;
        }
    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {//cant inderstand these dotss

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }

    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        Log.d(TAG, "Resumed");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(Controlling.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554

        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
// Unable to connect to device`
                // e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device. Please turn on your Hardware.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Connected to device!");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }
            progressDialog.dismiss();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

/*
LED in pin 13, other end to ground
HC-06 bluetooth module has RXD to pin 3, TXD to pin 2, GND to GND, and VCC to 5 volts.
DHT22 humidity/temperature sensor (with grills pointing towards me), from the left, the first pin goes to 5 volts, 2nd pin to pin 8, 2nd pin also to resistor connecting to 5 volts, and 4th pin to GND.

// Example testing sketch for various DHT humidity/temperature sensors
// Written by ladyada, public domain

// REQUIRES the following Arduino libraries:
// - DHT Sensor Library: https://github.com/adafruit/DHT-sensor-library
// - Adafruit Unified Sensor Lib: https://github.com/adafruit/Adafruit_Sensor

#include <SoftwareSerial.h>
#include "DHT.h"

#define DHTPIN 8     // Digital pin connected to the DHT sensor
// Feather HUZZAH ESP8266 note: use pins 3, 4, 5, 12, 13 or 14 --
// Pin 15 can work but DHT must be disconnected during program upload.

// Uncomment whatever type you're using!
//#define DHTTYPE DHT11   // DHT 11
#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
//#define DHTTYPE DHT21   // DHT 21 (AM2301)

// Connect pin 1 (on the left) of the sensor to +5V
// NOTE: If using a board with 3.3V logic like an Arduino Due connect pin 1
// to 3.3V instead of 5V!
// Connect pin 2 of the sensor to whatever your DHTPIN is
// Connect pin 4 (on the right) of the sensor to GROUND
// Connect a 10K resistor from pin 2 (data) to pin 1 (power) of the sensor

// Initialize DHT sensor.
// Note that older versions of this library took an optional third parameter to
// tweak the timings for faster processors.  This parameter is no longer needed
// as the current DHT reading algorithm adjusts itself to work on faster procs.
DHT dht(DHTPIN, DHTTYPE);

//Bluetooth stuff
SoftwareSerial Blue(2, 3);
long int data;
int LED = 13; // Led connected to pin 13
long int password1 = 92;// light on
long int password2 = 79; // light off


void setup() {
  Serial.begin(9600);
  Serial.println(F("DHTxx test!"));

  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);
  Blue.begin(9600);

  dht.begin();
}

void loop() {

//  Blue.print("12345678");

  // Wait a few seconds between measurements.
  delay(2000);

  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  float h = dht.readHumidity();
  // Read temperature as Celsius (the default)
  float t = dht.readTemperature();
  // Read temperature as Fahrenheit (isFahrenheit = true)
  float f = dht.readTemperature(true);

  // Check if any reads failed and exit early (to try again).
  if (isnan(h) || isnan(t) || isnan(f)) {
    Serial.println(F("Failed to read from DHT sensor!"));
    return;
  }

  // Compute heat index in Fahrenheit (the default)
  float hif = dht.computeHeatIndex(f, h);
  // Compute heat index in Celsius (isFahreheit = false)
  float hic = dht.computeHeatIndex(t, h, false);

  Serial.print(F("Humidity: "));
  Serial.print(h);
  Serial.print(F("% Temperature: "));
  Serial.print(t);
  Serial.print(F("°C "));
  Serial.print(f);
  Serial.print(F("°F Heat index: "));
  Serial.print(hic);
  Serial.print(F("°C "));
  Serial.print(hif);
  Serial.println(F("°F"));

//  Blue.print(F("Humidity: "));
//  Blue.print(h);
//  Blue.print(F("%  Temperature: "));
//  Blue.print(t);
//  Blue.print(F("°C "));
//  Blue.print(f);
//  Blue.print(F("°F  Heat index: "));
//  Blue.print(hic);
//  Blue.print(F("°C "));
//  Blue.print(hif);
//  Blue.println(F("°F"));

  Blue.print(h); //% humidity
  Blue.print(F(","));
  Blue.print(f); //temperature in F
  Blue.print(F(","));
  Blue.print(hif); //heat index in F

  if(Blue.available()>0){
    data = Blue.parseInt();
    }
  delay(1000);
  if (data == password1){
    digitalWrite(LED,HIGH);
    Serial.println("LED ON ");
    }

  if(data == password2){
    digitalWrite(LED,LOW);
    Serial.println("LED OFF");
    }

}//end loop()
 */