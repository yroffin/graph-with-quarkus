package org.devops.graph.model;

import org.neo4j.driver.types.Relationship;

public interface RelationshipInterface {
    public RelationshipInterface asRelationship(Relationship relationShip);

    public NodeInterface from();

    public NodeInterface to();

    public String getType();

    public long getInternalId();
}
