package pl.edu.pjwstk.engineeringthesis.bluetooth;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** One newline-delimited JSON packet from the ESP32. */
public class Packet {
    public final List<Float> temps;
    public final List<Integer> gsr;

    public final long epoch;
    public final JSONObject raw;

    public Packet(ArrayList<Float> temps, ArrayList<Integer> gsr, long epoch, JSONObject raw) {
        this.temps = temps;
        this.gsr = gsr;
        this.epoch = epoch;
        this.raw = raw;
    }
}