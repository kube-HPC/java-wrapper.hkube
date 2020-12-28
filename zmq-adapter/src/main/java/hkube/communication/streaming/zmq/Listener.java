package hkube.communication.streaming.zmq;

import java.util.List;
import java.util.Map;
import java.util.Random;

import hkube.communication.streaming.Flow;
import hkube.communication.streaming.Message;
import hkube.encoding.EncodingManager;
import org.zeromq.*;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import hkube.communication.streaming.IMessageHandler;

public class Listener {
    String remoteHost;
    String remotePort;
    IMessageHandler messageHandler;
    String name;
    EncodingManager encodingManager;

    public Listener(String remoteHost, String remotePort, IMessageHandler messageHandler, String encoding, String name) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.messageHandler = messageHandler;
        this.encodingManager = new EncodingManager(encoding);
        this.name = name;

    }

    private final static int HEARTBEAT_LIVENESS = 3;     //  3-5 is reasonable
    private final static int HEARTBEAT_INTERVAL = 1000;  //  msecs
    private final static int INTERVAL_INIT = 1000;  //  Initial reconnect
    private final static int INTERVAL_MAX = 32000; //  After exponential backoff

    //  Paranoid Pirate Protocol constants
    private final static String PPP_READY = "\001"; //  Signals worker is ready
    private final static String PPP_HEARTBEAT = "\002"; //  Signals worker heartbeat

    //  Helper function that returns a new configured socket
    //  connected to the Paranoid Pirate queue

    private static Socket worker_socket(ZContext ctx) {
        Socket worker = ctx.createSocket(ZMQ.DEALER);
        worker.connect("tcp://localhost:5556");

        //  Tell queue we're ready for work
        System.out.println("I: worker ready\n");
        ZFrame frame = new ZFrame(PPP_READY);
        frame.send(worker, 0);

        return worker;
    }

    //  We have a single task, which implements the worker side of the
    //  Paranoid Pirate Protocol (PPP). The interesting parts here are
    //  the heartbeating, which lets the worker detect if the queue has
    //  died, and vice-versa:

    public void start() {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (ZContext ctx = new ZContext()) {
                        Socket worker = worker_socket(ctx);

                        Poller poller = (ctx.getContext()).poller();
                        poller.register(worker, Poller.POLLIN);

                        //  If liveness hits zero, queue is considered disconnected
                        int liveness = HEARTBEAT_LIVENESS;
                        int interval = INTERVAL_INIT;

                        //  Send out heartbeats at regular intervals
                        long heartbeat_at = System.currentTimeMillis() + HEARTBEAT_INTERVAL;

                        Random rand = new Random(System.nanoTime());
                        int cycles = 0;
                    }
                }
            },name + " Listener");
            thread.start();


    }

}
