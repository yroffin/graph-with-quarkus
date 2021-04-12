package org.devops.graph.controller;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.devops.graph.controller.abs.CypherControllerAbstract;
import org.devops.graph.model.NodeInterface;
import org.devops.graph.model.NodeStd;
import org.devops.graph.model.RelationshipInterface;
import org.devops.graph.model.RelationshipStd;
import org.neo4j.driver.Driver;

@Path("/v1/graph/cypher")
public class CypherController extends CypherControllerAbstract<NodeInterface, RelationshipInterface> {
    @Inject
    Driver driver;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/relationships")
    public CompletionStage<Response> createRelationship(String query) {
        return this.create(driver, new RelationshipStd(), query);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/nodes")
    public CompletionStage<Response> createNode(String query) {
        return this.create(driver, new NodeStd(), query);
    }
}
