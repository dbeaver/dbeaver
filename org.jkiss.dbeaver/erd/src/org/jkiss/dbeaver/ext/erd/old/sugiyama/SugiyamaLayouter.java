/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.sugiyama;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.old.layout.Layouter;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterNode;
import org.jkiss.dbeaver.ext.erd.old.layout.LayouterObject;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.coordinate_assignment.SimpleMethod;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.crossing_reduction.SweepingMethod;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.cycle_removal.EadesLinSmyth;
import org.jkiss.dbeaver.ext.erd.old.sugiyama.layer_assignment.LongestPath;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class SugiyamaLayouter implements Layouter {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SugiyamaLayouter.class);
    
    /**
     * Default gap between nodes within a level.
     */
    public static int X_GAP = 100;
    
    /**
     * Default gap between levels.
     */
    public static int Y_GAP = 100;
    
    /**
     * An internal representation of the graph.
     */
    private SugiyamaGraph graph;

    /**
     * All edges in the layouter
     */
    private List<LayouterObject> edges;
    
    /**
     * All nodes in the layouter
     */
    private List<LayouterObject> nodes;
    
    /**
     * Construct a layouter.
     */    
    public SugiyamaLayouter() {
        edges = new ArrayList<LayouterObject>();
        nodes = new ArrayList<LayouterObject>();
        graph = new SugiyamaGraph();
    }
    
    /**
     * Add some object we want to layout.
     */
    public void add(LayouterObject obj) {
        if (obj instanceof SugiyamaNode) {
            log.debug("Adding node " + ((SugiyamaNode) obj).getNode().getId());
            graph.addNode((SugiyamaNode) obj);
            nodes.add(obj);
        } else if (obj instanceof SugiyamaEdge) {
            log.debug("Adding edge " + ((SugiyamaEdge) obj).getEdge().getId());
            graph.addEdge((SugiyamaEdge) obj);
            edges.add(obj);            
        } else {
            // TODO Containers...
        }
    }

    /**
     * Remove some object we want to layout.
     * @param obj object to remove
     */
    public void remove(LayouterObject obj) {

    }
    
    /**
     * Determine if the layouter contains a particular layoutable object
     * @param obj the object to find
     * @return true if the object is known to the layouter
     */
    public boolean contains(LayouterObject obj) {
        return nodes.contains(obj) || edges.contains(obj);
    }

    /**
     * Get the objects we want to layout.
     */
    public List<LayouterObject> getObjects() {
        List<LayouterObject> objects = new ArrayList<LayouterObject>(nodes);
        objects.addAll(edges);
        return objects;
    }

    /**
     * Get an object we want to layout.
     * @param index object index
     * @return object
     */
    public LayouterObject getObject(int index) {
        return null;
    }
      
    /**
     * Layout the graph; consists of five steps: temporarily remove any
     * directed cycles, assign the nodes to levels, reduce the number of
     * crossings, assign x-y coordinates to nodes and reroute edges.  
     */
    public void layout() {  
        log.debug("Starting layout of " + graph.getNodeCount() +
                " nodes and " + graph.getEdgeCount() + " edges");
        
        EadesLinSmyth hcr = new EadesLinSmyth(graph);
        LongestPath l = new LongestPath(graph);
        makeProper();
        SweepingMethod cr = new SweepingMethod(graph, l.getDepth());
        SimpleMethod sca = new SimpleMethod(graph, l.getDepth(), X_GAP, Y_GAP);
        routeEdges();
        
        graph = new SugiyamaGraph(); // reset all layout information for next time around
    }
    
    /**
     * Make the graph proper, i.e. insert dummy nodes so that no edge
     * spans more than one level.
     */
    private void makeProper() {
        log.debug("Making the graph proper");

        for (LayouterObject obj : edges) {
            SugiyamaEdge edge = (SugiyamaEdge) obj;
            SugiyamaNode tail = graph.getTail(edge);
            SugiyamaNode head = graph.getHead(edge);
            int span = tail.getLevel() - head.getLevel();
            if (span > 1) {
                graph.subdivideEdge(edge, span);
            }
        }
    }
    
    private void routeEdges() {
        for (LayouterObject edge : edges) {
            ((SugiyamaEdge) (edge)).route();
        }
    }

    /**
     * Get the dimension of the graph.
     * @return minimum size
     */
    public Dimension getMinimumDiagramSize() {
        return null;
    }
    
    public Rectangle getBounds() {
        Rectangle rect = null;
        for (LayouterObject obj : getObjects()) {
            if (obj instanceof LayouterNode) {
                if (rect == null) {
                    rect = ((LayouterNode) obj).getBounds();
                } else {
                    rect.add(((LayouterNode) obj).getBounds());
                }
            }
        }
        log.debug("Getting bounds of " + getObjects().size() + " objects as " + rect);
        return rect;
    }
    
    public void translate(int dx, int dy) {
        for (LayouterObject obj : getObjects()) {
            obj.translate(dx, dy);
        }
    }
    
    public Point getLocation() {
        return getBounds().getLocation();
    }
    
    public void setLocation(Point point) {
        Point oldPoint = getLocation();
        int dx = point.x - oldPoint.x;
        int dy = point.y - oldPoint.y;
        
        for (LayouterObject obj : getObjects()) {
            obj.translate(dx, dy);
        }
    }
}
