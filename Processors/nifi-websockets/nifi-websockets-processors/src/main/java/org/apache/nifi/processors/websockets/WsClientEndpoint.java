package org.apache.nifi.processors.websockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

/**
 * Created by acesir on 4/19/16.
 */
@ClientEndpoint
public class WsClientEndpoint {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private BlockingQueue messageQueue;
    private static CountDownLatch latch;
    public WsClientEndpoint(BlockingQueue queue){
        messageQueue = queue;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected ... " + session.getId());
        try {
            session.getBasicRemote().sendText("start");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        logger.info("Received ...." + message);
        try {
            messageQueue.put(message);
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
        return message;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
    }

    public void Start() {
        latch = new CountDownLatch(1);
        try {
            latch.await();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
