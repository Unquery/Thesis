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

        ArrayList<Float> temps = new ArrayList<>();
        if (tArr != null) {
            for (int i=0; i<tArr.length(); i++) temps.add((float) tArr.getDouble(i));
        }
        ArrayList<Integer> gsr = new ArrayList<>();
        if (gArr != null) {
            for (int i=0; i<gArr.length(); i++) gsr.add(gArr.getInt(i));
        }
        long epoch = obj.optLong("epoch", -1L);

        return new Packet(temps, gsr, epoch, obj);
    }
}