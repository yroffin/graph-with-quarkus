package org.devops.graph.controller.abs;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.devops.graph.model.NodeInterface;
import org.devops.graph.model.RelationshipInterface;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.exceptions.NoSuchRecordException;

public class CypherControllerAbstract<N extends NodeInterface, R extends RelationshipInterface> {

    @FunctionalInterface
    public interface ITFactoryNode<N> {
        N create();
    }

    @FunctionalInterface
    public interface ITFactoryRelation<R> {
        R create();
    }

    protected static final Logger LOG = Logger.getLogger(CypherControllerAbstract.class);

    public CompletionStage<Response> create(final Driver driver, final N instance, String query) {
        final AsyncSession session = driver.asyncSession();
        // Build the session
        return session.writeTransactionAsync(tx -> tx.runAsync(query).thenCompose(fn -> fn.singleAsync()))
                .thenApply(record -> instance.asNode(record.get("n").asNode())).handle((record, exception) -> {
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
    }

    public CompletionStage<Response> create(final Driver driver, final R instance, String query) {
        final AsyncSession session = driver.asyncSession();
        // Build the session
        return session.writeTransactionAsync(tx -> tx.runAsync(query).thenCompose(fn -> fn.singleAsync()))
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
    }

}
