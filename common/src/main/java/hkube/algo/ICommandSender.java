package hkube.algo;

public interface ICommandSender {
    public void sendMessage(String command, Object data, boolean isError);
    public void addResponseListener(CommandResponseListener listener);
}
