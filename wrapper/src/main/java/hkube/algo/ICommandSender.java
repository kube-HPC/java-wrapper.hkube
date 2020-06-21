package hkube.algo;

import org.json.JSONObject;

public interface ICommandSender {
    public void sendMessage(String command, JSONObject data, boolean isError);
    public void addResponseListener(CommandResponseListener listener);
}
