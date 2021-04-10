package org.devops.graph.controller;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.devops.graph.model.NodeInterface;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.exceptions.NoSuchRecordException;

public class ServiceControllerAbstract<T extends NodeInterface> {

    @FunctionalInterface
    interface ITFactory<T> {
        T create();
    }

    private static final Logger LOG = Logger.getLogger(ServiceControllerAbstract.class);

    public CompletionStage<Response> get(Driver driver, ITFactory<T> factory) {
        AsyncSession session = driver.asyncSession();
        T instance = factory.create();
        return session.runAsync("MATCH (f:" + instance.getClass().getSimpleName() + ") RETURN f ORDER BY f.name")
                .thenCompose(cursor -> cursor.listAsync(record -> {
                    return factory.create().asNode(record.get("f").asNode());
                })).thenCompose(nodes -> session.closeAsync().thenApply(signal -> nodes)).thenApply(Response::ok)
                .thenApply(ResponseBuilder::build);
    }

    public CompletionStage<Response> create(Driver driver, T instance) {
        AsyncSession session = driver.asyncSession();
        Map<String, Object> fields = instance.asMap();
        StringBuffer sb = new StringBuffer();
        for (Entry<String, Object> field : fields.entrySet()) {
            if (sb.length() == 0) {
                sb.append(field.getKey() + ": $" + field.getKey());
            } else {
                sb.append(", " + field.getKey() + ": $" + field.getKey());
            }
        }
        String query = "CREATE (f:" + instance.getClass().getSimpleName() + " {" + sb.toString() + "}) RETURN f";
        LOG.infof("" + query + " with " + fields);
        return session
                .writeTransactionAsync(
                        tx -> tx.runAsync(query, Values.value(fields)).thenCompose(fn -> fn.singleAsync()))
                .thenApply(record -> instance.asNode(record.get("f").asNode()))
                .thenCompose(persisted -> session.closeAsync().thenApply(signal -> persisted))
                .thenApply(persisted -> Response.created(URI.create("/services/" + persisted.getInternalId())).build());
    }

    public CompletionStage<Response> getSingle(Driver driver, ITFactory<T> factory, long id) {
        AsyncSession session = driver.asyncSession();
        T instance = factory.create();
        return session
                .readTransactionAsync(tx -> tx
                        .runAsync("MATCH (f:" + instance.getClass().getSimpleName() + ") WHERE id(f) = $id RETURN f",
                                Values.parameters("id", id))
                        .thenCompose(fn -> fn.singleAsync()))
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
                        return Response.status(status).build();
                    } else {
                        return Response.ok(instance.asNode(record.get("f").asNode())).build();
                    }
                }).thenCompose(response -> session.closeAsync().thenApply(signal -> response));
    }

    public CompletionStage<Response> delete(Driver driver, T instance, long id) {
        AsyncSession session = driver.asyncSession();
        String query = "MATCH (f:" + instance.getClass().getSimpleName() + ") WHERE id(f) = $id DELETE f";
        LOG.infof("" + query + " with " + id);
        return session
                .writeTransactionAsync(
                        tx -> tx.runAsync(query, Values.parameters("id", id)).thenCompose(fn -> fn.consumeAsync()))
                .thenCompose(response -> session.closeAsync()).thenApply(signal -> Response.noContent().build());
    }
}
