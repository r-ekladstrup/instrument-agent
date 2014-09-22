package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.File;
import java.util.Arrays;

import org.apache.camel.Exchange;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class InstrumentAgent {
	protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);
	
	private String sensor;
	private String miPath;
	private String driverModule;
	private String driverKlass;
	private String driverHost;
	private int commandPort;
	private int eventPort;
	private Process process;
	private AbstractDriverInterface driverInterface;
	private DriverEventHandler eventListener;
	
	public InstrumentAgent() {
	}

	public void agentCommand(Exchange exchange) throws Exception {
		String input = exchange.getIn().getBody(String.class);
		String reply;
		status.handle(Priority.INFO, "Received command: " + input);
		
		switch(input) {
			case ("launch"):
				killDriver();
				process = runDriver();
			case ("connect"):
				driverInterface = new ZmqDriverInterface(driverHost, commandPort, eventPort);
				driverInterface.deleteObservers();
				driverInterface.addObserver(eventListener);
				driverInterface.connect();
				reply = driverInterface.sendCommand(AgentCommand.ping().toString());
				status.handle(Priority.INFO, "Agent received reply from driver: " + reply);
				break;
			case ("kill"):
				killDriver();
				break;
			default:
				if (driverInterface != null) {
					reply = driverInterface.sendCommand(input);
					status.handle(Priority.INFO, "Agent received reply from driver: " + reply);
				}
				else
					status.handle(Priority.ERROR, "Attempted to send command when Agent not connected!");
		}
	}
	
	public void killDriver() {
		if (process != null)
			process.destroy();
	}
	
    public Process runDriver() throws Exception {
    	if (driverHost.equals("localhost")) {
	        String[] args = {"python", miPath + "/main.py", driverModule, driverKlass,
	        		Integer.toString(commandPort), Integer.toString(eventPort) };
	        status.handle(Priority.DEBUG, "Launching Instrument Driver with args: " + Arrays.asList(args).toString());
	        ProcessBuilder pb = new ProcessBuilder(args);
	        pb.directory(new File(miPath));
	        return pb.start();
    	}
    	else {
    		status.handle(Priority.ERROR, "Remote driver launching not yet implemented!");
    		throw new Exception("Remote driver launching not yet implemented");
    	}
    }

	public String getSensor() {
		return sensor;
	}

	public void setSensor(String sensor) {
		this.sensor = sensor;
	}

	public AbstractDriverInterface getDriverInterface() {
		return driverInterface;
	}

	public void setDriverInterface(AbstractDriverInterface driverInterface) {
		this.driverInterface = driverInterface;
	}

	public DriverEventHandler getEventListener() {
		return eventListener;
	}

	public void setEventListener(DriverEventHandler eventListener) {
		this.eventListener = eventListener;
	}

	public String getMiPath() {
		return miPath;
	}

	public void setMiPath(String miPath) {
		this.miPath = miPath;
	}

	public String getModule() {
		return driverModule;
	}

	public void setModule(String module) {
		this.driverModule = module;
	}

	public String getKlass() {
		return driverKlass;
	}

	public void setKlass(String klass) {
		this.driverKlass = klass;
	}
	
	public String getHost() {
		return driverHost;
	}

	public void setHost(String host) {
		this.driverHost = host;
	}

	public int getCommandPort() {
		return commandPort;
	}

	public void setCommandPort(int commandPort) {
		this.commandPort = commandPort;
	}

	public int getEventPort() {
		return eventPort;
	}

	public void setEventPort(int eventPort) {
		this.eventPort = eventPort;
	}
}
