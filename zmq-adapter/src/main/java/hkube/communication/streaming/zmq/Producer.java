package hkube.communication.streaming.zmq;

/**
 * Paranoid Pirate queue
 *
 * @author Arkadiusz Orzechowski <aorzecho@gmail.com>
 */

import java.util.*;

import hkube.communication.streaming.Flow;
import hkube.communication.streaming.IResponseAccumulator;
import hkube.communication.streaming.Message;
import hkube.communication.streaming.MessageQueue;
import hkube.encoding.EncodingManager;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public class Producer {

    private static final int HEARTBEAT_LIVENESS = 3; // 3-5 is reasonable
    private static final int HEARTBEAT_INTERVAL = 1000; // msecs

    private static final byte[] PPP_READY = {1}; // Signals worker is ready
    private static final byte[] PPP_HEARTBEAT = {2}; // Signals worker
    private String port = null;
    private IResponseAccumulator responseAccumulator;
    private MessageQueue queue;
    private EncodingManager encodingManager;
    private List<String> consumers;
    String name;

    public Producer(String name, String port, IResponseAccumulator responseAccumulator, List<String> consumers, String encoding) {
        this.port = port;
        this.responseAccumulator = responseAccumulator;
        this.name = name;
        queue = new MessageQueue(name, consumers);
        encodingManager = new EncodingManager(encoding);
        this.consumers = consumers;

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
            this.expiry = System.currentTimeMillis() + HEARTBEAT_INTERVAL
                    * HEARTBEAT_LIVENESS;
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

    private static class WorkersPool {
        private Map<String, Deque<Worker>> queues;
        private static final ZFrame heartbeatFrame = new ZFrame(PPP_HEARTBEAT);
        private long heartbeatAt = System.currentTimeMillis()
                + HEARTBEAT_INTERVAL;

        public WorkersPool(List<String> consumers) {
            queues = new HashMap<>();
            consumers.stream().forEach(consumer -> {
                queues.put(consumer, new ArrayDeque<Worker>());
            });

        }

        /**
         * Worker is ready, remove if on list and move to end
         */
        public synchronized void workerReady(Worker worker, String consumer) {
            Deque<Worker> workers = queues.get(consumer);
            if (workers.remove(worker)) {
                System.out.printf("I:    %s is alive, waiting\n",
                        worker.address.toString());
            } else {
                System.out.printf("I: %s is now ready to work\n",
                        worker.address.toString());
            }
            workers.offerLast(worker);
        }

        /**
         * Return next available worker address
         */
        public synchronized ZFrame next(String consumerName) {
            Deque<Worker> workers = queues.get(consumerName);
            return workers.pollFirst().address;
        }

        /**
         * Send heartbeats to idle workers if it's time
         */
        public synchronized void sendHeartbeats(Socket backend) {
            // Send heartbeats to idle workers if it's time
            queues.values().stream().forEach(workers -> {
                if (System.currentTimeMillis() >= heartbeatAt) {
                    for (Worker worker : workers) {
                        worker.address.sendAndKeep(backend, ZMQ.SNDMORE);
                        heartbeatFrame.sendAndKeep(backend);
                    }
                    heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
                }
            });
        }

        /**
         * Look for & kill expired workers. Workers are oldest to most recent,
         * so we stop at the first alive worker.
         */
        public synchronized void purge() {
            queues.values().stream().forEach(workers -> {
                for (Worker w = workers.peekFirst(); w != null
                        && w.expiry < System.currentTimeMillis(); w = workers
                        .peekFirst()) {
                    workers.pollFirst().address.destroy();
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
    public void produce(Message msg){
        queue.push(msg);
    }

    public void start() {
        // Prepare our context and sockets
        ZContext context = new ZContext();
        ZMQ.Socket backend = context.createSocket(ZMQ.ROUTER);
        backend.bind("tcp://*:" + port); // For workers
        WorkersPool workers = new WorkersPool(this.consumers);
        Thread thread = new Thread( new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    ZMQ.Poller items = new ZMQ.Poller(consumers.size());
                    items.register(backend, ZMQ.Poller.POLLIN);
                    items.poll();
                    if (items.pollin(0)) {
                        // receive whole message (all ZFrames) at once
                        ZMsg msg = ZMsg.recvMsg(backend);
                        if (msg == null)
                            break; // Interrupted

                        // Any sign of life from worker means it's ready
                        ZFrame address = msg.unwrap();


                        // Validate control message, or return reply to client

                        ZFrame frame = msg.unwrap();
                        ZFrame consumerNameFrame = msg.unwrap();
                        String consumerName = (String) encodingManager.decodeNoHeader(consumerNameFrame.getData());
                        workers.workerReady(new Worker(address), consumerName);
                        byte[] data = frame.getData();
                        if (!(Arrays.equals(data, PPP_HEARTBEAT) || Arrays
                                .equals(frame.getData(), PPP_READY))) {
                            responseAccumulator.onResponse(data, consumerName);
                        }
                        msg.destroy();
                        workers.sendHeartbeats(backend);
                        workers.purge();
                    }
                    consumers.stream().forEach(consumerName -> {
                        ZFrame frame = workers.next(consumerName);
                        frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                        Message message = queue.pop(consumerName);
                        Flow flow = message.getFlow().getRestOfFlow(consumerName);
                        byte[] bytes = encodingManager.encodeNoHeader(flow);
                        frame = new ZFrame(bytes);
                        frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                        bytes = message.getHeader();
                        frame = new ZFrame(bytes);
                        frame.sendAndDestroy(backend, ZMQ.SNDMORE);
                        bytes = message.getData();
                        frame = new ZFrame(bytes);
                        frame.sendAndDestroy(backend);
                    });
                }
            }
        },name + " producer");
        thread.start();
        workers.close();
        context.destroy();
    }
}