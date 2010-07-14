// $Id: SugiyamaLayouter.java 128 2006-08-29 13:59:33Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.sugiyama;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.layout.Layouter;
import org.jkiss.dbeaver.ext.erd.layout.LayouterNode;
import org.jkiss.dbeaver.ext.erd.layout.LayouterObject;
import org.jkiss.dbeaver.ext.erd.sugiyama.coordinate_assignment.SimpleMethod;
import org.jkiss.dbeaver.ext.erd.sugiyama.crossing_reduction.SweepingMethod;
import org.jkiss.dbeaver.ext.erd.sugiyama.cycle_removal.EadesLinSmyth;
import org.jkiss.dbeaver.ext.erd.sugiyama.layer_assignment.LongestPath;

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
