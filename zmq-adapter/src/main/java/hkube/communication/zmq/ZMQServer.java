package hkube.communication.zmq;

import hkube.communication.IRequestListener;
import hkube.communication.IRequestServer;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;


public class ZMQServer implements IRequestServer {
    private ZMQ.Socket socket = null;
    private List<IRequestListener> listeners = new ArrayList();
    Thread thread;
    public ZMQServer(ZMConfiguration config) {
        ZContext context = new ZContext();
        socket = context.createSocket(SocketType.REP);
        socket.bind("tcp://*:" + config.getListeningPort());
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    // Block until a message is received
                    byte[] request = socket.recv(0);
                    listeners.forEach((listener) -> {
                        listener.onRequest(request);
                    });
                }
            }
        });
        thread.start();
    }

    @Override
    public void addRequestsListener(IRequestListener listener) {
        listeners.add(listener);
    }

    @Override
    public void reply(byte[] reply) {
        socket.send(reply, 0);
    }
    public void close(){
        thread.interrupt();
        socket.close();
    }
}
