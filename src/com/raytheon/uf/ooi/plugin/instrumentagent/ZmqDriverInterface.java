package com.raytheon.uf.ooi.plugin.instrumentagent;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Concrete implementation of the Instrument Driver interface for ZMQ
 */

public class ZmqDriverInterface extends AbstractDriverInterface {
	private ZContext context;
    private ZMQ.Socket commandSocket;
    private ZMQ.Socket eventSocket;
    private boolean keepRunning = true;
    private String eventUrl;
    private String commandUrl;

	public ZmqDriverInterface(String host, int commandPort, int eventPort) {
		commandUrl = String.format("tcp://%s:%d", host, commandPort);
        eventUrl = String.format("tcp://%s:%d", host, eventPort);
	}
	
	public void connect() {
        context = new ZContext();
        
        connectCommand();
        connectEvent();
        
        Thread t = new Thread() {
        	public void run() {
        		eventLoop();
        	}
        };

        t.setName("Event Loop " + eventUrl);
        t.start();
    }
	
	private void connectCommand() {
		status.handle(Priority.INFO, "Connecting to command port: {}", commandUrl);
        commandSocket = context.createSocket(ZMQ.REQ);
        commandSocket.connect(commandUrl);
	}
	
	private void connectEvent() {
		status.handle(Priority.INFO, "Connecting to event port: {}", eventUrl);
        eventSocket = context.createSocket(ZMQ.SUB);
        eventSocket.connect(eventUrl);
        eventSocket.subscribe(new byte[0]);
	}

	@Override
    protected String _sendCommand(String command, int timeout) {
		status.handle(Priority.INFO, "Sending command: " + command + " with timeout: " + timeout);
		// Send the command
        commandSocket.send(command);
		
        // Get the response
        PollItem items[] = {new PollItem(commandSocket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, timeout);
        
        if (rc == -1)
        	// INTERRUPTED
        	return null;
        String reply = null;
        if (items[0].isReadable()) {
        	reply = commandSocket.recvStr();
        }
        if (reply == null) {
            status.handle(Priority.INFO, "Empty message received from command: {}", command);
        	connectCommand();
        }
  
        return reply;
    }
    
	@Override
	protected String _sendCommand(String command) {
		return _sendCommand(command, DEFAULT_TIMEOUT);
	}

    protected void eventLoop() {
        while (keepRunning) {
        	try {
	            String reply = eventSocket.recvStr();
	
	            if (reply != null) {
	                try {
	                	setChanged();
	                	notifyObservers(reply);
	                }
	                catch (Exception e) {
	                    e.printStackTrace();
	                    status.handle(Priority.ERROR, ("Exception notifying observers: " + e.getMessage()));
	                }
	            } else {
	                status.handle(Priority.INFO, "Empty message received in event loop");
	            }
        	} catch (Exception e) {
	        	status.handle(Priority.ERROR, "Exception in event loop: " + e);
	        }
        }
    }

    public void shutdown() {
        keepRunning = false;
        if (eventSocket != null)
        	eventSocket.close();
        if (commandSocket != null)
        	commandSocket.close();
        if (context != null)
        	context.close();
    }

}