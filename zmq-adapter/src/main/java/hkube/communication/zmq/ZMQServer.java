package hkube.communication.zmq;

import hkube.model.HeaderContentPair;
import hkube.communication.ICommConfig;
import hkube.communication.IRequestListener;
import hkube.communication.IRequestServer;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;


public class ZMQServer implements IRequestServer {
    private ZMQ.Socket socket = null;
    private ZMQ.Socket pingSocket = null;
    private List<IRequestListener> listeners = new ArrayList();
    Thread thread;
    Thread pingThread;

    public ZMQServer(ICommConfig config) {
        ZContext context = new ZContext();
        socket = context.createSocket(SocketType.REP);
        pingSocket = context.createSocket(SocketType.REP);
        socket.bind("tcp://*:" + config.getListeningPort());
        pingSocket.bind("tcp://*:" + (Integer.valueOf(config.getListeningPort()) + 1));
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

        pingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    // Block until a message is received
                    byte[] request = pingSocket.recv(0);
                    if (new String(request).equals("ping")) {
                        pingSocket.send("pong".getBytes());
                    }
                }
            }
        });
        pingThread.start();
    }

    @Override
    public void addRequestsListener(IRequestListener listener) {
        listeners.add(listener);
    }

    @Override
    public void reply(List<HeaderContentPair> replies) {
        for (int i = 0; i < replies.size(); i++) {
            HeaderContentPair reply = replies.get(i);
            socket.send(reply.getHeaderAsBytes(), ZMQ.SNDMORE);
            if (i == replies.size() - 1) {
                socket.send(reply.getContent(), 0);
            } else {
                socket.send(reply.getContent(), ZMQ.SNDMORE);
            }
        }
    }

    public void close() {
        thread.interrupt();
        pingThread.interrupt();
        socket.close();
        pingSocket.close();
    }
}
