package hkube.algo.wrapper;

import org.json.JSONObject;

import java.util.EventListener;

public interface CommandResponseListener extends EventListener {
    public void onCommand(String command, JSONObject data);
}
