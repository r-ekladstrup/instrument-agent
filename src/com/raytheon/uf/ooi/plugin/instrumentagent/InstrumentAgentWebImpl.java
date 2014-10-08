package com.raytheon.uf.ooi.plugin.instrumentagent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

@Path("/instrument")
public class InstrumentAgentWebImpl implements IAgentWebInterface {
	
	private final IUFStatusHandler log = UFStatus.getHandler(this.getClass());
	private final Map<String, InstrumentAgent> agentMap = new HashMap<>();
	private final Executor executor;
	private String contentPathString;
	
	public InstrumentAgentWebImpl(String basePath) {
		
		IPathManager pathManager = PathManagerFactory.getPathManager();
		LocalizationContext context = pathManager.getContext(LocalizationType.EDEX_STATIC,
		        LocalizationLevel.BASE);
		
		File contentPath = pathManager.getFile(context, basePath);
		if (!contentPath.exists()) {
		    throw new IllegalArgumentException("Unable to find web agent static resources at "
		            + contentPath);
		}
		contentPathString = contentPath.getAbsolutePath();
		
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

	/*
	 * (non-Javadoc)
	 * @see com.raytheon.uf.ooi.plugin.instrumentagent.IAgentWebInterface#getAgent(javax.ws.rs.container.AsyncResponse, java.lang.String, double)
	 * 
	 * If blocking == false, fetch and return the current state
	 * otherwise, do not return until the overall state has changed.
	 * 
	 * Supports AJAX state updates
	 */
	@Override
	public void getAgent(final AsyncResponse asyncResponse, String id, boolean blocking) {
		log.handle(Priority.INFO, "getAgent: " + id);
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			if (!blocking) {
				asyncResponse.resume(Response.ok(thisAgent.getOverallState(),
						MediaType.APPLICATION_JSON).build());
			} else {
				
				executor.execute(new Runnable() {
					@Override
					public void run() {
						asyncResponse.resume(Response.ok(thisAgent.getOverallStateChanged(),
									MediaType.APPLICATION_JSON).build());
					}
				});
			}
		} else {
			asyncResponse.resume(agentNotFound());
		}
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
		
		try {
			InstrumentAgent agent = new InstrumentAgent(id, module, klass, host, commandPort, eventPort, producer);
			agentMap.put(id, agent);
			return Response.ok(agent.getOverallState(), MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			log.handle(Priority.ERROR, "Exception creating agent: " + e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@Override
	public Response createAgentJson(String id, String agentDef) {
		log.handle(Priority.INFO, "createAgent: " + agentDef);
		if (agentMap.containsKey(id))
			return Response.notModified().build();
		
		try {
			InstrumentAgent agent = new InstrumentAgent(id, agentDef, producer);
			agentMap.put(id, agent);
			return Response.ok(agent.getOverallState(), MediaType.APPLICATION_JSON_TYPE).build();
		} catch (Exception e) {
			log.handle(Priority.ERROR, "Exception creating agent: " + e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
	public void initParams(final AsyncResponse asyncResponse, final String id,
			final String config, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.initParams(config, timeout);
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
	public void disconnect(final AsyncResponse asyncResponse, final String id, final int timeout) {
		final InstrumentAgent thisAgent = agentMap.get(id);
		if (thisAgent != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					String reply = thisAgent.disconnect(timeout);
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
	
	@Override
	public Response getApp() {
		try {
			return Response.seeOther(new URI("instrument/app/index.html")).build();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.noContent().build();
		}
	}
	
	@Override
	public Response getStatic(String path) {
		try {
			String resource = new String(Files.readAllBytes(Paths.get(contentPathString, path)));
			if (path.endsWith(".js"))
				return Response.ok(resource, MediaType.APPLICATION_JSON_TYPE).build();
			if (path.endsWith(".html"))
				return Response.ok(resource, MediaType.TEXT_HTML_TYPE).build();
			return Response.ok(resource).build();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.noContent().build();
		}
	}

	private Response agentNotFound() {
		// TODO
		return Response.status(404).build();
	}
}
