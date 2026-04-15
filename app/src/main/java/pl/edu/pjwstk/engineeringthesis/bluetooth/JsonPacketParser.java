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

        ArrayList<Float> temps = new ArrayList<>();
        if (tArr != null) {
            for (int i=0; i<tArr.length(); i++) temps.add((float) tArr.getDouble(i));
        }
        ArrayList<Float> gsr = new ArrayList<>();
        if (gArr != null) {
            for (int i=0; i<gArr.length(); i++) gsr.add((float) gArr.getDouble(i));
        }
        ArrayList<Float> hearthRate = new ArrayList<>();
        if (hrArr != null) {
            for (int i=0; i<hrArr.length(); i++) hearthRate.add((float) hrArr.getDouble(i));
        }
        ArrayList<Float> spo2 = new ArrayList<>();
        if (spo2Arr != null) {
            for (int i=0; i<spo2Arr.length(); i++) spo2.add((float) spo2Arr.getDouble(i));
        }
        long epoch = obj.optLong("epoch", -1L);

        return new Packet(temps, gsr, hearthRate, spo2, epoch, obj);
    }
}
