package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.IOException;
import java.util.*;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Abstract class representing a generic interface to an Instrument Driver
 */

public abstract class AbstractDriverInterface extends Observable {
    protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);
    protected int DEFAULT_TIMEOUT = 60;

    protected abstract String _sendCommand(String command);

    protected abstract void eventLoop();

    protected abstract void connect();

    protected abstract void shutdown();

    protected void handleException(List<?> exception) {
        // TODO - alert user
        status.handle(Priority.ERROR, "handleException: " + exception);
    }

    protected String sendCommand(String command, String args, String kwargs) {
        String json = "{\"cmd\": \"" + command + "\", \"args\": " + args + ", \"kwargs\": " + kwargs + "}";
        status.handle(Priority.DEBUG, "Preparing to send: " + json);
        try {
            // parse json to verify validity prior to sending...
            JsonHelper.toMap(json);
            return _sendCommand(json);
        } catch (Exception e) {
            return failedCommand(json, e);
        }
    }

    private String failedCommand(String command, Exception e) {
        Map<String, Object> map = new HashMap<>();
        map.put("cmd", command);
        map.put("reply", "FAIL: " + e);
        try {
            return JsonHelper.toJson(map);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return "Exception building failure message: " + e1;
        }
    }
}
