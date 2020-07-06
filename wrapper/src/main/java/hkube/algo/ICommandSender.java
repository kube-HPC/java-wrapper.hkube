package hkube.algo;

import org.json.JSONObject;

import java.util.Map;

public interface ICommandSender {
    public void sendMessage(String command, Object data, boolean isError);
    public void addResponseListener(CommandResponseListener listener);
}
