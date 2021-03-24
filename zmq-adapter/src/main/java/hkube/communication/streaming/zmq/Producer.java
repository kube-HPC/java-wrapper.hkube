package hkube.communication.streaming.zmq;

/**
 * Paranoid Pirate queue
 *
 * @author Arkadiusz Orzechowski <aorzecho@gmail.com>
 */

import java.util.*;

import hkube.algo.ICommandSender;
import hkube.communication.streaming.*;
import hkube.encoding.EncodingManager;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import static hkube.communication.streaming.zmq.Signals.*;

public class Producer implements IProducer {

    private static final int HEARTBEAT_INTERVAL = 10000; // msecs
    private static final int EXPIRY_INTERVAL = 15000;
    private static final int CYCLE_LENGTH_MS = 1000;
    private static final int PURGE_INTERVAL = 5000;
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

    private static class WorkersPool {
        private Map<String, List<Worker>> queues;
        private static final ZFrame heartbeatFrame = new ZFrame(PPP_HEARTBEAT.toBytes());
        private long heartbeatAt = System.currentTimeMillis()
                + HEARTBEAT_INTERVAL;

        public WorkersPool(List<String> consumers) {
            queues = new HashMap<>();
            consumers.stream().forEach(consumer -> {
                queues.put(consumer, new ArrayList<Worker>());
            });

        }

        /**
         * Worker is ready, remove if on list and move to end
         */
        public synchronized void workerReady(Worker worker, String consumer) {
            List<Worker> workers = queues.get(consumer);
            if (!workers.contains(worker)) {
                workers.add(worker);
            }
        }

        /**
         * Return next available worker addressList
         */
        public synchronized ZFrame next(String consumerName) {
            List<Worker> workers = queues.get(consumerName);
            if (workers != null && workers.size() > 0) {
                int index = new Random().nextInt(workers.size());
                return workers.remove(index).address;
            }
            return null;
        }

        public synchronized boolean hasNext(String consumerName) {
            List<Worker> workers = queues.get(consumerName);
            return (workers != null && workers.size() > 0);
        }

        public synchronized void remove(String consumerName, ZFrame address) {
            List<Worker> workers = queues.get(consumerName);
            if (workers != null && workers.size() > 0) {
                Optional<Worker> toBeRemoved = workers.stream().filter(worker -> Arrays.equals(worker.address.getData(), address.getData())).findFirst();
                if (toBeRemoved.isPresent()) {
                    workers.remove(toBeRemoved.get());
                }
            }
        }

        /**
         * Send heartbeats to idle workers if it's time
         */
        public synchronized void sendHeartbeats(Socket backend) {
            // Send heartbeats to idle workers if it's time
            if (System.currentTimeMillis() >= heartbeatAt) {
                queues.values().stream().forEach(workers -> {

                    for (Worker worker : workers) {
                        worker.address.sendAndKeep(backend, ZMQ.SNDMORE);
                        heartbeatFrame.sendAndKeep(backend);
                    }
                    heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
                });
            }
        }

        /**
         * Look for & kill expired workers. Workers are oldest to most recent,
         * so we stop at the first alive worker.
         */
        public synchronized void purge() {
            queues.values().stream().forEach(workers -> {
                while (workers.size() > 0 && workers.get(0).expiry < System.currentTimeMillis()) {
                    workers.remove(0).address.destroy();
                }
            });
        }


        public synchronized void close() {
            queues.values().stream().forEach(workers -> {
                for (Worker worker : workers)
                    worker.address.destroy();
            });
        }
    }

    public void produce(Message msg) {
        msg.setProduceTime(System.currentTimeMillis());
        queue.push(msg);
    }

    public void start() {
        // Prepare our context and sockets

        Thread thread = new Thread(new Runnable() {
            private long nextPurgeTime = new Date().getTime() + PURGE_INTERVAL;

            @Override
            public void run() {
                try {
                    ZContext context = new ZContext();
                    backend = context.createSocket(ZMQ.ROUTER);
                    backend.bind("tcp://*:" + port); // For workers
                    WorkersPool workers = new WorkersPool(consumers);
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

                                // Any sign of life from worker means it's ready
                                ZFrame addressFrame = msg.unwrap();
                                String address = Base64.getEncoder().encodeToString(addressFrame.getData());

                                // Validate control message, or return reply to client

                                Signals signal = Signals.getByBytes(msg.pop().getData());
                                String consumerName = (String) encodingManager.decodeNoHeader(msg.pop().getData());


                                if (((!sent.keySet().contains(address)) && (signal == PPP_INIT ||
                                        signal == PPP_READY || signal == PPP_HEARTBEAT)) || signal == PPP_DONE) {
                                    workers.workerReady(new Worker(addressFrame), consumerName);
                                }
                                if (signal == PPP_DONE_DISCONNECT || signal == PPP_NOT_READY) {
                                    workers.remove(consumerName, addressFrame);
                                }
                                if (signal == PPP_DONE || signal == PPP_DONE_DISCONNECT) {
                                    byte[] data = msg.pop().getData();
                                    long duration = new Date().getTime() - sent.get(address).getTime();
                                    sent.remove(address);
                                    responseAccumulators.stream().forEach(responseAccumlator -> {
                                        responseAccumlator.onResponse(data, consumerName, Long.valueOf(duration));
                                    });
                                }

                                msg.destroy();
                                workers.sendHeartbeats(backend);
                                workers.purge();
                            } catch (Exception exe) {
                                if (!active) {
                                    break;
                                } else throw exe;
                            }
                        }
                        consumers.stream().forEach(consumerName -> {
                            if (workers.hasNext(consumerName)) {
                                Message message = queue.pop(consumerName);
                                if (message != null) {
                                    try {
                                        ZFrame frame = workers.next(consumerName);
                                        String address = Base64.getEncoder().encodeToString(frame.getData());
                                        frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                                        frame = new ZFrame(PPP_MSG.toBytes());
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
                                }
                            }
                        });
                        if (new Date().getTime() > nextPurgeTime) {
                            nextPurgeTime = new Date().getTime() + PURGE_INTERVAL;
                            workers.purge();
                        }
                    }
                    workers.close();
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
    public ArrayDeque resetQueueTimeDurations(String consumer){
        return  queue.resetQueueTimeDurations(consumer);
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