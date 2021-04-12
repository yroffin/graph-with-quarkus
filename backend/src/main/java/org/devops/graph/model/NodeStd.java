package org.devops.graph.model;

import java.util.Map;
import java.util.TreeMap;

import org.neo4j.driver.types.Node;

public class NodeStd implements NodeInterface {

    public long internalId;
    public String type;
    public String path;
    public String version;

    public NodeInterface asNode(Node node) {
        this.internalId = node.id();
        this.path = node.get("path").asString();
        this.version = node.get("version").asString();
        return this;
    }

    public NodeStd() {
        // This is neaded for the REST-Easy JSON Binding
        type = this.getClass().getSimpleName();
    }

    @Override
    public long getInternalId() {
        return internalId;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("type", this.type);
        result.put("id", this.internalId);
        result.put("path", this.path);
        result.put("version", this.version);
        return result;
    }

    @Override
    public String toString() {
        return "NodeStd [internalId=" + internalId + ", path=" + path + ", version=" + version + "]";
    }
}
