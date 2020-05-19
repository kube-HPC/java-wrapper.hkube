package hkube.algo;

import org.json.JSONObject;

public interface ICommandSender {
    public void sendMessage(String command, JSONObject data);
    public void addResponseListener(CommandResponseListener listener);
}
