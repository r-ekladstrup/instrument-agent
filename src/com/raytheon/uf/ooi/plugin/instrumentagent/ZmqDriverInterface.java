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
    private final int commandTimeout = 10000;
    private final int eventTimeout = 1000;

	public ZmqDriverInterface(String host, int commandPort, int eventPort) {
		commandUrl = String.format("tcp://%s:%d", host, commandPort);
        eventUrl = String.format("tcp://%s:%d", host, eventPort);
	}
	
	public void connect() {
        context = new ZContext();
        context.setLinger(0);
        
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
        commandSocket.setLinger(0);
	}
	
	private void connectEvent() {
		status.handle(Priority.INFO, "Connecting to event port: {}", eventUrl);
        eventSocket = context.createSocket(ZMQ.SUB);
        eventSocket.connect(eventUrl);
        eventSocket.subscribe(new byte[0]);
        eventSocket.setLinger(0);
	}

	@Override
    protected synchronized String _sendCommand(String command) {
		status.handle(Priority.INFO, "Sending command: " + command);
		// Send the command
        commandSocket.send(command);
		
        // Get the response
        PollItem items[] = {new PollItem(commandSocket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, commandTimeout);
        
        if (rc == -1)
        	// INTERRUPTED
        	return null;
        String reply = null;
        if (items[0].isReadable()) {
        	reply = commandSocket.recvStr();
        	status.handle(Priority.INFO, "ZMQ received: " + reply);
        }
        if (reply == null) {
            status.handle(Priority.INFO, "Empty message received from command: {}", command);
        	connectCommand();
        }
  
        return reply;
    }

    protected void eventLoop() {
        while (keepRunning) {
        	try {
        		PollItem items[] = {new PollItem(eventSocket, Poller.POLLIN)};
                ZMQ.poll(items, eventTimeout);
                
                String reply = null;
                if (items[0].isReadable()) {
                	reply = eventSocket.recvStr();
                	status.handle(Priority.INFO, "ZMQ received: " + reply);
                }
	            if (reply != null) {
	                try {
	                	setChanged();
	                	notifyObservers(reply);
	                }
	                catch (Exception e) {
	                    e.printStackTrace();
	                    status.handle(Priority.ERROR, ("Exception notifying observers: " + e.getMessage()));
	                }
	            }
        	} catch (Exception e) {
	        	status.handle(Priority.ERROR, "Exception in event loop: " + e);
	        }
        }
    }

    public void shutdown() {
        keepRunning = false;
        if (context != null) {
        	status.handle(Priority.INFO, "Closing ZMQ context");
        	for (ZMQ.Socket socket: context.getSockets()) {
        		socket.setLinger(0);
        		socket.close();
        	}
        }
    }

}