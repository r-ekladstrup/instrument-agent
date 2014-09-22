package com.raytheon.uf.ooi.plugin.instrumentagent;

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
	
    protected abstract String sendCommand(String command, int timeout);
    
    protected abstract String sendCommand(String command);

    protected abstract void eventLoop();
    
    protected abstract void connect();

    protected abstract void shutdown();

    protected void handleException(List<?> exception) {
        // TODO - alert user
        status.handle(Priority.ERROR, "handleException: " + exception);
    }
}

