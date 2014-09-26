package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

@Path("/instrument")
public class InstrumentAgentWebImpl implements IAgentWebInterface {
	
	private final IUFStatusHandler log = UFStatus.getHandler(this.getClass());
	private final Map<String, InstrumentAgent> agentMap = new HashMap<>();
	private final Executor executor;
	
	public InstrumentAgentWebImpl() {
		executor = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder()
				    .setNameFormat("agentWebAsync-%d")
				    .setDaemon(true)
				    .build());
	}
	
	@EndpointInject(uri="jms-durable:queue:Ingest.instrument", context="agent-ingest-camel")
	protected ProducerTemplate producer;

	@Override
	public Response listAgents() {
		log.handle(Priority.INFO, "listAgents");
		String json;
		try {
			json = JsonHelper.toJson(agentMap.keySet().toArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			json = "error";
			e.printStackTrace();
		}
		return Response.ok(json).build();
	}
	
	@Override
	public Response listAgentsHtml() {
		log.handle(Priority.INFO, "listAgentsHtml");
		String html = "<!DOCTYPE html><html><body>\n";
		for (String agent: agentMap.keySet()) {
			html += "<p><a href=\"" + agent + "\">" + agent + "</a></p>";
		}
		html += "</body></html>";
		return Response.ok(html).build();
	}

	@Override
	public Response getAgent(String id) {
		log.handle(Priority.INFO, "getAgent: " + this);
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			try {
				return Response.ok(thisAgent.agentState(), MediaType.APPLICATION_JSON).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Response.ok(agentNotFound(), MediaType.APPLICATION_JSON).build();
	}
	
	@Override
	public Response getAgentAsHtml(String id) {
		log.handle(Priority.INFO, "getAgent: " + this);
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			try {
				return Response.ok(thisAgent.agentStateHtml()).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Response.ok(agentNotFound()).build();
	}

	@Override
	public Response deleteAgent(String id) {
		log.handle(Priority.INFO, "deleteAgent: " + id);
		if (agentMap.containsKey(id)) {
			InstrumentAgent agent = agentMap.remove(id);
			agent.killDriver();
			return Response.ok("Driver killed", MediaType.APPLICATION_JSON).build();
		}
		return null;
	}

	@Override
	public Response createAgent(String id, String module, String klass,
			String host, int commandPort, int eventPort) {
		log.handle(Priority.INFO, "createAgent: sensor: " + id);
		if (agentMap.containsKey(id))
			return Response.ok("nope", MediaType.APPLICATION_JSON).build();
		InstrumentAgent agent = new InstrumentAgent(id, module, klass, host, commandPort, eventPort, producer);
		agentMap.put(id, agent);
		try {
			agent.runDriver();
			agent.connectDriver();
			return Response.ok(agent.agentState(), MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.ok("failure", MediaType.APPLICATION_JSON).build();
		}
	}

	@Override
	public void ping(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.ping(timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
			
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void initialize(final AsyncResponse asyncResponse, final String id,
			final String config, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.initialize(config, timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void configure(final AsyncResponse asyncResponse, final String id,
			final String config, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.configure(config, timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void connect(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.connect(timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void discover(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.discover(timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void getMetadata(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.getMetadata(timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void getCapabilities(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.getCapabilities(timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void getState(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.getState(timeout);
					log.handle(Priority.INFO, "Received reply, calling resume...");
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void getResource(final AsyncResponse asyncResponse, final String id,
			String resource, final int timeout) {
		if (resource == null)
			resource = "DRIVER_PARAMETER_ALL";
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			final String myResource = resource;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.getResource(myResource, timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void setResource(final AsyncResponse asyncResponse, final String id,
			final String resource, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.setResource(resource, timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}

	@Override
	public void execute(final AsyncResponse asyncResponse, final String id,
			final String command, String kwargs, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			if (kwargs == null)
				kwargs = "{}";
			final String myKwargs = kwargs;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.execute(command, myKwargs, timeout);
					asyncResponse.resume(Response.ok(reply).type(MediaType.APPLICATION_JSON).build());
				}
			});
		} else {
			asyncResponse.resume(agentNotFound());
		}
	}
	
	private String agentNotFound() {
		// TODO
		return "\"agent not found\"";
	}
}
