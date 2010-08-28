/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.model;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.GraphConstants;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

/**
 * ERDLink
 */
public class ERDLink extends DefaultEdge {

    private DBSForeignKey foreignKey;
    private ERDNode sourceNode;
    private ERDNode targetNode;

    public ERDLink(DBSForeignKey foreignKey, ERDNode sourceNode, ERDNode targetNode)
    {
        super(foreignKey);
        this.foreignKey = foreignKey;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    public DBSForeignKey getForeignKey()
    {
        return foreignKey;
    }

    public ERDNode getTargetNode()
    {
        return targetNode;
    }

    public ERDNode getSourceNode()
    {
        return sourceNode;
    }

    public String getTipString()
    {
        return foreignKey.getName();
    }

    public void setPoints(Point[] points)
    {
        GraphConstants.setPoints(getAttributes(), Arrays.asList(points));
    }

    public Point[] getPoints()
    {
        List points = GraphConstants.getPoints(getAttributes());
        if (points == null) {
            return new Point[0];
        }
        Point[] pa = new Point[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point2D point2D = (Point2D) points.get(i);
            if (point2D instanceof Point) {
                pa[i] = (Point)point2D;
            } else {
                pa[i] = new Point((int)point2D.getX(), (int)point2D.getY());
            }
        }
        return pa;
    }

    public void translate(int dx, int dy)
    {
        Point2D location = GraphConstants.getOffset(getAttributes());
        location.setLocation(location.getX() + dx, location.getY() + dy);
        GraphConstants.setOffset(getAttributes(), location);
    }

    public String getId()
    {
        return foreignKey.getTable().getFullQualifiedName() + "::" + foreignKey.getName();
    }

    public void computeRoute()
    {
        //System.out.println("computeRoute!");
    }
}
