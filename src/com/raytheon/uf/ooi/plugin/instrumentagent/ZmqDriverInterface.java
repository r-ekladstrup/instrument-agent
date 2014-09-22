package com.raytheon.uf.ooi.plugin.instrumentagent;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Concrete implementation of the Instrument Driver interface for ZMQ
 */

public class ZmqDriverInterface extends AbstractDriverInterface {
	private ZContext context;
    private ZMQ.Socket commandSocket;
    private ZMQ.Socket eventSocket;
    private boolean keepRunning = true;
    private String host;
    private int commandPort;
    private int eventPort;

	public ZmqDriverInterface(String host, int commandPort, int eventPort) {
		this.host = host;
		this.commandPort = commandPort;
		this.eventPort = eventPort;
	}
	
	public void connect() {
        String commandUrl = String.format("tcp://%s:%d", host, commandPort);
        String eventUrl = String.format("tcp://%s:%d", host, eventPort);
        
        status.handle(Priority.INFO, "Initialize ZmqDriverInterface");
        context = new ZContext();
        
        status.handle(Priority.INFO, "Connecting to command port: {}", commandUrl);
        commandSocket = context.createSocket(ZMQ.REQ);
        commandSocket.connect(commandUrl);

        status.handle(Priority.INFO, "Connecting to event port: {}", eventUrl);
        eventSocket = context.createSocket(ZMQ.SUB);
        eventSocket.connect(eventUrl);
        eventSocket.subscribe(new byte[0]);
        
        status.handle(Priority.INFO, "Connected, starting event loop");
        Thread t = new Thread() {
        	public void run() {
        		eventLoop();
        	}
        };

        t.setName("Event Loop " + eventUrl);
        t.start();
    }

	@Override
    protected String sendCommand(String command, int timeout) {
        commandSocket.send(command);
        commandSocket.setReceiveTimeOut(timeout * 1000);
        String reply = commandSocket.recvStr();
        if (reply == null)
            status.handle(Priority.INFO, "Empty message received from command: {}", command);
        return reply;
    }
    
	@Override
	protected String sendCommand(String command) {
		return sendCommand(command, DEFAULT_TIMEOUT);
	}

    protected void eventLoop() {
        while (keepRunning) {
        	try {
	            String reply = eventSocket.recvStr();
	
	            if (reply != null) {
	                status.handle(Priority.INFO, "REPLY = " + reply);
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