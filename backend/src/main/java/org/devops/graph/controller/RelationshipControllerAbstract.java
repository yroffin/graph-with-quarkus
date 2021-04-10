package org.devops.graph.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.devops.graph.model.RelationshipInterface;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.exceptions.NoSuchRecordException;

public class RelationshipControllerAbstract<T extends RelationshipInterface> {

    @FunctionalInterface
    interface ITFactory<T> {
        T create();
    }

    private static final Logger LOG = Logger.getLogger(RelationshipControllerAbstract.class);

    public CompletionStage<Response> create(final Driver driver, final T instance) {
        final AsyncSession session = driver.asyncSession();
        // an edge must contains from and to
        if (instance.from() != null && instance.from().getPath() != null && instance.from().getVersion() != null
                && instance.to() != null && instance.to().getPath() != null && instance.to().getVersion() != null) {
            // build parameters
            final Map<String, Object> values = new HashMap<String, Object>() {
                private static final long serialVersionUID = 1L;
                {
                    put("fromPath", instance.from().getPath());
                    put("fromVersion", instance.from().getVersion());
                    put("toPath", instance.to().getPath());
                    put("toVersion", instance.to().getVersion());
                }
            };
            // Store query to log it aned use it further
            final String query = "MATCH (a:" + instance.from().getType() + "), (b:" + instance.to().getType()
                    + ") WHERE a.path = $fromPath AND b.path = $toPath AND a.version = $fromVersion AND b.version = $toVersion CREATE (a)-[r:"
                    + instance.getType() + "]->(b) RETURN r";
            LOG.infof("" + query + " with " + values);
            // Build the session
            return session
                    .writeTransactionAsync(
                            tx -> tx.runAsync(query, Values.value(values)).thenCompose(fn -> fn.singleAsync()))
                    .thenApply(record -> instance.asRelationship(record.get("r").asRelationship()))
                    .handle((record, exception) -> {
                        if (exception != null) {
                            Throwable source = exception;
                            if (exception instanceof CompletionException) {
                                source = ((CompletionException) exception).getCause();
                            }
                            Status status = Status.INTERNAL_SERVER_ERROR;
                            if (source instanceof NoSuchRecordException) {
                                status = Status.NOT_FOUND;
                            }
                            return Response.status(status).entity(exception).build();
                        } else {
                            return Response.created(URI.create("/v1/graph/relationships/" + record.getInternalId()))
                                    .build();
                        }
                    }).thenCompose(response -> session.closeAsync().thenApply(signal -> response));
        } else {
            final CompletableFuture<Response> cf = CompletableFuture.supplyAsync(() -> {
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(new HashMap<String, String>() {
                    /**
                     *
                     */
                    private static final long serialVersionUID = 1L;

                    {
                        put("error", "XXX");
                        put("message", "A relationship must contain a from and a to field");
                    }
                }).build();
            });
            return cf;
        }
    }

}
