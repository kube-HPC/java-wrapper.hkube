package hkube.communication.streaming.zmq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import hkube.algo.ICommandSender;
import hkube.communication.streaming.Flow;
import hkube.communication.streaming.IListener;
import hkube.communication.streaming.Message;
import hkube.encoding.EncodingManager;
import org.zeromq.*;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import hkube.communication.streaming.IMessageHandler;

public class Listener implements IListener {
    String remoteHost;
    String remotePort;
    IMessageHandler messageHandler;
    String name;
    EncodingManager encodingManager;
    ICommandSender errorHandler;
    ZMQ.Socket worker;


    private final static int HEARTBEAT_LIVENESS = 3;     //  3-5 is reasonable
    private final static int HEARTBEAT_INTERVAL = 1000;  //  msecs
    private final static int INTERVAL_INIT = 1000;  //  Initial reconnect
    private final static int INTERVAL_MAX = 32000; //  After exponential backoff

    //  Paranoid Pirate Protocol constants
    private final static String PPP_READY = "\001"; //  Signals worker is ready
    private final static String PPP_HEARTBEAT = "\002";
    private final static String PPP_DISCONNECT = "\003"; //  Signals worker heartbeat
    private boolean active = false;
    ZMQ.Poller poller;

    public Listener(String remoteHost, String remotePort, String encoding, String name, ICommandSender errorHandler) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.errorHandler = errorHandler;
        this.encodingManager = new EncodingManager(encoding);
        this.name = name;


    }

    public void setMessageHandler(IMessageHandler handler) {
        messageHandler = handler;
    }

    private final static String WORKER_READY = "\001";

    private static Socket worker_socket(ZMQ.Context ctx) {
        Random rand = new Random(System.nanoTime());
        Socket worker = ctx.socket(ZMQ.DEALER);
        String identity = String.format(
                "%04X-%04X", rand.nextInt(0x10000), rand.nextInt(0x10000)
        );
        worker.setIdentity(identity.getBytes());
        worker.connect("tcp://localhost:5556");

        //  Tell queue we're ready for work
        System.out.println("I: worker ready\n");
        ZFrame frame = new ZFrame(PPP_READY);
        frame.send(worker, 0);

        return worker;
    }

    public void start() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZMQ.Context ctx = ZMQ.context(1);
                    worker = ctx.socket(ZMQ.DEALER);

                    //  Set random identity to make tracing easier
                    Random rand = new Random(System.nanoTime());
                    String identity = String.format(
                            "%04X-%04X", rand.nextInt(0x10000), rand.nextInt(0x10000)
                    );
                    worker.setIdentity(identity.getBytes());
                    worker.connect("tcp://" + remoteHost + ":" + remotePort);

                    //  Tell broker we're ready for work
                    System.out.printf("I: (%s) worker ready\n", identity);
                    ZFrame frame = new ZFrame(WORKER_READY);
                    frame.send(worker, ZMQ.SNDMORE);
                    frame = new ZFrame(encodingManager.encodeNoHeader(name));
                    frame.send(worker, 0);
                    poller = new ZMQ.Poller(1);
                    poller.register(worker, Poller.POLLIN);

                    //  If liveness hits zero, queue is considered disconnected
                    int liveness = HEARTBEAT_LIVENESS;
                    int interval = INTERVAL_INIT;

                    //  Send out heartbeats at regular intervals
                    long heartbeat_at = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
                    active = true;
                    while (active) {
                        System.out.print("About to poll");
                        int rc = poller.poll(HEARTBEAT_INTERVAL);
                        if (rc == -1)
                            break; //  Interrupted
                        if (poller.pollin(0)) {
                            //  Get message
                            //  - 3-part envelope + content -> request
                            //  - 1-part HEARTBEAT -> heartbeat
                            ZMsg zmqMsg;
                            synchronized (this.getClass()) {
                                zmqMsg = ZMsg.recvMsg(worker);
                                if (zmqMsg == null)
                                    break; //  Interrupted

                                //  To test the robustness of the queue implementation we
                                //  simulate various typical problems, such as the worker
                                //  crashing, or running very slowly. We do this after a few
                                //  cycles so that the architecture can get up and running
                                //  first:
                                if (zmqMsg.size() == 3) {
                                    frame = zmqMsg.pop();
                                    byte[] flowBytes = frame.getData();
                                    frame = zmqMsg.pop();
                                    byte[] header = frame.getData();
                                    frame = zmqMsg.pop();
                                    byte[] data = frame.getData();
                                    Map flowMap = (Map) encodingManager.decodeNoHeader(flowBytes);
                                    Flow flow = new Flow((List) flowMap.get("nodes"));
                                    Message msg = new Message(data, header, flow);
                                    byte[] response = messageHandler.onMessage(msg);
                                    frame = new ZFrame(response);
                                    frame.send(worker, ZMQ.SNDMORE);
                                    frame = new ZFrame(encodingManager.encodeNoHeader(name));
                                    frame.send(worker, 0);
                                    liveness = HEARTBEAT_LIVENESS;
                                }
                            }
                            if (zmqMsg.size() != 3 && zmqMsg.size() != 0)
                                //  When we get a heartbeat message from the queue, it
                                //  means the queue was (recently) alive, so reset our
                                //  liveness indicator:
                                if (zmqMsg.size() == 1) {
                                    frame = zmqMsg.getFirst();
                                    String frameData = new String(
                                            frame.getData()
                                    );
                                    if (PPP_HEARTBEAT.equals(frameData))
                                        liveness = HEARTBEAT_LIVENESS;
                                    else {
                                        System.out.println("E: invalid message\n");
                                        zmqMsg.dump(System.out);
                                    }
                                    zmqMsg.destroy();
                                } else {
                                    System.out.println("E: invalid message\n");
                                    zmqMsg.dump(System.out);
                                }
                            interval = INTERVAL_INIT;
                        } else
                            //  If the queue hasn't sent us heartbeats in a while,
                            //  destroy the socket and reconnect. This is the simplest
                            //  most brutal way of discarding any messages we might have
                            //  sent in the meantime.
                            if (--liveness == 0) {
                                System.out.println(
                                        "W: heartbeat failure, can't reach queue\n"
                                );
                                System.out.printf(
                                        "W: reconnecting in %sd msec\n", interval
                                );
                                try {
                                    Thread.sleep(interval);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (interval < INTERVAL_MAX)
                                    interval *= 2;
                                worker.close();
                                worker = worker_socket(ctx);
                                poller = new ZMQ.Poller(1);
                                poller.register(worker, Poller.POLLIN);
                                liveness = HEARTBEAT_LIVENESS;
                            }

                        //  Send heartbeat to queue if it's time
                        if (System.currentTimeMillis() > heartbeat_at) {
                            long now = System.currentTimeMillis();
                            heartbeat_at = now + HEARTBEAT_INTERVAL;
                            System.out.println("I: worker heartbeat\n");
                            frame = new ZFrame(PPP_HEARTBEAT);
                            frame.send(worker, ZMQ.SNDMORE);
                            frame = new ZFrame(encodingManager.encodeNoHeader(name));
                            frame.send(worker, 0);
                        }
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                    Map<String, String> res = new HashMap<>();
                    res.put("code", "Failed");
                    res.put("message", exc.toString());
                    errorHandler.sendMessage("errorMessage", res, true);
                }
            }
        }, name + " Listener");
        thread.start();
    }

    public void close() {
        active = false;
        try {
            Thread.sleep(HEARTBEAT_INTERVAL);
            while (poller.pollin(0)) {
                //  Get message
                //  - 3-part envelope + content -> request
                //  - 1-part HEARTBEAT -> heartbeat
                synchronized (this.getClass()) {
                    ZMsg zmqMsg = ZMsg.recvMsg(worker);
                    if (zmqMsg == null)
                        break; //  Interrupted

                    //  To test the robustness of the queue implementation we
                    //  simulate various typical problems, such as the worker
                    //  crashing, or running very slowly. We do this after a few
                    //  cycles so that the architecture can get up and running
                    //  first:
                    if (zmqMsg.size() == 3) {
                        ZFrame frame = zmqMsg.pop();
                        byte[] flowBytes = frame.getData();
                        frame = zmqMsg.pop();
                        byte[] header = frame.getData();
                        frame = zmqMsg.pop();
                        byte[] data = frame.getData();
                        Map flowMap = (Map) encodingManager.decodeNoHeader(flowBytes);
                        Flow flow = new Flow((List) flowMap.get("nodes"));
                        Message msg = new Message(data, header, flow);
                        messageHandler.onMessage(msg);
                        frame = new ZFrame(PPP_DISCONNECT);
                        frame.send(worker, ZMQ.SNDMORE);
                        frame = new ZFrame(encodingManager.encodeNoHeader(name));
                        frame.send(worker, 0);
                    }
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
