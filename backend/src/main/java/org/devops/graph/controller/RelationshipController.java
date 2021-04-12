package org.devops.graph.controller;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.devops.graph.controller.abs.RelationshipControllerAbstract;
import org.devops.graph.model.RelationshipStd;
import org.neo4j.driver.Driver;

@Path("/v1/graph/relationships")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RelationshipController extends RelationshipControllerAbstract<RelationshipStd> {
    @Inject
    Driver driver;

    @POST
    public CompletionStage<Response> create(RelationshipStd relationship) {
        return this.create(driver, relationship);
    }

}
