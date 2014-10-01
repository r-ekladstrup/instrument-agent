package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManager;
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
	
	private Map<String, Object> metadata = new HashMap<>();
	private String state = "";
	private List<Object> capabilities = new ArrayList<>();
	private Map<String, Object> resources = new HashMap<>();
	
	public InstrumentAgent(String sensor, String driverModule,
			String driverKlass, String driverHost, int commandPort,
			int eventPort, ProducerTemplate producer) {
		this.sensor = sensor;
		this.driverModule = driverModule;
		this.driverKlass = driverKlass;
		this.driverHost = driverHost;
		this.commandPort = commandPort;
		this.eventPort = eventPort;
		eventListener = new DriverEventHandler(this, producer, sensor);
	}
	
	public void connectDriver() throws Exception {
		if (process == null)
			runDriver();
		driverInterface = new ZmqDriverInterface(driverHost, commandPort, eventPort);
		driverInterface.deleteObservers();
		driverInterface.addObserver(eventListener);
		driverInterface.connect();
		getMetadata(2000);
		getState(2000);
		getCapabilities(2000);
	}
	
	public void killDriver() {
		status.handle(Priority.INFO, "Killing driver process: " + sensor);
		if (process != null)
			process.destroy();
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
    
    protected String sendCommand(String command, String args, String kwargs, int timeout) {
    	if (! args.startsWith("[")) {
    		List<Object> argList = new ArrayList<>(1);
    		try {
    			argList.add(JsonHelper.toObject(args));
    		} catch (Exception ignore) {
    			argList.add(args);
    		}
    		try {
				args = JsonHelper.toJson(argList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	String reply = driverInterface.sendCommand(command, args, kwargs, timeout);
    	status.handle(Priority.INFO, "Received reply from InstrumentDriver: " + reply);
    	return reply;
    }
    
    protected String sendCommand(String command, int timeout) {
    	return sendCommand(command, "[]", "{}", timeout);
    }
    
    protected String sendCommand(String command, String args, int timeout) {
    	return sendCommand(command, args, "{}", timeout);
    }
    
    public String agentState() throws IOException {
    	Map<String, Object> map = new HashMap<>();
    	map.put("metadata", metadata);
    	map.put("capabilities", capabilities);
    	map.put("state", state);
    	map.put("resources", getResources());
    	return JsonHelper.toJson(map);
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
    
    @SuppressWarnings("unchecked")
	public String getMetadata(int timeout) {
    	String reply = sendCommand(Constants.GET_CONFIG_METADATA, timeout);
    	// update the stored agent state based on this reply
		try {
			Map<String, Object> response = JsonHelper.toMap(reply);
			Object driverReply = response.get("reply");
	    	if (driverReply instanceof Map)
		    	metadata.putAll((Map<String, Object>) driverReply);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return reply;
    }
    
    @SuppressWarnings("unchecked")
	public String getCapabilities(int timeout) {
    	String reply = sendCommand(Constants.GET_CAPABILITIES, timeout);
    	// update the stored agent state based on this reply
    	try {
	    	Map<String, Object> response = JsonHelper.toMap(reply);
	    	Object driverReply = response.get("reply");
	    	if (driverReply instanceof List)
	    		capabilities = (List<Object>) driverReply;
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return reply;
    }
    
    public String getState(int timeout) {
    	String reply = sendCommand(Constants.GET_RESOURCE_STATE, timeout);
    	// update the stored agent state based on this reply
    	try {
	    	Map<String, Object> response = JsonHelper.toMap(reply);
	    	Object driverReply = response.get("reply");
	    	if (driverReply instanceof String)
	    		state = (String) driverReply;
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return reply;
    }
    
    @SuppressWarnings("unchecked")
	public String getResource(String args, int timeout) {
    	String reply = sendCommand(Constants.GET_RESOURCE, args, timeout);
    	// update the stored agent state based on this reply
    	try {
	    	Map<String, Object> response = JsonHelper.toMap(reply);
	    	Object driverReply = response.get("reply");
	    	if (driverReply instanceof Map)
	    		resources.putAll((Map<String, Object>) driverReply);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return reply;
    }
    
    @SuppressWarnings("unchecked")
	public String setResource(String args, int timeout) {
    	String reply = sendCommand(Constants.SET_RESOURCE, args, timeout);
    	// update the stored agent state based on this reply
    	try {
	    	Map<String, Object> response = JsonHelper.toMap(reply);
	    	Object driverReply = response.get("reply");
	    	if (driverReply instanceof Map)
	    		resources.putAll((Map<String, Object>) driverReply);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return reply;
    }
    
    public String execute(String args, String kwargs, int timeout) {
    	String reply = sendCommand(Constants.EXECUTE_RESOURCE, args, kwargs, timeout);
    	getCapabilities(2000);
    	return reply;
    }

	public Map<String, Object> getResources() {
		return resources;
	}

	public void setResources(Map<String, Object> resources) {
		this.resources = resources;
	}

	public void setState(String state) {
		this.state = state;
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
