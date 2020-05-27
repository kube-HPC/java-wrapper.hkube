package hkube.communication.zmq;

import org.junit.*;
import hkube.communication.IRequestListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TestZMQ {
    ZMQServer server;

    @After
    public void closeResources() {
        if (server != null) {
            try {
                server.close();
            } finally {
                server = null;
            }
        }
        else System.out.println("server is null");
    }

    @Test
    public void testSendReceive() throws TimeoutException{
        server = new ZMQServer(new ZMConfiguration());
        class ServerListener implements IRequestListener {
            @Override
            public void onRequest(byte[] request) {
                server.reply(request);
            }
        }
        ServerListener listener = new ServerListener();
        server.addRequestsListener(listener);
        ZMQRequest request = new ZMQRequest("localhost", new ZMConfiguration().getListeningPort(), new ZMConfiguration());
        String rep = new String(request.send("maaa".getBytes()));
        assert rep.equals("maaa");
    }

    @Test
    public void test2Clients() throws InterruptedException ,TimeoutException{
        server = new ZMQServer(new ZMConfiguration());
        class ServerListener implements IRequestListener {
            @Override
            public void onRequest(byte[] request) {
                server.reply(request);
            }
        }
        ServerListener listener = new ServerListener();
        server.addRequestsListener(listener);
        final Map results = new HashMap<>();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZMQRequest request = new ZMQRequest("localhost", new ZMConfiguration().getListeningPort(), new ZMConfiguration());
                    String rep = new String(request.send("maaa".getBytes()));
                    results.put("thread1", rep);
                }
                catch (TimeoutException e){
                    results.put("thread1", "Timeout error occurred");
                }
            }
        });
        thread1.start();
        ZMQRequest request = new ZMQRequest("localhost", new ZMConfiguration().getListeningPort(), new ZMConfiguration());
        String rep = new String(request.send("muu".getBytes()));
        thread1.join();
        assert rep.equals("muu");
        assert results.get("thread1").equals("maaa");
    }

    @Test
    public void testTimeOut() throws TimeoutException{
        server = new ZMQServer(new ZMConfiguration());
        class ServerListener implements IRequestListener {
            @Override
            public void onRequest(byte[] request) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                server.reply(request);
            }
        }
        ServerListener listener = new ServerListener();
        server.addRequestsListener(listener);
        ZMConfiguration config = new ZMConfiguration() {
            public Integer getTimeout() {
                return 20;
            }
        };
        ZMQRequest request = new ZMQRequest("localhost", config.getListeningPort(), config);
        Assert.assertThrows(TimeoutException.class,() -> {
            String rep = new String(request.send("nothing".getBytes()));
        });
    }

    @Test
    public void noServer() {
        ZMQRequest request = new ZMQRequest("localhost", new ZMConfiguration().getListeningPort(), new ZMConfiguration());
        Assert.assertThrows(TimeoutException.class,() -> {
            String rep = new String(request.send("maaa".getBytes()));
        });
    }
}
