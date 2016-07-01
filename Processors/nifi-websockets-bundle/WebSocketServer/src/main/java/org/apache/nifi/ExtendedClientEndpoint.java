package org.apache.nifi;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

/**
 * Created by acesir on 4/21/16.
 */
public class ExtendedClientEndpoint extends Endpoint {
    @Override
    public void onOpen(Session session, EndpointConfig config) {

    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {

    }

    @Override
    public void onError (Session session, Throwable throwable) {

    }
}
