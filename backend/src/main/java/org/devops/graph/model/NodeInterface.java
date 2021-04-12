package org.devops.graph.model;

import java.util.Map;

import org.neo4j.driver.types.Node;

public interface NodeInterface {
    public NodeInterface asNode(Node node);

    public Map<String, Object> asMap();

    public long getInternalId();

    public String getPath();

    public String getVersion();

    public String getType();
}
