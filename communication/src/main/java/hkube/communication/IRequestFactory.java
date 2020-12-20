package hkube.communication;

public interface IRequestFactory {
    IRequest getRequest (String host, String port, ICommConfig config);
}
