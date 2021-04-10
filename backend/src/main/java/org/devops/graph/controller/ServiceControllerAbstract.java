package org.devops.graph.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.devops.graph.model.NodeInterface;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
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

    public List<Long> findAny(Driver driver, T instance) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                List<Long> names = new ArrayList<>();
                Result result = tx.run("MATCH (a:" + instance.getClass().getSimpleName() + ") WHERE a.path = '"
                        + instance.getPath() + "' AND a.version = '" + instance.getVersion() + "' RETURN id(a)");
                while (result.hasNext()) {
                    names.add(result.next().get(0).asLong());
                }
                return names;
            });
        }
    }

    public CompletionStage<Response> create(Driver driver, T instance) {
        // Check for same entity with path and version
        var any = this.findAny(driver, instance);
        if (any.size() > 0) {
            return CompletableFuture.supplyAsync(() -> {
                Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(new HashMap<String, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put("error", "XXX");
                        put("message", "Node already exist with this path and this version");
                    }
                }).build();
            });
        }
        AsyncSession session = driver.asyncSession();
        Map<String, Object> fields = instance.asMap();
        // a node must contains path and version
        if (fields.containsKey("path") && fields.get("path") != null
                && fields.get("path").toString().matches("([a-zA-Z0-9/\056//\057/]+)+") && fields.containsKey("version")
                && fields.get("version") != null
                && fields.get("version").toString().matches("([a-zA-Z0-9/\056//\057/]+)+")) {
            StringBuffer sb = new StringBuffer();
            for (Entry<String, Object> field : fields.entrySet()) {
                if (sb.length() == 0) {
                    sb.append(field.getKey() + ": $" + field.getKey());
                } else {
                    sb.append(", " + field.getKey() + ": $" + field.getKey());
                }
            }
            // Store query to log it aned use it further
            String query = "CREATE (f:" + instance.getClass().getSimpleName() + " {" + sb.toString() + "}) RETURN f";
            LOG.infof("" + query + " with " + fields);
            return session
                    .writeTransactionAsync(
                            tx -> tx.runAsync(query, Values.value(fields)).thenCompose(fn -> fn.singleAsync()))
                    .thenApply(record -> instance.asNode(record.get("f").asNode()))
                    .thenCompose(persisted -> session.closeAsync().thenApply(signal -> persisted))
                    .thenApply(persisted -> Response.created(URI.create("/services/" + persisted.getInternalId()))
                            .build());
        } else {
            return CompletableFuture.supplyAsync(() -> {
                Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(new HashMap<String, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put("error", "XXX");
                        put("message",
                                "A node must contain a path and a version and match alphanumeric (plus . and /)");
                    }
                }).build();
            });
        }
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

    public CompletionStage<Response> delete(Driver driver, ITFactory<T> factory, long id) {
        AsyncSession session = driver.asyncSession();
        T instance = factory.create();
        String query = "MATCH (f:" + instance.getClass().getSimpleName() + ") WHERE id(f) = $id DELETE f";
        LOG.infof("" + query + " with " + id);
        return session
                .writeTransactionAsync(
                        tx -> tx.runAsync(query, Values.parameters("id", id)).thenCompose(fn -> fn.consumeAsync()))
                .thenCompose(response -> session.closeAsync()).thenApply(signal -> Response.noContent().build());
    }
}
