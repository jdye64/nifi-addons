package com.jeremydyer.basin.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/nifi")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/cluster", position = 4)
public interface NiFiEndpoint {

    @GET
    @Path("{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(produces = ContentType.JSON)
    Response get(@PathParam(value = "id") Long id);

}
