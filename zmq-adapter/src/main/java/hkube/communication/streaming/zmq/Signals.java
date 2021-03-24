package hkube.communication.streaming.zmq;

import java.util.HashMap;
import java.util.Map;

public enum Signals {
    PPP_INIT("\001"),  // Signals worker is ready
    PPP_HEARTBEAT("\002"),  // Signals worker heartbea
    PPP_DISCONNECT("\003"),//Disconnect
    PPP_READY("\004"),// Signals worker is not ready
    PPP_NOT_READY("\005"), // Signals worker is not ready
    PPP_DONE("\006"),
    PPP_EMPTY("\007"),
    PPP_MSG(new String(new byte[]{8})),
    PPP_DONE_DISCONNECT(new String(new byte[]{9}));
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
