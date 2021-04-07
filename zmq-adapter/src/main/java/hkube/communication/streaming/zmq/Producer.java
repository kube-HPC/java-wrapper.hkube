package hkube.communication.streaming.zmq;


import java.util.*;

import hkube.algo.ICommandSender;
import hkube.communication.streaming.*;
import hkube.encoding.EncodingManager;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import static hkube.communication.streaming.zmq.Signals.*;

public class Producer implements IProducer {

    private static final int EXPIRY_INTERVAL = 15000;
    private static final int CYCLE_LENGTH_MS = 1;
    private String port = null;
    private List<IResponseAccumulator> responseAccumulators = new ArrayList<>();
    private MessageQueue queue;
    private EncodingManager encodingManager;
    private List<String> consumers;
    String name;
    boolean active = false;
    public double maxBufferSize;
    ICommandSender errorHandler;
    ZMQ.Socket backend;
    Map<String, Date> sent = new HashMap();


    public Producer(String name, String port, List<String> consumers, String encoding, double maxBufferSize, ICommandSender errorHandler) {
        this.port = port;
        this.name = name;
        this.errorHandler = errorHandler;
        queue = new MessageQueue(name, consumers, maxBufferSize);
        encodingManager = new EncodingManager(encoding);
        this.consumers = consumers;
        this.maxBufferSize = maxBufferSize;
    }
    // heartbeat

    /**
     * Keeps worker's address and expiry time.
     */
    private static class Worker {
        ZFrame address;
        long expiry;

        public Worker(ZFrame address) {
            this.address = address;
            this.expiry = System.currentTimeMillis() + EXPIRY_INTERVAL;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(address.getData());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Worker))
                return false;
            Worker other = (Worker) obj;
            return Arrays.equals(address.getData(), other.address.getData());
        }

    }

    public void registerResponseAccumulator(IResponseAccumulator accumulator) {
        responseAccumulators.add(accumulator);
    }


    public void produce(Message msg) {
        msg.setProduceTime(System.currentTimeMillis());
        queue.push(msg);
    }

    public void start() {
        // Prepare our context and sockets

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZContext context = new ZContext();
                    backend = context.createSocket(ZMQ.ROUTER);
                    backend.bind("tcp://*:" + port); // For workers

                    active = true;
                    while (active) {
                        ZMQ.Poller items = new ZMQ.Poller(consumers.size());
                        items.register(backend, ZMQ.Poller.POLLIN);
                        try {
                            items.poll(CYCLE_LENGTH_MS);
                        } catch (Exception exe) {
                            if (!active) {
                                break;
                            }
                            throw exe;
                        }
                        if (items.pollin(0)) {
                            // receive whole message (all ZFrames) at once
                            try {
                                ZMsg msg = ZMsg.recvMsg(backend);


                                if (msg == null)
                                    break; // Interrupted

                                ZFrame addressFrame = msg.unwrap();
                                String address = Base64.getEncoder().encodeToString(addressFrame.getData());

                                // Validate control message, or return reply to client

                                Signals signal = Signals.getByBytes(msg.pop().getData());
                                String consumerName = (String) encodingManager.decodeNoHeader(msg.pop().getData());
                                if (signal == PPP_READY) {
                                    addressFrame.sendAndDestroy(backend, ZMQ.SNDMORE);
                                    Message message = queue.pop(consumerName);
                                    if (message != null) {
                                        try {
                                            ZFrame frame = new ZFrame(PPP_MSG.toBytes());
                                            frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                                            Flow flow = message.getFlow().getRestOfFlow(name);
                                            byte[] bytes = encodingManager.encodeNoHeader(flow.asList());
                                            frame = new ZFrame(bytes);
                                            frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                                            bytes = message.getHeader();
                                            frame = new ZFrame(bytes);
                                            frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                                            bytes = message.getData();
                                            frame = new ZFrame(bytes);
                                            frame.sendAndDestroy(backend, 0);
                                            sent.put(address, new Date());
                                        } catch (Exception exe) {
                                            if (active) {
                                                throw exe;
                                            }
                                        }
                                    } else {
                                        ZFrame frame = new ZFrame(PPP_NO_MSG.toBytes());
                                        frame.sendAndDestroy(backend, 0);
                                    }
                                }
                                if (signal == PPP_DONE) {
                                    byte[] data = msg.pop().getData();
                                    Date sentDate = sent.get(address);
                                    if (sentDate != null) {
                                        long duration = new Date().getTime() - sent.get(address).getTime();
                                        sent.remove(address);
                                        responseAccumulators.stream().forEach(responseAccumlator -> {
                                            responseAccumlator.onResponse(data, consumerName, Long.valueOf(duration));
                                        });
                                    } else {
                                        System.out.println(address + " is missing");
                                    }

//                                    ZFrame frame = new ZFrame(PPP_NO_MSG.toBytes());
//                                    frame.sendAndDestroy(backend, 0);
                                }
                                msg.destroy();

                            } catch (Exception exe) {
                                if (!active) {
                                    break;
                                } else throw exe;
                            }
                        }


                    }

                } catch (Exception exc) {
                    exc.printStackTrace(System.out);
                    Map<String, String> res = new HashMap<>();
                    res.put("code", "Failed");
                    res.put("message", exc.toString());
                    errorHandler.sendMessage("errorMessage", res, true);

                }
            }
        }, name + " producer");
        thread.start();

    }

    public void close(boolean forceStop) {
        while (queue.anyLeft() && !forceStop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Stop listening on " + port);
        active = false;
        backend.close();
    }

    @Override
    public int getQueueSize(String consumer) {
        return queue.getInQueue(consumer);
    }

    @Override
    public ArrayDeque resetQueueTimeDurations(String consumer) {
        return queue.resetQueueTimeDurations(consumer);
    }

    @Override
    public int getQueueSize() {
        return queue.getInQueue();
    }

    @Override
    public int getSent(String consumer) {
        return queue.getSent(consumer);
    }

    @Override
    public int getDropped(String consumer) {
        return queue.getDropped(consumer);
    }

}