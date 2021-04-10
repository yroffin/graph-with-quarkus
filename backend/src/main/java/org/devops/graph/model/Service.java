package org.devops.graph.model;

import java.util.Map;

import org.jboss.logging.Logger;
import org.neo4j.driver.types.Node;

public class Service extends NodeStd {

    protected static final Logger LOG = Logger.getLogger(Service.class);

    public String technology;

    public Service() {
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = super.asMap();
        result.put("technology", this.technology);
        return result;
    }

    @Override
    public NodeInterface asNode(Node node) {
        super.asNode(node);
        this.technology = node.get("technology").asString();
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "Service [technology=" + technology + "]";
    }

}
