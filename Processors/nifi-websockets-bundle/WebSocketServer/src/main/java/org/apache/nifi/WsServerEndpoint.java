package org.apache.nifi;

/**
 * Created by acesir on 4/19/16.
 */
import java.io.IOException;
import java.rmi.server.ExportException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/test")
public class WsServerEndpoint {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static Set<Session> allSessions;

    static ScheduledExecutorService timer =
            Executors.newSingleThreadScheduledExecutor();
    DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @OnOpen
    public void showTime(Session session){
        allSessions = session.getOpenSessions();

        // start the scheduler on the very first connection
        // to call sendTimeToAll every second
        if (allSessions.size()==1){
            timer.scheduleAtFixedRate(() -> sendTimeToAll(session),0,1,TimeUnit.SECONDS);
        }
    }

    private void sendTimeToAll(Session session){
        allSessions = session.getOpenSessions();
        for (Session sess: allSessions){
            try{
                sess.getBasicRemote().sendText("SessionId: " + sess.getId() +  "  Local time: " +
                        LocalTime.now().format(timeFormatter) + "\r\n");
                System.out.println("Sending to session:" + sess.getId());
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
    }
}
