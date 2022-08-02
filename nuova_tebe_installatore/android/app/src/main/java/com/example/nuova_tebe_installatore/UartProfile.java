package com.example.nuova_tebe_installatore;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

public class UartProfile {
    static String TAG="UART_TAG";
    /* Current Time Service UUID */
    public static UUID UART_SERVICE = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0");
    /* Mandatory Current Time Information Characteristic */
    public static UUID UART_CHARACTERISTIC = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0");

    public static BluetoothGattService createUartService() {
        Log.w(TAG, "uart create");
        BluetoothGattService service = new BluetoothGattService(UART_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        Log.w(TAG, "uart service");
        Log.w(TAG, service.getUuid().toString());


        // Current Time characteristic
        BluetoothGattCharacteristic uart = new BluetoothGattCharacteristic(UART_CHARACTERISTIC,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        Log.w(TAG, "aggiungo uart characteristic");

        service.addCharacteristic(uart);
        Log.w(TAG, "uart characteristic");
       // Log.w(TAG, uart.getValue().toString());
        Log.w(TAG, uart.getUuid().toString());


        return service;
    }
}
