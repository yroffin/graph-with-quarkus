package org.devops.controller;

import org.neo4j.driver.types.Node;

public class Service {
    
    public Long id;

    public String name;

    public static Service from(Node node) {
        return new Service(node.id(), node.get("name").asString());
    }

    public Service() {
        // This is neaded for the REST-Easy JSON Binding
    }

    public Service(String name) {
        this.name = name;
    }

    public Service(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
