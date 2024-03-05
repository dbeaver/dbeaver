/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.erd.ui.layout.algorithm.direct;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import java.util.*;
import java.util.Map.Entry;

/**
 * The class represents node layout logic based on tree structure with max depth
 * edges equal of 2
 */
public class OrthoDirectedGraphLayout extends DirectedGraphLayout {

    private AbstractGraphicalEditPart diagram;
    private TreeMap<Integer, List<Node>> nodeByLevels;
    private List<Node> isolatedNodes = new LinkedList<>();
    /**
     * Initial offset from top line
     */
    private static final int OFFSET_FROM_TOP = 30;
    /**
     * Initial offset from left line
     */
    private static final int OFFSET_FROM_LEFT = 50;
    /**
     * Initial distance by X
     */
    private static final int DISTANCE_ENTITIES_X = 70;
    /**
     * Initial distance by Y
     */
    private static final int DISTANCE_ENTITIES_Y = 40;

    /**
     * How far elements can be placed to his child
     */
    private static final int DISTANCE_BTW_ELEMENT_PER_COLUMNS = 2;
    /**
     * Repeatable columns value
     */
    private static final int VIRTUAL_COLUMNS = 6;
    /**
     * Distance by X require to draw connection
     */
    private static final int DISTANCE_PER_EDGE_X = 5;

    /**
     * Distance by Y require to draw connection
     */
    private static final int DISTANCE_PER_EDGE_Y = 15;
    
    public OrthoDirectedGraphLayout(AbstractGraphicalEditPart diagram) {
        this.diagram = diagram;
    }

    /**
     * The visitor method
     */
    @Override
    public void visit(DirectedGraph graph) {
        nodeByLevels = computeRootNodes(graph);
        isolatedNodes = computeIsolatedNodes(graph);
        nodeByLevels = removeIslandNodesFromRoots(isolatedNodes, nodeByLevels);
        nodeByLevels = recomputeGraph(nodeByLevels);
        nodeByLevels = verifyNodesOnGraph(graph, nodeByLevels);
        drawGraphNodes(nodeByLevels);
        drawIsolatedNodes(isolatedNodes, nodeByLevels);
        List<Node> nodeMissed = findMissedGraphNodes(graph, nodeByLevels);
        drawMissedNodes(isolatedNodes, nodeMissed, nodeByLevels);
    }

    private int computeDistanceX(List<Node> nodes) {
        int maxCountOfEdges = 0;
        int distance = DISTANCE_ENTITIES_X;
        for (Node node : nodes) {
            maxCountOfEdges += node.incoming.size() + node.outgoing.size();
        }
        if (maxCountOfEdges > 0) {
            distance += maxCountOfEdges * DISTANCE_PER_EDGE_X;
        }
        return distance;
    }

    private int computeDistanceY(List<Node> nodes) {
        int maxCountOfEdges = 0;
        for (Node node : nodes) {
            maxCountOfEdges += node.incoming.size() + node.outgoing.size();
        }
        int distance = maxCountOfEdges * DISTANCE_PER_EDGE_Y;
        if (distance < DISTANCE_ENTITIES_Y) {
            distance = DISTANCE_ENTITIES_Y;
        }
        return distance;
    }

    private TreeMap<Integer, List<Node>> removeIslandNodesFromRoots(List<Node> islands, TreeMap<Integer, List<Node>> nodeByLevels) {
        List<Node> listOfNodes = nodeByLevels.get(0);
        if (listOfNodes != null) {
            listOfNodes.removeAll(islands);
            nodeByLevels.put(0, listOfNodes);
        }
        return nodeByLevels;
    }

    private void drawIsolatedNodes(List<Node> islands, TreeMap<Integer, List<Node>> nodeByLevels) {
        // considered to have a islands one - to -one
        Entry<Integer, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int offsetX = OFFSET_FROM_LEFT;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = 0;
        if (lastEntry.getValue().isEmpty()) {
            currentX = OFFSET_FROM_LEFT;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + offsetX + OFFSET_FROM_LEFT;
        }

        int currentY = OFFSET_FROM_TOP;
        for (Node nodeSource : islands) {
            nodeSource.x = currentX;
            nodeSource.y = currentY;
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                nodeTarget.x = currentX + nodeSource.width + DISTANCE_ENTITIES_X / 2;
                nodeTarget.y = currentY;
                if (nodeSource.height > nodeTarget.height) {
                    currentY += nodeSource.height + DISTANCE_ENTITIES_Y;
                } else {
                    currentY += nodeTarget.height + DISTANCE_ENTITIES_Y;
                }
            }
        }
    }

    private void drawMissedNodes(List<Node> islands, List<Node> missedNodes, TreeMap<Integer, List<Node>> nodeByLevels) {
        Entry<Integer, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int offsetX = OFFSET_FROM_LEFT;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = 0;
        if (lastEntry.getValue().isEmpty()) {
            currentX = OFFSET_FROM_LEFT;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + offsetX + OFFSET_FROM_LEFT;
        }
        int currentY = OFFSET_FROM_TOP;
        for (Node nodeSource : islands) {
            currentY += nodeSource.height + DISTANCE_ENTITIES_Y;
        }
        int virtColumns = 0;
        offsetX = currentX;
        int offsetY = currentY;
        int width = DISTANCE_ENTITIES_X;
        int height = DISTANCE_ENTITIES_Y;
        for (Node node : missedNodes) {
            if (width < node.width) {
                width = node.width;
            }
            if (height < node.height) {
                height = node.height;
            }
            node.x = offsetX;
            node.y = offsetY;
            virtColumns++;
            if (virtColumns % VIRTUAL_COLUMNS == 0) {
                // next row
                offsetY += height + DISTANCE_ENTITIES_Y;
                offsetX = currentX;
                width = DISTANCE_ENTITIES_X;
                height = DISTANCE_ENTITIES_Y;
            } else {
                offsetX += width + OFFSET_FROM_LEFT / 7;
            }
        }
    }

    private void drawGraphNodes(TreeMap<Integer, List<Node>> nodeByEdges) {
        int currentX = OFFSET_FROM_LEFT;
        int currentY = OFFSET_FROM_TOP;
        // here we place all elements from middle line
        Map<Integer, Integer> height2Level = computeHeight(nodeByLevels);
        int middle = Collections.max(height2Level.values()) / 2;
        int index = 0;
        for (Entry<Integer, List<Node>> entry : nodeByEdges.entrySet()) {
            Integer height = height2Level.get(index);
            if (height / 2 > middle) {
                currentY = OFFSET_FROM_TOP;
            } else {
                currentY = OFFSET_FROM_TOP + middle - height / 2;
            }
            List<Node> nodes = entry.getValue();
            int nodeWidthMax = 0;
            for (Node n : nodes) {
                n.x = currentX;
                n.y = currentY;
                if (index == 0) {
                    currentY += n.height + DISTANCE_ENTITIES_Y;
                } else {
                    currentY += n.height + computeDistanceY(nodeByEdges.get(index));
                }
                if (nodeWidthMax < n.width) {
                    nodeWidthMax = n.width;
                }
            }
            if (!nodes.isEmpty()) {
                // next increase X
                currentX += nodeWidthMax + computeDistanceX(nodes);
            }
            index++;
        }
    }

    private List<Node> computeIsolatedNodes(DirectedGraph graph) {
        List<Node> isolated = new LinkedList<>();
        for (int i = 0; i < graph.nodes.size(); i++) {
            Node node = graph.nodes.get(i);
            if (node.outgoing.size() == 1 && node.incoming.isEmpty()) {
                boolean hasNoFurtherConnections = false;
                Node nodeTarget = null;
                for (Edge edge : node.outgoing) {
                    nodeTarget = edge.target;
                    if (nodeTarget != null &&
                        nodeTarget.outgoing.isEmpty() &&
                        nodeTarget.incoming.size() == 1) {
                        hasNoFurtherConnections = true;
                    } else {
                        hasNoFurtherConnections = false;
                    }
                }
                if (hasNoFurtherConnections) {
                    isolated.add(node);
                }
            }
        }
        return isolated;
    }

    private TreeMap<Integer, List<Node>> computeRootNodes(DirectedGraph graph) {
        TreeMap<Integer, List<Node>> nodes = new TreeMap<>();
        List<Node> firstLineOutput = new LinkedList<>();
        for (int i = 0; i < graph.nodes.size(); i++) {
            Node node = graph.nodes.get(i);
            if (!node.outgoing.isEmpty() && node.incoming.isEmpty()) {
                firstLineOutput.add(node);
            }
        }
        if (!firstLineOutput.isEmpty()) {
            nodes.put(0, firstLineOutput);
        }
        return nodes;
    }

    private TreeMap<Integer, List<Node>> recomputeGraph(TreeMap<Integer, List<Node>> graph) {
        int idx = 0;
        while (idx < graph.keySet().size()) {
            createGraphLayers(graph, idx);
            // catch empty nodes
            List<Node> nextLevelNodes = graph.get(idx + 1);
            if (nextLevelNodes != null && graph.get(idx).isEmpty() && !nextLevelNodes.isEmpty()) {
                // shiftAllElements
                graph.put(idx, nextLevelNodes);
                graph.remove(idx + 1);
            }
            idx++;
        }
        return graph;
    }

    private List<Node> findMissedGraphNodes(DirectedGraph graph, TreeMap<Integer, List<Node>> nodeByEdges) {
        List<Node> missedNodes = new ArrayList<>();
        for (Node node : graph.nodes) {
            boolean isContains = false;
            for (Entry<Integer, List<Node>> entry : nodeByEdges.entrySet()) {
                if (entry.getValue().contains(node) ||
                    isolatedNodes.contains(node)) {
                    isContains = true;
                    break;
                }
                // find as a target in isolated
                for (Node isoNode : isolatedNodes) {
                    for (Edge edge : isoNode.outgoing) {
                        Node nodeTarget = edge.target;
                        if (node.equals(nodeTarget)) {
                            isContains = true;
                            break;
                        }
                    }
                }
            }
            if (!isContains) {
                missedNodes.add(node);
            }
        }
        return missedNodes;
    }

    private TreeMap<Integer, List<Node>> verifyNodesOnGraph(DirectedGraph graph, TreeMap<Integer, List<Node>> nodeByLevels) {
        if (nodeByLevels.isEmpty() && !graph.nodes.isEmpty()) {
            // still no roots but elements exists in graph add all elements as possible
            // roots.
            nodeByLevels.put(0, graph.nodes);
        }
        return nodeByLevels;
    }

    private void createGraphLayers(TreeMap<Integer, List<Node>> nodeByEdges, int idx) {
        Map<Node, Integer> duplicationNode2index = new HashMap<>();
        List<Node> nodesLine = nodeByEdges.get(idx);
        for (Node inNode : nodesLine) {
            for (Edge edge : inNode.outgoing) {
                // parent
                Node src = edge.source;
                if (src != null && !src.equals(inNode)) {
                    Integer nodeIndex = getNodeIndex(nodeByEdges, src);
                    if (nodeIndex != null) {
                        duplicationNode2index.put(src, nodeIndex);
                    }
                    nodeByEdges.computeIfAbsent(idx + 2, n -> new ArrayList<Node>()).add(src);
                }
                // next
                Node trg = edge.target;
                if (trg != null) {
                    Integer nodeIndex = getNodeIndex(nodeByEdges, trg);
                    // skip by distance
                    boolean skip = false;
                    for (Edge e : trg.incoming) {
                        Node incomingSourceNode = e.source;
                        Integer childIndex = getNodeIndex(nodeByEdges, incomingSourceNode);
                        if (childIndex != null && childIndex != 0 && (idx - childIndex) > DISTANCE_BTW_ELEMENT_PER_COLUMNS) {
                            skip = true;
                            break;
                        }
                    }
                    if (skip) {
                        continue;
                    }
                    if (nodeIndex != null) {
                        if (duplicationNode2index.containsKey(trg)) {
                            continue;
                        }
                        duplicationNode2index.put(trg, nodeIndex);
                    }
                    nodeByEdges.computeIfAbsent(idx + 1, n -> new ArrayList<Node>()).add(trg);
                }
            }
        }
        duplicationNode2index.forEach((key, value) -> nodeByEdges.get(value).remove(key));
    }

    private Integer getNodeIndex(TreeMap<Integer, List<Node>> nodeByEdges, Node src) {
        for (Entry<Integer, List<Node>> nodeOnLevel : nodeByEdges.entrySet()) {
            if (nodeOnLevel.getValue().contains(src)) {
                return nodeOnLevel.getKey();
            }
        }
        return null;
    }

    private Map<Integer, Integer> computeHeight(TreeMap<Integer, List<Node>> nodeByEdges) {
        Map<Integer, Integer> mapOfHeight = new HashMap<>();
        for (Entry<Integer, List<Node>> entry : nodeByEdges.entrySet()) {
            int height = 0;
            for (Node node : entry.getValue()) {
                height += node.height + DISTANCE_ENTITIES_Y;
            }
            mapOfHeight.put(entry.getKey(), height);
        }
        return mapOfHeight;
    }

    /**
     * The method to return diagram edit part
     *
     * @return - return a diagram editor part
     */
    public AbstractGraphicalEditPart getDiagram() {
        return diagram;
    }

}