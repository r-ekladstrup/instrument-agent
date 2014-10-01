package com.raytheon.uf.ooi.plugin.instrumentagent;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 22, 2014            pcable     Initial creation
 *
 * </pre>
 *
 * @author pcable
 * @version 1.0	
 */

public interface IAgentWebInterface {

    @GET
    @Path("api")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listAgents();

    @POST
    @Path("api/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createAgent(
    		@PathParam("id") String id,
    		// TODO - these should be retrieved from a datastore
    		@FormParam("module") String module,
    		@FormParam("class") String klass,
    		@FormParam("host") String host,
    		@FormParam("commandPort") int commandPort,
    		@FormParam("eventPort") int eventPort);
    
    @GET
    @Path("api/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getAgent(@PathParam("id") String id);
    
    
    @DELETE
    @Path("api/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteAgent(@PathParam("id") String id);
    
    @GET
    @Path("api/{id}/ping")
    @Produces({ MediaType.APPLICATION_JSON })
    public void ping(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("1000") @QueryParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/initialize")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void initialize(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("config") String config,
    		@DefaultValue("1000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/configure")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void configure(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("config") String config,
    		@DefaultValue("1000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/initparams")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void initParams(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("config") String config,
    		@DefaultValue("1000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/connect")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void connect(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("60000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/disconnect")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void disconnect(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("60000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/discover")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void discover(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("600000") @FormParam("timeout") int timeout);
    
    @GET
    @Path("api/{id}/metadata")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void getMetadata(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("1000") @QueryParam("timeout") int timeout);
    
    @GET
    @Path("api/{id}/capabilities")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void getCapabilities(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("1000") @QueryParam("timeout") int timeout);
    
    @GET
    @Path("api/{id}/state")
    @Produces({ MediaType.APPLICATION_JSON })
    public void getState(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@DefaultValue("1000") @QueryParam("timeout") int timeout);
    
    @GET
    @Path("api/{id}/resource")
    @Produces({ MediaType.APPLICATION_JSON })
    public void getResource(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("resource") String resource,
    		@DefaultValue("60000") @QueryParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/resource")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void setResource(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("resource") String resource,
    		@DefaultValue("60000") @FormParam("timeout") int timeout);
    
    @POST
    @Path("api/{id}/execute")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void execute(
    		@Suspended final AsyncResponse asyncResponse,
    		@PathParam("id") String id,
    		@FormParam("command") String resource,
    		@FormParam("kwargs") String kwargs,
    		@DefaultValue("60000") @FormParam("timeout") int timeout);
    
    @GET
	@Path("app")
	public Response getApp();
    
    @GET
	@Path("app/{path:.*}")
	public Response getStatic(@PathParam("path") String path);
    
}
