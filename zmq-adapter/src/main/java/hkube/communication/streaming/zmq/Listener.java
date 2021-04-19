package hkube.communication.streaming.zmq;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import com.fasterxml.jackson.databind.util.StdDateFormat;
import hkube.algo.ICommandSender;
import hkube.communication.streaming.Flow;
import hkube.communication.streaming.IListener;
import hkube.communication.streaming.Message;
import hkube.encoding.EncodingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import hkube.communication.streaming.IMessageHandler;

import static hkube.communication.streaming.zmq.Signals.*;

public class Listener implements IListener {
    private static final int CYCLE_LENGTH_MS = 1;
    String remoteHost;
    String remotePort;
    IMessageHandler messageHandler;
    String name;
    EncodingManager encodingManager;
    ICommandSender errorHandler;
    ZMQ.Socket worker;
    IReadyUpdater readyUpdater;
    Listener me;
    long lastReceiveTime;
    static Lock lock = new ReentrantLock();

    private final static int HEARTBEAT_LIVENESS = 300;     //  3-5 is reasonable
    private final static int HEARTBEAT_INTERVAL = 10;  //  msecs
    private final static int INTERVAL_INIT = 1000;  //  Initial reconnect
    private final static int INTERVAL_MAX = 32000; //  After exponential backoff
    private final static int HEARTBEAT_LIVENESS_TIMEOUT = 30000;
    private final static int POLL_TIMEOUT_MS = 1000;
    private final static int STOP_TIMEOUT_MS = 5000;

    private long lastSentTime;
    //  Paranoid Pirate Protocol constants
    //  Signals worker heartbeat
    private boolean active = false;
    private boolean stillWorking = true;
    private boolean forceClose = false;
    ZMQ.Poller poller;
    private static final Logger logger = LogManager.getLogger();

    public Listener(String remoteHost, String remotePort, String encoding, String name, ICommandSender errorHandler) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.errorHandler = errorHandler;
        this.encodingManager = new EncodingManager(encoding);
        this.name = name;
        id = remoteHost + remotePort;
        me = this;
    }

    public void setMessageHandler(IMessageHandler handler) {
        messageHandler = handler;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void ready(boolean isReady) {
        this.isReady = isReady;
    }

    boolean isReady = true;
    boolean isReadySentValue = true;
    String id;


    private void worker_socket(ZMQ.Context ctx) {
        Random rand = new Random(System.nanoTime());
        Socket worker = ctx.socket(ZMQ.DEALER);
        String identity = String.format(
                "%04X-%04X", rand.nextInt(0x10000), rand.nextInt(0x10000)
        );
        worker.setIdentity(identity.getBytes());
        worker.connect("tcp://" + remoteHost + ":" + remotePort);

        //  Tell queue we're ready for work
        System.out.println(identity + " connected\n");
        this.worker = worker;
//        send(PPP_READY, null);
        poller = new ZMQ.Poller(1);
        poller.register(worker, Poller.POLLIN);
        lastReceiveTime = new Date().getTime();
    }

    public void fetch() {

        if(active) {
            send(PPP_READY, null);
            int rc = poller.poll(POLL_TIMEOUT_MS);
            if (rc == -1) {
                System.out.print("Poll failed break loop");
            } else if (poller.pollin(0)) {
                //  Get message
                //  - 3-part envelope + content -> request
                //  - 1-part HEARTBEAT -> heartbeat
                ZMsg zmqMsg;

                zmqMsg = ZMsg.recvMsg(worker);
                if (zmqMsg == null) {
                    System.out.print("Got null frame");
                }
                //  To test the robustness of the queue implementation we
                //  simulate various typical problems, such as the worker
                //  crashing, or running very slowly. We do this after a few
                //  cycles so that the architecture can get up and running
                //  first:
                ZFrame signalFrame = zmqMsg.pop();
                if (Signals.getByBytes(signalFrame.getData()) == PPP_MSG) {
                    ZFrame frame = zmqMsg.pop();
                    byte[] flowBytes = frame.getData();
                    frame = zmqMsg.pop();
                    byte[] header = frame.getData();
                    frame = zmqMsg.pop();
                    byte[] data = frame.getData();
                    List flowList = (List) encodingManager.decodeNoHeader(flowBytes);
                    Flow flow = new Flow(flowList);
                    Message msg = new Message(data, header, flow);
                    byte[] response = messageHandler.onMessage(msg);
                    send(PPP_DONE, response);
                }
            } else {
                System.out.println("Nothing on poll");
            }
        }
        else if(stillWorking){
            worker.close();
            stillWorking = false;
        }
    }

    public void start() {
        try {
            active = true;
            ZMQ.Context ctx = ZMQ.context(1);
            worker_socket(ctx);
        } catch (Exception exc) {
            exc.printStackTrace();
            Map<String, String> res = new HashMap<>();
            res.put("code", "Failed");
            res.put("message", exc.toString());
            errorHandler.sendMessage("errorMessage", res, true);
        }
    }

    void send(Signals signal, byte[] response) {
        ZFrame frame = new ZFrame(signal.toString());
        frame.send(worker, ZMQ.SNDMORE);
        frame = new ZFrame(encodingManager.encodeNoHeader(name));
        frame.send(worker, ZMQ.SNDMORE);
        if (response == null) {
            response = PPP_EMPTY.toBytes();
        }
        frame = new ZFrame(response);
        frame.send(worker, 0);
        lastSentTime = new Date().getTime();
    }

    public void close(boolean forceClose) {
        active = false;
        while (stillWorking) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
