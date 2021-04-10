package org.devops.graph.model;

import java.util.Map;
import java.util.TreeMap;

import org.neo4j.driver.types.Node;

public class NodeStd implements NodeInterface {
    
    public long internalId;
    public String name;

    public NodeInterface asNode(Node node) {
        this.internalId = node.id();
        this.name = node.get("name").asString();
        return this;
    }

    public NodeStd() {
        // This is neaded for the REST-Easy JSON Binding
    }

    public NodeStd(String name) {
        this.name = name;
    }

    public NodeStd(long id, String name) {
        this.internalId = id;
        this.name = name;
    }

    @Override
    public long getInternalId() {
        return internalId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("id", this.internalId);
        result.put("name", this.name);
        return result;
    }

    @Override
    public String toString() {
        return "NodeStd [internalId=" + internalId + ", name=" + name + "]";
    }
}
