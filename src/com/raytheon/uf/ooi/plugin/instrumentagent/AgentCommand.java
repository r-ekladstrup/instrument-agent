package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

public class AgentCommand {
	protected String command;
	protected List<String> args;
	protected Map<String, Object> keywordArgs;
	protected IUFStatusHandler status = UFStatus.getHandler(Ingest.class);

	public AgentCommand(String command, List<String> args, Map<String, Object> keywordArgs) {
		this.command = command;
		this.args = args;
		this.keywordArgs = keywordArgs;
	}
	
	public AgentCommand(String command, String arg) {
		this.command = command;
		this.args = new ArrayList<>(1);
		this.args.add(arg);
		this.keywordArgs = null;
	}
	
	public AgentCommand(String command) {
		this.command = command;
		this.args = null;
		this.keywordArgs = null;
	}

	public static AgentCommand ping() {
		return new AgentCommand(Constants.PING, null, null);
	}
	
	public static AgentCommand configure(String portAgentConfig) {
		return new AgentCommand(Constants.CONFIGURE, portAgentConfig);
	}
	
	public static AgentCommand initParams(String initParams) {
		return new AgentCommand(Constants.INITIALIZE, initParams);
	}
	
	public static AgentCommand connect() {
		return new AgentCommand(Constants.CONNECT);
	}
	
	public static AgentCommand discoverState() {
		return new AgentCommand(Constants.DISCOVER_STATE);
	}
	
	public static AgentCommand stopDriver() {
		return new AgentCommand(Constants.STOP_DRIVER);
	}
	
	public static AgentCommand getMetadata() {
		return new AgentCommand(Constants.GET_CONFIG_METADATA);
	}
	
	public static AgentCommand getCapabilities() {
		return new AgentCommand(Constants.GET_CAPABILITIES);
	}
	
	public static AgentCommand executeResource(String command) {
		return new AgentCommand(Constants.EXECUTE_RESOURCE, command);
	}
	
	public static AgentCommand getResourceState() {
		return new AgentCommand(Constants.GET_RESOURCE_STATE);
	}
	
	public static AgentCommand getResource(String... resources) {
		if (resources.length == 0)
			return new AgentCommand(Constants.GET_RESOURCE, "DRIVER_PARAMETER_ALL");
		return new AgentCommand(Constants.GET_RESOURCE, Arrays.asList(resources), null);
	}
	
	public static AgentCommand setResource(String... resources) {
		return new AgentCommand(Constants.SET_RESOURCE, Arrays.asList(resources), null);
	}
	
	public String toString() {
		Map<String, Object> message = new HashMap<>();

        message.put("cmd", command);
        if (args != null)
        	message.put("args", args);
        if (keywordArgs != null)
        	message.put("kwargs", keywordArgs);
        
        status.handle(Priority.DEBUG, "BUILT COMMAND: " + message);
        try {
			return JsonHelper.toJson(message);
		} catch (IOException e) {
			status.handle(Priority.ERROR, "Unable to generate JSON command string: " + e);
			return "";
		}
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public Map<String, Object> getKeywordArgs() {
		return keywordArgs;
	}

	public void setKeywordArgs(Map<String, Object> keywordArgs) {
		this.keywordArgs = keywordArgs;
	}
}
