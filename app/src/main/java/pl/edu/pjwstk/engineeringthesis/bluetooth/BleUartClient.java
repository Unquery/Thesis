package pl.edu.pjwstk.engineeringthesis.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class BleUartClient {

    public interface Listener {
        default void onStatus(String s) {}
        default void onConnected(String address, int mtu) {}
        default void onDisconnected() {}
        default void onPacket(Packet p) {}
        default void onError(String msg, Throwable t) {}
    }

    private static final String TAG = "BleUartClient";
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context appCtx;
    private final UUID serviceUuid;
    private final UUID txUuid;

    private Listener listener;

    private final BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic txChar;

    private String deviceNameFilter;
    private final StringBuilder lineBuf = new StringBuilder();

    public BleUartClient(Context ctx,
                         BluetoothAdapter adapter,
                         UUID serviceUuid,
                         UUID txUuid) {
        this.appCtx = ctx.getApplicationContext();
        this.serviceUuid = serviceUuid;
        this.txUuid = txUuid;
        this.scanner = adapter != null ? adapter.getBluetoothLeScanner() : null;
    }

    public void setListener(Listener l) { this.listener = l; }

    public void setDeviceNameFilter(String name) { this.deviceNameFilter = name; }

    public boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= 31)
            return permGranted(Manifest.permission.BLUETOOTH_SCAN);
        else
            return permGranted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(appCtx,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void notifyErr(String msg) { if (listener != null) listener.onError(msg, null); }
    private void notifyErr(String msg, Exception e) { if (listener != null) listener.onError(msg, e); }


    private boolean permGranted(String p) {
        return ContextCompat.checkSelfPermission(appCtx, p) == PackageManager.PERMISSION_GRANTED;
    }

    // ---------- Public API ----------
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (!hasScanPermission()) {
            throw new SecurityException("Scan permission not granted");
        }

        if (scanner == null) {
            if (listener != null) listener.onError("No BLE scanner available", null);
            return;
        }
        if (listener != null) listener.onStatus("Scanning…");

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUuid))
                .build());
        if (deviceNameFilter != null && !deviceNameFilter.isEmpty()) {
            filters.add(new ScanFilter.Builder().setDeviceName(deviceNameFilter).build());
        }
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(filters, settings, scanCb);
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        try {
            if (scanner != null && hasScanPermission()) scanner.stopScan(scanCb);
        } catch (SecurityException ignored) {}
    }


    // ---------- Internals ----------
    private final ScanCallback scanCb = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = (result.getScanRecord() != null) ? result.getScanRecord().getDeviceName() : device.getName();

            if (deviceNameFilter == null || deviceNameFilter.equals(name)) {
                try { if (scanner != null) scanner.stopScan(this); } catch (Exception ignored) {}
                if (listener != null) listener.onStatus("Connecting…");

                if (!hasConnectPermission())
                    throw new SecurityException("Connect permission not granted");

                gatt = device.connectGatt(appCtx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
            }
        }

        @Override public void onScanFailed(int errorCode) {
            if (listener != null) listener.onError("Scan failed: " + errorCode, null);
        }
    };

    private boolean ensureConnectPermOrNotify(String op) {
        if (!hasConnectPermission()) {
            if (listener != null) listener.onError("Missing BLUETOOTH_CONNECT for " + op, null);
            return false;
        }
        return true;
    }

    private void safeGatt(String op, Runnable r) {
        if (!ensureConnectPermOrNotify(op)) return;
        try { r.run(); }
        catch (SecurityException se) {
            if (listener != null) listener.onError("SecurityException during " + op, se);
        }
    }

    private final BluetoothGattCallback gattCb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                if (listener != null) listener.onStatus("Discovering services…");
                safeGatt("discoverServices", g::discoverServices);
            } else {
                if (listener != null) listener.onStatus("Disconnected");
                disconnect();
            }
        }


        @Override
        public void onServicesDiscovered(final BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) { notifyErr("Service discovery failed: " + status); return; }
            BluetoothGattService svc = g.getService(serviceUuid);
            txChar = (svc != null) ? svc.getCharacteristic(txUuid) : null;
            if (txChar == null) { notifyErr("UART TX characteristic not found"); return; }

            enableNotifications(g, (txChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0);

            if (hasConnectPermission()) {
                try {
                    g.requestMtu(185);
                } catch (SecurityException se) {
                    notifyErr("SecurityException during requestMtu", se);
                }
            } else {
                notifyErr("Missing BLUETOOTH_CONNECT for requestMtu", null);
            }
        }



        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            if (listener != null) listener.onStatus("MTU=" + mtu);
            boolean useIndicate = (txChar != null) &&
                    ((txChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0);
            enableNotifications(g, useIndicate);
            if (listener != null) listener.onConnected(g.getDevice().getAddress(), mtu);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor d, int status) {
            if (CCCD_UUID.equals(d.getUuid())) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (listener != null) listener.onStatus("Subscribed; waiting for data…");
                } else {
                    if (listener != null) listener.onError("Subscribe failed: " + status, null);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (c.getUuid().equals(txUuid)) handleNotify(c.getValue());
        }
    };

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt g, boolean useIndicate) {
        if (!hasConnectPermission()) return;
        try {
            boolean ok = g.setCharacteristicNotification(txChar, true);
            BluetoothGattDescriptor cccd = txChar.getDescriptor(CCCD_UUID);
            if (!ok || cccd == null) {
                if (listener != null) listener.onError("Failed to enable notifications", null);
                return;
            }
            cccd.setValue(useIndicate ?
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(cccd);
        } catch (SecurityException se) {
            if (listener != null) listener.onError("No permission for notifications", se);
        }
    }

    private void handleNotify(byte[] bytes) {
        String chunk = new String(bytes, StandardCharsets.UTF_8);
        lineBuf.append(chunk);

        int nl = indexOfNewline(lineBuf);
        while (nl != -1) {
            String line = lineBuf.substring(0, nl);
            lineBuf.delete(0, nl + 1);
            try {
                Packet p = JsonPacketParser.parse(line);
                if (listener != null && p != null) listener.onPacket(p);
            } catch (Exception e) {
                if (listener != null) listener.onError("JSON parse error: " + e.getMessage(), e);
                Log.e(TAG, "Bad line: " + line);
            }
            nl = indexOfNewline(lineBuf);
        }
    }

    private static int indexOfNewline(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '\n' || c == '\r') return i;
        }
        return -1;
    }

    @SuppressLint("MissingPermission")
    private void disconnect() {
        try { if (gatt != null && hasConnectPermission()) gatt.disconnect(); } catch (Exception ignored) {}
        try { if (gatt != null) gatt.close(); } catch (Exception ignored) {}
        gatt = null; txChar = null;
        if (listener != null) listener.onDisconnected();
    }
}
