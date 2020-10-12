package hkube.communication.zmq;


import hkube.model.HeaderContentPair;
import hkube.communication.ICommConfig;
import hkube.communication.IRequest;
import org.apache.logging.log4j.LogManager;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ZMQRequest implements IRequest {
    ZMQ.Socket socket;
    ZMQ.Socket pingSocket;
    String remoteURL;
    Integer timeOut;
    Integer networkTimeOut;
    private static final Logger logger = LogManager.getLogger();

    public ZMQRequest(String host, String port, ICommConfig config) {
        ZContext context = new ZContext();

        socket = context.createSocket(ZMQ.REQ);
        pingSocket = context.createSocket(ZMQ.REQ);
        timeOut = config.getTimeout();
        networkTimeOut = config.getNetworkTimeout();

        //  Socket to talk to server

        remoteURL = "tcp://" + host + ":" + port;
        socket.connect(remoteURL);
        String pingUrl = "tcp://" + host + ":" + (Integer.valueOf(port) + 1);
        pingSocket.connect(pingUrl);
    }

    public List<HeaderContentPair> send(byte[] data) throws TimeoutException {
        pingSocket.setReceiveTimeOut(networkTimeOut);
        pingSocket.setSendTimeOut(networkTimeOut);
        pingSocket.send("ping".getBytes(), 0);
        byte[] reslut = pingSocket.recv();
        if (reslut == null || !new String(reslut).equals("pong")) {
            throw new TimeoutException();
        }
        socket.setReceiveTimeOut(timeOut);
        socket.setSendTimeOut(timeOut);
        socket.send(data, 0);
        List<HeaderContentPair> headerContentPairs = new ArrayList<HeaderContentPair>();
        byte[] header = socket.recv();
        byte[] body = socket.recv();
        headerContentPairs.add(new HeaderContentPair(header, body));
        boolean hasMore = socket.hasReceiveMore();
        while (hasMore) {
            header = socket.recv();
            body = socket.recv();
            headerContentPairs.add(new HeaderContentPair(header, body));
            hasMore = socket.hasReceiveMore();
        }
        if (header == null || body == null) {
            throw new TimeoutException();
        }
        logger.debug("reply from " + remoteURL);
        return headerContentPairs;
    }

    public void close() {
        socket.close();
    }
}
