package pl.edu.pjwstk.engineeringthesis.bluetooth;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Packet {
    public final List<Float> temps;
    public final List<Integer> gsr;
    public final List<Float> hearthRate;
    public final List<Float> spo2;

    public final long epoch;
    public final JSONObject raw;

    public Packet(ArrayList<Float> temps, ArrayList<Integer> gsr, ArrayList<Float> hearthRate, ArrayList<Float> spo2, long epoch, JSONObject raw) {
        this.temps = temps;
        this.gsr = gsr;
        this.hearthRate = hearthRate;
        this.spo2 = spo2;
        this.epoch = epoch;
        this.raw = raw;
    }
}
