package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class InstrumentAgent {
	protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);
	
	private String sensor;
	private String miPath = "ooi/instruments/mi-instrument";
	private String driverModule;
	private String driverKlass;
	private String driverHost;
	private int commandPort;
	private int eventPort;
	private Process process;
	private AbstractDriverInterface driverInterface;
	private DriverEventHandler eventListener;
	protected Map<Integer, String> transactionMap = new ConcurrentHashMap<>();
	private final Object cachedStateMonitor = new Object();
	private String cachedState;
	
	public InstrumentAgent(String sensor, String driverModule,
			String driverKlass, String driverHost, int commandPort,
			int eventPort, ProducerTemplate producer) throws Exception {
		this.sensor = sensor;
		this.driverModule = driverModule;
		this.driverKlass = driverKlass;
		this.driverHost = driverHost;
		this.commandPort = commandPort;
		this.eventPort = eventPort;
		eventListener = new DriverEventHandler(this, producer, sensor);
		startup();
	}
	
	public InstrumentAgent(String id, String jsonDefinition, ProducerTemplate producer) throws Exception {
		Map<String, Object> map = JsonHelper.toMap(jsonDefinition);
		sensor = id;
		driverModule = (String) map.get("module");
		driverKlass = (String) map.get("klass");
		driverHost = (String) map.get("host");
		commandPort = (int) map.get("commandPort");
		eventPort = (int) map.get("eventPort");
		eventListener = new DriverEventHandler(this, producer, sensor);
		startup();
	}
	
	public void startup() throws Exception {
		// TODO, check for running driver
		runDriver();
		connectDriver();
		getOverallState();
	}
	
	public void connectDriver() throws Exception {
		if (process == null)
			runDriver();
		if (driverInterface != null)
			driverInterface.shutdown();
		driverInterface = new ZmqDriverInterface(driverHost, commandPort, eventPort);
		driverInterface.deleteObservers();
		driverInterface.addObserver(eventListener);
		driverInterface.connect();
	}
	
	public void killDriver() {
		status.handle(Priority.INFO, "Killing driver process: " + sensor);
		if (process != null)
			process.destroy();
		if (driverInterface != null)
			driverInterface.shutdown();
		
		try {
			// TODO - build a real response...
			cachedState = "shutdown";
			cachedStateMonitor.notifyAll();
		} catch (Exception ignore) {
			// ignore
		}
			
	}
	
    public void runDriver() throws Exception {
    	IPathManager pathManager = PathManagerFactory.getPathManager();
    	LocalizationContext context = pathManager.getContext(LocalizationType.EDEX_STATIC,
                LocalizationLevel.BASE);

        File baseDir = pathManager.getFile(context, miPath);
        if (!baseDir.exists()) {
            throw new IllegalArgumentException("Unable to find instrument drivers at "
                    + baseDir);
        }
        
        String[] args = {"python", "main.py", driverModule, driverKlass,
        		Integer.toString(commandPort), Integer.toString(eventPort) };
        status.handle(Priority.INFO, "Launching Instrument Driver with args: " + Arrays.asList(args).toString());
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(baseDir);
        pb.inheritIO();
        process = pb.start();
    }
    
    protected String handleResponse(String reply, int timeout) {
    	try {
			Map<String, Object> driverReply = JsonHelper.toMap(reply);
			String replyType = (String) driverReply.get("type");
			switch (replyType) {
				case Constants.DRIVER_ASYNC_FUTURE:
					int transactionId = (int) driverReply.get("value");
					return waitForReply(transactionId, timeout);
				case Constants.DRIVER_SYNC_EVENT:
					Object commandMapObject = driverReply.get("cmd");
					if (commandMapObject instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> commandMap = (Map<String, Object>) commandMapObject;
						String command = (String) commandMap.get("cmd");
						if (command.equals("overall_state"))
							synchronized(cachedStateMonitor) {
								cachedState = reply;
								cachedStateMonitor.notifyAll();
							}
					}
				case Constants.DRIVER_BUSY:
				case Constants.DRIVER_EXCEPTION:
				default:
					return reply;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "exception";
		}
    }
    
    private String waitForReply(int transactionId, int timeout) {
    	status.handle(Priority.INFO, "waitForReply, transactionId: " + transactionId + " timeout: " + timeout);
    	long expireTime = System.currentTimeMillis() + timeout;
    	while (System.currentTimeMillis() < expireTime) {
    		if (transactionMap.containsKey(transactionId)) {
    			return transactionMap.remove(transactionId);
    		}
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
    	}
    	// timed out
    	// TODO, make this a proper reply or raise an Exception
    	return "timed out";
    }
    
    protected String sendCommand(String command, String args, String kwargs, int timeout) {
    	String reply = driverInterface.sendCommand(command, args, kwargs);
    	status.handle(Priority.INFO, "Received reply from InstrumentDriver: " + reply);
    	return handleResponse(reply, timeout);
    }
    
    protected String getOverallState() {
    	return sendCommand("overall_state", "[]", "{}", 0);
    }
    
    protected String getOverallStateChanged() {
    	synchronized(cachedStateMonitor) {
    		try {
				cachedStateMonitor.wait();
				return cachedState;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
    	}
    }
    
    protected String sendCommand(String command, int timeout) {
    	return sendCommand(command, "[]", "{}", timeout);
    }
    
    protected String sendCommand(String command, String args, int timeout) {
    	return sendCommand(command, args, "{}", timeout);
    }
    
    public String ping(int timeout) {
    	return sendCommand(Constants.PING, "PONG", timeout);
    }
    
    public String initialize(String config, int timeout) {
    	return sendCommand(Constants.INITIALIZE, config, timeout);
    }
    
    public String configure(String config, int timeout) {
    	return sendCommand(Constants.CONFIGURE, config, timeout);
    }
    
    public String initParams(String config, int timeout) {
    	return sendCommand(Constants.SET_INIT_PARAMS, config, timeout);
    }
    
    public String connect(int timeout) {
    	return sendCommand(Constants.CONNECT, timeout);
    }
    
    public String disconnect(int timeout) {
    	return sendCommand(Constants.DISCONNECT, timeout);
    }
    
    public String discover(int timeout) {
    	return sendCommand(Constants.DISCOVER_STATE, timeout);
    }
    
	public String getMetadata(int timeout) {
    	return sendCommand(Constants.GET_CONFIG_METADATA, timeout);
    }
    
	public String getCapabilities(int timeout) {
    	return sendCommand(Constants.GET_CAPABILITIES, timeout);
    }
    
    public String getState(int timeout) {
    	return sendCommand(Constants.GET_RESOURCE_STATE, timeout);
    }
    
	public String getResource(String args, int timeout) {
    	return sendCommand(Constants.GET_RESOURCE, args, timeout);
    }
    
	public String setResource(String args, int timeout) {
    	return sendCommand(Constants.SET_RESOURCE, args, timeout);
    }
    
    public String execute(String args, String kwargs, int timeout) {
    	return sendCommand(Constants.EXECUTE_RESOURCE, args, kwargs, timeout);
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
