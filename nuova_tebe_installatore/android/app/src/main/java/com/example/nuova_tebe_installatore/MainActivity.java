package com.example.nuova_tebe_installatore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.os.IBinder;
import android.util.Log;
import android.widget.RadioButton;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends FlutterActivity {
    private final static String TAG = "GATT SERVER";
    private static final String CHANNEL = "com.example.nuova_tebe_installatore/GATTserver";
    private String STATUS = "SEND ENABLE CONF";
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;

    private int FwOffset = 0;
    private int FwIDX = 0;
    private int FwPhase = 0;
    private byte mFirmwareData[];
    private int mFirmwareLen = 0;
    private MethodChannel methodChannel;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private String numInmpianto="25";
    private String flag = "NFC+125KHz";
    byte[] cmd = null;
    public String numImpiantoInverted = "";
    Boolean esito= false;
    int retry=3;
    Boolean iniziaIncremento=false;



    public void setNumeroImpianto(String nImp){
        numInmpianto=nImp;
    }
    public String getNumeroImpianto(){
        return numInmpianto;
    }

    public void setFlag(String f){
        flag=f;
    }

    public String getFlag(){
        return flag;
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {

        super.configureFlutterEngine(flutterEngine);
        methodChannel= new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel.setMethodCallHandler(
                        (call, result) -> {
                            if (call.method.equals("startGattServer")) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                                }
                                mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                        
                                // Register for system Bluetooth events
                                IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                                Log.w(TAG, "Registro receiver");
                        
                                registerReceiver(mBluetoothReceiver, filter);
                                startServer();
                                result.success("servizio startato");
                            } else if(call.method.equals("setPlant")) {
                                Log.w(TAG, (String) call.arguments);
                                setNumeroImpianto((String) call.arguments);
                                result.success("plant settato");
                            }
                            else if(call.method.equals("setFlag")) {
                                Log.w(TAG, (String) call.arguments);
                                setFlag((String) call.arguments);
                                result.success("flag settato");
                            }
                            else if(call.method.equals("setStatus")) {
                                Log.w(TAG, (String) call.arguments);
                                setSTATUS((String) call.arguments);
                                if(((String) call.arguments).equalsIgnoreCase("SEND FIRMWARE")){
                                    iniziaIncremento=false;
                                    FwOffset=0;
                                    FwIDX=0;
                                    FwPhase=0;
                                }
                                result.success("stato settato");
                            }
                            else if(call.method.equals("setFile")) {
                                FwOffset = 0;

                                String fwString= (String) call.arguments;
                                fwString= removeFirstandLast(fwString);
                                fwString=fwString.replaceAll(" ","");
                                String[] fwListString= fwString.split(",");
                                Log.w(TAG,"setFile call method"+ fwListString[0] + " , "+ fwListString[1] + " , "+ fwListString[2]);
                                mFirmwareLen= fwListString.length;
                                Log.w(TAG, "Lunghezza stream byte "+String.valueOf(mFirmwareLen));

                                mFirmwareData=new byte[(int) 200000];
                                for(int i=0; i<fwListString.length; i++){

                                    mFirmwareData[i]=(byte) Integer.parseInt(fwListString[i]);

                                }
                                Log.w(TAG,"M FW DATA "+ mFirmwareData[0] + " , "+ mFirmwareData[1] );

                                Log.w(TAG,"VETTORE LENGHT"+  String.valueOf(mFirmwareData.length));

                                result.success("file di byte settato");
                            }
                            else if(call.method.equals("setFileLenght")) {
                                mFirmwareLen= (int) call.arguments;
                                Log.w(TAG, (String) call.arguments);
                                result.success("lunghezza settato");
                            }
                            else
                            {
                              result.notImplemented();
                            }
                        }
                );

    }


    public static String removeFirstandLast(String str)
    {

        // Creating a StringBuilder object
        StringBuilder sb = new StringBuilder(str);

        // Removing the last character
        // of a string
        sb.deleteCharAt(str.length() - 1);

        // Removing the first character
        // of a string
        sb.deleteCharAt(0);

        // Converting StringBuilder into a string
        // and return the modified string
        return sb.toString();
    }









    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }




    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    break;
                default:
                    // Do nothing
            }

        }
    };


    private void startServer() {
        Log.w(TAG, "Start GATT server");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permessi non concessi");
Log.w(TAG, String.valueOf(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)));
          //  return;
        }
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        Log.w(TAG, "Ho aperto il gatto");

        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            //return;
        }

        mBluetoothGattServer.addService(UartProfile.createUartService());
    }

    private void stopServer() {
        if (mBluetoothGattServer == null) return;
        Log.w(TAG, "stop GATT server");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mBluetoothGattServer.close();
    }

//    BluetoothGatt gattMy;


    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);

            }
        }



        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            // Invalid characteristic
            //Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
        }

        public int byteArrayToInt(byte[] b) {
            return b[3] & 0xFF |
                    (b[2] & 0xFF) << 8 |
                    (b[1] & 0xFF) << 16 |
                    (b[0] & 0xFF) << 24;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {


        //    Log.w(TAG, "SCRIVO su onCharacteristicWriteRequest " + value);



            int chunkdim = 128;
            //int chunkdim = 16;

            //Log.w(TAG, "Characteristic Write: " + characteristic.getUuid());
            String text = new String(value, StandardCharsets.US_ASCII);
         //   Log.w(TAG, "text from value " + text);

            //Log.w(TAG, text);
            //txtnumImpianto = ((TextView) findViewById(R.id.txtnumImpianto));
            if (text.contentEquals("PING!\0")) {
                Log.w(TAG, "Comunicazione ok");

            }
            else if((value[0] == (byte) 0x56 && value[1] == (byte) 0x3A)){


                int version=Byte.toUnsignedInt(value[2])+Byte.toUnsignedInt((byte) (value[3]<<8))+Byte.toUnsignedInt((byte) (value[3]<<16))+Byte.toUnsignedInt((byte) (value[3]<<24));


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        methodChannel.invokeMethod("read_version", String.valueOf(version));
                    }
                });
            }
            else if (text.contentEquals("C:ACK\0") || text.contentEquals("NACK\0") || (value[0] == (byte) 0x43 && value[1] == (byte) 0x80) || (value[0] == (byte) 0x43 && value[1] == (byte) 0x81)) {

                Log.w(TAG, "ack o nack: "+ text);



                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF READ")) {

                    cmd = new byte[]{(byte) 0x43, (byte) 0x80};
                    setSTATUS("SEND READ FLAG");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("send", cmd);
                        }
                    });
                } else if (getSTATUS().equalsIgnoreCase("SEND READ FLAG")) {

                    Log.w(TAG, value.toString());

                    if (!text.contentEquals("NACK\0")) {

                    if (value[2] == (byte) 0x01) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("read_flag", 1);
                            }
                        });
                    } else if (value[2] == (byte) 0x00) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("read_flag", 0);
                            }
                        });
                    } else if (value[2] == (byte) 0x02) {
                        ///nuovo caso da gestire
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("read_flag", 2);
                            }
                        });
                    }


                    cmd = new byte[]{(byte) 0x43, (byte) 0x81};
                    setSTATUS("SEND READ PLANT");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("send", cmd);
                        }
                    });

                }

                } else if (getSTATUS().equalsIgnoreCase("SEND READ PLANT")) {
                    Log.w(TAG, value.toString());
                    numImpiantoInverted="";

                    if (!text.contentEquals("NACK\0")) {


                        byte[] ab = new byte[]{value[2], value[3], value[4], value[5]};
                        byte[] inverted = new byte[4];
                        int index = 0;
                        for (int z = ab.length - 1; z >= 0; z--) {
                            inverted[index] = ab[z];
                            index++;
                        }
                        Integer nii = byteArrayToInt(inverted);
                        String nitext = nii.toString();
                        for (int i = nitext.length(); i > 0; i--) {
                            numImpiantoInverted += nitext.charAt(i - 1);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("read_num_impianto", numImpiantoInverted);
                            }
                        });
                    }
                    else
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("read_num_impianto", "Lettura fallita");
                            }
                        });
                    }



                }


                if(getSTATUS().equalsIgnoreCase("REBOOT")){
                    cmd = new byte[]{(byte) 0x43, (byte) 0x82};
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            methodChannel.invokeMethod("send", cmd);
                        }
                    });
                }

                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF")) {

                   if( text.contentEquals("C:ACK\0"))
                   {
                       Log.w(TAG,"Scrittura enable config avvenuta correttamente");
                       esito=true;
                   }
                   else if(text.contentEquals("NACK\0"))
                   {
                       Log.w(TAG,"Scrittura enable config fallita");
                       esito=false;
                   }

                   ///mettere da chiamata da flutter
                    if (flag.equalsIgnoreCase("NFC+125KHz")) {
                        cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        setSTATUS("SEND CMD FLAG");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send", cmd);
                            }
                        });

                    } else if (flag.equalsIgnoreCase("NFC+125KHz+13MHz")) {
                        cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        setSTATUS("SEND CMD FLAG");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send", cmd);
                            }
                        });
                    }
                    else if (flag.equalsIgnoreCase("125KHz+13MHz")) {
                        cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        setSTATUS("SEND CMD FLAG");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send", cmd);
                            }
                        });
                    }



                } else if (getSTATUS().equalsIgnoreCase("SEND CMD FLAG")) {
                    Log.w(TAG, "SEND CMD FLAG");
                    if( text.contentEquals("C:ACK\0"))
                    {
                        Log.w(TAG,"Scrittura SEND CMD FLAG avvenuta correttamente");
                            esito=true;
                    }
                    else if(text.contentEquals("NACK\0"))
                    {
                        Log.w(TAG,"Scrittura SEND CMD FLAG fallita");
                        esito=false;
                    }

                    cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

                    //Invertire la stringa
                    String numImpiantoInverted = "";
                    for (int i = numInmpianto.length(); i > 0; i--) {
                        numImpiantoInverted += numInmpianto.charAt(i - 1);
                    }

                    if (!numInmpianto.isEmpty()) {
                        Integer ni = Integer.parseInt(numImpiantoInverted);
                        byte[] bytesimp = ByteBuffer.allocate(4).putInt(ni).array();
                        byte[] inverted = new byte[6];

                        int index = 2;
                        inverted[0] = (byte) 0x43;
                        inverted[1] = (byte) 0x01;
                        for (int z = bytesimp.length - 1; z >= 0; z--) {
                            inverted[index] = bytesimp[z];
                            index++;
                        }
                        Log.w(TAG, "ritorno byte impianto" + inverted.toString());

                        setSTATUS("SEND CMD PLANT");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.w(TAG, "sono nell'ui thread");

                                methodChannel.invokeMethod("send", inverted);
                            }
                        });


                    }
                } else if (getSTATUS().equalsIgnoreCase("SEND CMD PLANT")) {
                        //last one if everything correct return esito true
                    if( text.contentEquals("C:ACK\0"))
                    {
                        Log.w(TAG,"Scrittura SEND CMD PLANT avvenuta correttamente");
                        if(esito)
                            esito=true;
                    }
                    else if(text.contentEquals("NACK\0"))
                    {
                        Log.w(TAG,"Scrittura SEND CMD PLANT fallita");
                        esito=false;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "sono nell'ui thread");

                            methodChannel.invokeMethod("scrittura_corretta", esito);
                        }
                    });


                }


            } else if (text.contentEquals("U:ACK\0") || text.contentEquals("U:COK\0") || text.contentEquals("U:NACK\0")) {
                if (getSTATUS().equalsIgnoreCase("SEND FIRMWARE")) {

                    if (FwPhase == 0 || (text.contentEquals("U:NACK\0") && FwPhase == 1)) {
           //             Log.w(TAG, "FASE 0 con FwFase="+FwPhase);

                        if(text.contentEquals("U:NACK\0"))
                        {
                            retry=retry-1;
                        }
                        else
                        {
                            retry=3;
                            FwPhase = 1;
                        }
                        cmd = new byte[]{(byte) 0x55, (byte) 0x3}; // erase signature
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send_fw_chunk", cmd);
                            }
                        });
                    } else if (FwPhase == 1 || (text.contentEquals("U:NACK\0") && FwPhase == 2) ) {
                     //   Log.w(TAG, "FASE 1 con FwFase="+FwPhase);

                        if(text.contentEquals("U:NACK\0"))
                        {
                            retry=retry-1;
                        }
                        else
                        {
                            retry=3;
                            FwPhase = 2;
                        }
                        cmd = new byte[]{(byte) 0x55, (byte) 0x4}; // erase flash
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send_fw_chunk", cmd);
                            }
                        });
                    } else if (FwPhase == 2 || (text.contentEquals("U:NACK\0") && FwPhase == 3) ) {
                       // Log.w(TAG, "FASE 2 con FwFase="+FwPhase);

                        if(text.contentEquals("U:NACK\0"))
                        {
                            retry=retry-1;
                        }
                        else
                        {
                            retry=3;
                            FwPhase = 3;
                        }
                        cmd = new byte[]{(byte) 0x55, (byte) 0x2, (byte) 0xa5, (byte) 0x5a};
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send_fw_chunk", cmd);
                            }
                        });
                    }
                    else if (FwOffset < mFirmwareLen) {

                     //   Log.w(TAG, "FASE 3 (SCRITTURA FILE EFFETTIVA) con FwFase="+FwPhase);

                        if(text.contentEquals("U:NACK\0"))
                        {
                            Log.w(TAG, "FACCIO RETRY, ricevuto NACK");
                            retry=retry-1;
                        }
                        else
                        {
                            retry=3;
                            if(iniziaIncremento) {
                                FwOffset += chunkdim;
                                FwIDX++;
                      //          Log.w(TAG, "FwOffset="+ FwOffset);
                            }
                        }


                    if(FwOffset < mFirmwareLen) {

                        if (FwOffset == 0) FwIDX = 0;

                        cmd = new byte[4 + chunkdim];
                        cmd[0] = 0x55;
                        if ((mFirmwareLen - FwOffset) > chunkdim) {
                            cmd[1] = 0x00;
                        } else {
                            Log.w(TAG, "ULTIMO PACCHETTO con fwlen: " + String.valueOf(mFirmwareLen) + " e fwOffset: " + FwOffset);
                            cmd[1] = 0x01;  // ultimo pacchetto
                        }

                        if ((FwIDX > 0) && ((FwIDX & 0xff) == 0)) FwIDX++;
                        cmd[2] = (byte) (FwIDX & 0xff);
                        cmd[3] = (byte) (FwIDX >> 8);
                        //Log.w(TAG, "ID: L=" + String.format("%02x", cmd[2]) + " H=" + String.format("%02x", cmd[3]));
                        if ((mFirmwareLen - FwOffset) >= chunkdim) {
                            System.arraycopy(mFirmwareData, FwOffset, cmd, 4, chunkdim);
                        } else {
                            System.arraycopy(mFirmwareData, FwOffset, cmd, 4, mFirmwareLen - FwOffset);
                            for (int i = 4 + mFirmwareLen - FwOffset; i < cmd.length; i++)
                                cmd[i] = (byte) 0xff;
                            // L'ultimo pacchetto non restituisce ACK perché il lettore si disconnette
                        }

                        //setSTATUS("SEND FIRMWARE");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                methodChannel.invokeMethod("send_fw_chunk", cmd);
                                  methodChannel.invokeMethod("updatePBar", FwOffset + chunkdim);
                            }
                        });


               //         Log.w(TAG, String.valueOf(FwOffset));

                        if (FwOffset == 0) {
                            iniziaIncremento = true;
                        }
                    }
                    else
                    {
                        Log.w(TAG, "ho fwOffset>= Fwlen");
                    }
                } else {
                        Log.w(TAG, "FWOFFSET HA SUPERATO FWLEN");
                        iniziaIncremento=false;
                        FwPhase = 0;
                        FwOffset=0;
                    }

                }
            } else {

                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF"))
                    Log.w(TAG, "Errore configurazione");
                else if (getSTATUS().equalsIgnoreCase("SEND CMD FLAG"))
                    Log.w(TAG, "Errore salvataggio flag");
                else if (getSTATUS().equalsIgnoreCase("SEND CMD PLANT"))
                    Log.w(TAG, "Errore salvataggio impianto");
                else if (getSTATUS().equalsIgnoreCase("SEND FIRMWARE")) {
                    Log.w(TAG, "Errore aggiornamento firmware");
                    FwPhase = 0;
                    FwOffset=0;
                    iniziaIncremento=false;
                } else
                    Log.w(TAG, "Errore di comunicazione");
            }

            if(retry<=0){
                Log.w(TAG, "retry esaurito, STACCAH STACCAH");
                FwPhase = 0;
                FwOffset=0;
                iniziaIncremento=false;
                retry=3;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {

            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
            //            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
            //            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopServer();
        unregisterReceiver(mBluetoothReceiver);

    }


}
