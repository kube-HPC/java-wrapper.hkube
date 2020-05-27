package hkube.communication.zmq;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.concurrent.TimeoutException;

public class ZMQRequest {
    ZMQ.Socket socket;

    public ZMQRequest(String host, String port, ZMConfiguration config) {
        ZContext context = new ZContext();

        socket = context.createSocket(SocketType.REQ);
        //  Socket to talk to server
        socket.setReceiveTimeOut( config.getTimeout());
        socket.setSendTimeOut(config.getTimeout());

        socket.connect("tcp://" + host + ":" + port);
    }

    public byte[] send(byte[] data) throws TimeoutException {
        socket.send(data, 0);
        byte[] reslut = socket.recv();
        if(reslut == null){
            throw new TimeoutException();
        }
        return reslut;
    }
    public void close(){
        socket.close();
    }
}
