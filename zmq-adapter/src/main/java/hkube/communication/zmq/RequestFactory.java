package hkube.communication.zmq;

import hkube.communication.ICommConfig;
import hkube.communication.IRequest;
import hkube.communication.IRequestFactory;

public class RequestFactory implements IRequestFactory {
    @Override
    public IRequest getRequest(String host, String port, ICommConfig config) {
        return new ZMQRequest(host,port,config);
    }
}
