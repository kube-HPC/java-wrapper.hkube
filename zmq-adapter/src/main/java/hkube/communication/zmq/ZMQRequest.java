package hkube.communication.zmq;



import hkube.communication.ICommConfig;
import hkube.communication.IRequest;
import org.apache.logging.log4j.LogManager;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeoutException;

public class ZMQRequest implements IRequest {
    ZMQ.Socket socket;
    String remoteURL;
    Integer timeOut;
    Integer networkTimeOut;
    private static final Logger logger = LogManager.getLogger();

    public ZMQRequest(String host, String port, ICommConfig config) {
        ZContext context = new ZContext();

        socket = context.createSocket(SocketType.REQ);
        timeOut = config.getTimeout();
        networkTimeOut = config.getNetworkTimeout();

        //  Socket to talk to server

        remoteURL = "tcp://" + host + ":" + port;
        socket.connect(remoteURL);
    }

    public byte[] send(byte[] data) throws TimeoutException {
        socket.setReceiveTimeOut( networkTimeOut);
        socket.setSendTimeOut(networkTimeOut);
        socket.send("Are you there".getBytes(), 0);
        byte[] reslut = socket.recv();
        if(reslut == null ||  !new String(reslut).equals("Yes")){
            throw new TimeoutException();
        }
        socket.setReceiveTimeOut( timeOut);
        socket.setSendTimeOut(timeOut);
        socket.send(data, 0);
        reslut = socket.recv();
        if(reslut == null){
            throw new TimeoutException();
        }
        logger.debug("reply from "+remoteURL);
        return reslut;
    }
    public void close(){
        socket.close();
    }
}
