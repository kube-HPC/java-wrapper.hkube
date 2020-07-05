package hkube.algo;

import org.json.JSONObject;

import java.util.EventListener;
import java.util.Map;

public interface CommandResponseListener extends EventListener {
    public void onCommand(String command, Map data);
}
