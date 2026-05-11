package pl.edu.pjwstk.engineeringthesis.bluetooth;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public final class JsonPacketParser {

    public static Packet parse(String line) throws Exception {
        if (line == null || line.isEmpty()) return null;
        JSONObject obj = new JSONObject(line);

        JSONArray tArr = obj.optJSONArray("temp");
        if (tArr == null) tArr = obj.optJSONArray("t");
        JSONArray gArr = obj.optJSONArray("gsr");
        JSONArray hrArr = obj.optJSONArray("hr");
        JSONArray spo2Arr = obj.optJSONArray("spo2");

        ArrayList<Float> temps = readFloatArray(tArr);
        ArrayList<Float> gsr = readFloatArray(gArr);
        ArrayList<Float> hearthRate = readFloatArray(hrArr);
        ArrayList<Float> spo2 = readFloatArray(spo2Arr);
        long epoch = obj.optLong("epoch", -1L);

        return new Packet(temps, gsr, hearthRate, spo2, epoch, obj);
    }

    private static ArrayList<Float> readFloatArray(JSONArray arr) throws Exception {
        ArrayList<Float> values = new ArrayList<>();
        if (arr == null) return values;

        for (int i = 0; i < arr.length(); i++) {
            if (arr.isNull(i)) continue;

            double value = arr.getDouble(i);
            if (Double.isFinite(value)) {
                values.add((float) value);
            }
        }
        return values;
    }
}
