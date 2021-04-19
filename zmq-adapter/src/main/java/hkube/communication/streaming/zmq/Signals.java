package hkube.communication.streaming.zmq;

import java.util.HashMap;
import java.util.Map;

public enum Signals {
    PPP_READY("\001"),  // Signals worker is ready
    PPP_DONE("\002"),  // Signals worker heartbea
    PPP_EMPTY("\003"),//Disconnect
    PPP_MSG("\004"),// Signals worker is not ready
    PPP_NO_MSG("\005") // Signals worker is not ready
    ;
    private String value;
    static Map <String,Signals>all = new HashMap();
    static {
        for (Signals signal : Signals.values()) {
            all.put(signal.toString(), signal);
        }
    }
    Signals(String value) {
        this.value = value;

    }

    public String toString() {
        return value;
    }


    public byte[] toBytes() {
        return value.getBytes();
    }
    public static Signals getByBytes (byte[] signalAsBytes){
        return all.get(new String(signalAsBytes));
    }
}
