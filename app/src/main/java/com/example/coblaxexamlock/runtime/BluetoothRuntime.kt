package com.example.coblaxexamlock.runtime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


internal fun getBluetoothConnectPermission(): String = "android.permission.BLUETOOTH_CONNECT"

internal fun requiresBluetoothExamPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

internal fun hasBluetoothExamPermission(context: Context): Boolean {
    return !requiresBluetoothExamPermission() ||
        ContextCompat.checkSelfPermission(
            context,
            getBluetoothConnectPermission()
        ) == PackageManager.PERMISSION_GRANTED
}

internal fun isBluetoothEnabledForExam(context: Context): Boolean {
    if (!hasBluetoothExamPermission(context)) {
        return false
    }

    return runCatching {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        bluetoothAdapter?.isEnabled == true
    }.getOrDefault(false)
}

internal fun getBluetoothAdapterStateLabel(context: Context): String {
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java) ?: return "Tidak tersedia"
    val adapter = bluetoothManager.adapter ?: return "Tidak didukung"
    return when (adapter.state) {
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
        BluetoothAdapter.STATE_ON -> "ON"
        BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
        else -> "UNKNOWN"
    }
}

@SuppressLint("MissingPermission")
internal fun getBluetoothConnectedDevicesCount(context: Context): Int? {
    if (!hasBluetoothExamPermission(context)) {
        return null
    }

    val bluetoothManager = context.getSystemService(BluetoothManager::class.java) ?: return null
    return runCatching {
        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size +
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).size +
            bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET).size +
            bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP).size
    }.getOrNull()
}

@SuppressLint("MissingPermission")
internal fun isBluetoothA2dpOrHeadsetConnected(context: Context): Boolean? {
    if (!hasBluetoothExamPermission(context)) {
        return null
    }

    val bluetoothManager = context.getSystemService(BluetoothManager::class.java) ?: return null
    return runCatching {
        bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP).isNotEmpty() ||
            bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET).isNotEmpty()
    }.getOrNull()
}
