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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BleUartClient {

    public interface Listener {
        default void onStatus(String s) {}
        default void onConnected(String address, int mtu) {}
        default void onDisconnected() {}
        default void onPacket(Packet p) {}
        default void onError(String msg, Throwable t) {}
        default void onDeviceFound(String address, String name, int rssi) {}
    }

    private static final String TAG = "BleUartClient";
    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID DEFAULT_NUS_RX =
            UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");

    private final Context appCtx;
    private final BluetoothAdapter adapter;
    private final UUID serviceUuid;
    private final UUID txUuid;
    private final UUID rxUuid;

    private Listener listener;

    private final BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic txChar;

    private BluetoothGattCharacteristic rxChar;

    private String deviceNameFilter;
    private final StringBuilder lineBuf = new StringBuilder();

    private final Map<String, BluetoothDevice> found = new HashMap<>();
    private volatile boolean connected = false;

    private boolean pushTimeOnConnect = true;
    private boolean timePushedThisConn = false;

    public boolean isConnected() { return connected; }

    public BleUartClient(Context ctx,
                         BluetoothAdapter adapter,
                         UUID serviceUuid,
                         UUID txUuid) {
        this(ctx, adapter, serviceUuid, txUuid, DEFAULT_NUS_RX);
    }

    public BleUartClient(Context ctx,
                         BluetoothAdapter adapter,
                         UUID serviceUuid,
                         UUID txUuid,
                         UUID rxUuid) {
        this.appCtx = ctx.getApplicationContext();
        this.adapter = adapter;
        this.serviceUuid = serviceUuid;
        this.txUuid = txUuid;
        this.rxUuid = rxUuid != null ? rxUuid : DEFAULT_NUS_RX;
        this.scanner = adapter != null ? adapter.getBluetoothLeScanner() : null;
    }

    public void setListener(Listener l) { this.listener = l; }

    public void setDeviceNameFilter(String name) { this.deviceNameFilter = name; }

    public void setPushTimeOnConnect(boolean enable) { this.pushTimeOnConnect = enable; }

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

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (!hasScanPermission()) {
            throw new SecurityException("Scan permission not granted");
        }

        if (scanner == null) {
            if (listener != null) listener.onError("No BLE scanner available", null);
            return;
        }
        found.clear();
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

    @SuppressLint("MissingPermission")
    public void connect(String address) {
        if (!hasConnectPermission()) throw new SecurityException("Connect permission not granted");
        BluetoothDevice dev = resolveDevice(address);
        if (dev == null) { notifyErr("Device unavailable: " + address, null); return; }
        stop();
        closeCurrentGatt();
        timePushedThisConn = false;
        gatt = dev.connectGatt(appCtx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
    }

    private BluetoothDevice resolveDevice(String address) {
        BluetoothDevice dev = found.get(address);
        if (dev != null) return dev;
        if (adapter == null) return null;
        try {
            return adapter.getRemoteDevice(address);
        } catch (IllegalArgumentException iae) {
            notifyErr("Invalid device address: " + address, iae);
            return null;
        }
    }

    private void closeCurrentGatt() {
        closeGattSafely();
        gatt = null;
        txChar = null;
        rxChar = null;
        connected = false;
    }
    @SuppressLint("MissingPermission")
    public void pushEpochNow() {
        pushEpoch(System.currentTimeMillis() / 1000L);
    }

    @SuppressLint("MissingPermission")
    public void pushEpoch(long epochSeconds) {
        if (gatt == null || rxChar == null) { notifyErr("RX char not ready", null); return; }
        if (!hasConnectPermission()) { notifyErr("Missing BLUETOOTH_CONNECT for write", null); return; }

        byte[] payload = ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(epochSeconds)
                .array();

        int props = rxChar.getProperties();
        int writeType = (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                gatt.writeCharacteristic(rxChar, payload, writeType);
            } else {
                rxChar.setWriteType(writeType);
                rxChar.setValue(payload);
                gatt.writeCharacteristic(rxChar);
            }
            if (listener != null) listener.onStatus("Pushed time " + epochSeconds + " (type=" + writeType + ")");
        } catch (SecurityException se) {
            notifyErr("SecurityException during writeCharacteristic", se);
        }
    }

    private final ScanCallback scanCb = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            String name = (result.getScanRecord() != null) ? result.getScanRecord().getDeviceName() : device.getName();
            int rssi = result.getRssi();
            if (deviceNameFilter == null || deviceNameFilter.equals(name)) {
                try { if (scanner != null) scanner.stopScan(this); } catch (Exception ignored) {}
                found.put(address, device);
                if (listener != null) listener.onDeviceFound(address, name, rssi);
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
                connected = true;
                if (listener != null) listener.onConnected(g.getDevice().getAddress(), 23);
                if (listener != null) listener.onStatus("Discovering services…");
                safeGatt("discoverServices", g::discoverServices);
            } else {
                connected = false;
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

            rxChar = svc.getCharacteristic(rxUuid);
            if (rxChar == null) {
                if (listener != null) listener.onStatus("RX characteristic not found (time sync disabled)");
            }

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
            pendingTimePush = true;
        }


        private boolean pendingTimePush = false;

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            if (listener != null) listener.onStatus("MTU=" + mtu);
            boolean useIndicate = (txChar != null) &&
                    ((txChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0);
            enableNotifications(g, useIndicate);
            if (listener != null) listener.onConnected(g.getDevice().getAddress(), mtu);

            maybePushTime(g);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor d, int status) {
            if (CCCD_UUID.equals(d.getUuid())) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (listener != null) listener.onStatus("Subscribed; waiting for data…");
                    if (pendingTimePush) {
                        pendingTimePush = false;
                        maybePushTime(g);
                    }
                } else {
                    if (listener != null) listener.onError("Subscribe failed: " + status, null);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (c.getUuid().equals(txUuid)) handleNotify(c.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            if (c.getUuid().equals(rxUuid)) {
                if (listener != null) listener.onStatus("onCharacteristicWrite RX status=" + status);
                if (status != BluetoothGatt.GATT_SUCCESS) notifyErr("Write failed: " + status, null);
            }
        }
    };

    private void maybePushTime(BluetoothGatt g) {
        if (!pushTimeOnConnect || timePushedThisConn || rxChar == null) return;
        if (!hasConnectPermission()) return;
        long epoch = System.currentTimeMillis() / 1000L;
        if (listener != null) listener.onStatus("Pushing epoch " + epoch + "…");
        timePushedThisConn = true;
        pushEpoch(epoch);
    }

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt g, boolean useIndicate) {
        if (!hasConnectPermission() || txChar == null) return;
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

        String json = extractNextJsonObject(lineBuf);
        while (json != null) {
            try {
                Packet p = JsonPacketParser.parse(json);
                if (listener != null && p != null) listener.onPacket(p);
            } catch (Exception e) {
                if (listener != null) listener.onError("JSON parse error: " + e.getMessage(), e);
                Log.e(TAG, "Bad JSON packet: " + json, e);
            }
            json = extractNextJsonObject(lineBuf);
        }
    }

    private static String extractNextJsonObject(StringBuilder sb) {
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);

            if (start == -1) {
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (c == '{') {
                    start = i;
                    depth = 1;
                } else {
                    sb.deleteCharAt(i);
                    i--;
                }
                continue;
            }

            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String json = sb.substring(start, i + 1);
                    sb.delete(0, i + 1);
                    return json;
                }
            }
        }

        if (start > 0) {
            sb.delete(0, start);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        try {
            if (gatt != null && hasConnectPermission()) gatt.disconnect();
        } catch (SecurityException se) {
            notifyErr("SecurityException during disconnect", se);
        }
        closeGattSafely();
        gatt = null; txChar = null;  rxChar = null; connected = false;
        if (listener != null) listener.onDisconnected();
    }

    private void closeGattSafely() {
        if (gatt == null) return;
        if (!hasConnectPermission()) return;
        try {
            gatt.close();
        } catch (SecurityException se) {
            notifyErr("SecurityException during close", se);
        }
    }
}
