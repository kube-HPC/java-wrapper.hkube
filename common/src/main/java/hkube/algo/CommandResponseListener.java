package hkube.algo;

import java.util.EventListener;

public interface CommandResponseListener extends EventListener {
    public void onCommand(String command, Object data, boolean isDebug);
}
