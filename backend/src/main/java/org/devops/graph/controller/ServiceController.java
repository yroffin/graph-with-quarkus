package org.devops.graph.controller;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.devops.graph.controller.abs.NodeControllerAbstract;
import org.devops.graph.model.Service;
import org.neo4j.driver.Driver;

@Path("/v1/graph/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceController extends NodeControllerAbstract<Service> {
    @Inject
    Driver driver;

    @GET
    public CompletionStage<Response> get() {
        return this.get(driver, () -> new Service());
    }

    @POST
    public CompletionStage<Response> create(Service service) {
        return this.create(driver, service);
    }

    @GET
    @Path("{id}")
    public CompletionStage<Response> getSingle(@PathParam("id") long id) {
        return this.getSingle(driver, () -> new Service(), id);
    }

    @DELETE
    @Path("{id}")
    public CompletionStage<Response> delete(@PathParam("id") long id) {
        return this.delete(driver, () -> new Service(), id);
    }
}
