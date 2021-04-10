package org.devops.graph.model;

import org.neo4j.driver.types.Relationship;

public class RelationshipStd implements RelationshipInterface {

    public long internalId;
    public String type;
    public NodeStd from;
    public NodeStd to;

    public RelationshipInterface asRelationship(final Relationship relationShip) {
        this.internalId = relationShip.id();
        this.type = relationShip.type();
        return this;
    }

    public RelationshipStd() {
        // This is neaded for the REST-Easy JSON Binding
    }

    @Override
    public long getInternalId() {
        return internalId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public NodeInterface from() {
        return from;
    }

    @Override
    public NodeInterface to() {
        return to;
    }
}
