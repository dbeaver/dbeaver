/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The class represents node layout logic based on tree structure with max depth
 * edges equal of 2
 */
public class OrthoDirectedGraphLayout extends DirectedGraphLayout {

    private AbstractGraphicalEditPart diagram;
    private TreeMap<String, List<Node>> nodeByLevels = new TreeMap<>();
    private List<Node> isolatedNodes = new LinkedList<>();
    private static final int DEFAUL_OFFSET_FROM_TOP_LINE = 20;
    private static final int DEFAULT_ISO_OFFSET_HORZ = 100;
    private static final int DEFAULT_OFFSET_BY_X = 250;
    private static final int DEFAULT_OFFSET_BY_Y = 40;

    public OrthoDirectedGraphLayout(AbstractGraphicalEditPart diagram) {
        this.diagram = diagram;
    }

    /**
     * The visitor method
     */
    @Override
    public void visit(DirectedGraph graph) {
        computeRootNodes(graph, nodeByLevels);
        isolatedNodes = computeIsolatedNodes(graph);
        nodeByLevels = balanceRoots(isolatedNodes, nodeByLevels);
        nodeByLevels = computeGraph(nodeByLevels);
        nodeByLevels = verifyDirectedGraph(graph, nodeByLevels);
        nodeByLevels = rebalanceGraph(nodeByLevels);
        drawGraphNodes(nodeByLevels);
        drawIsolatedNodes(isolatedNodes, nodeByLevels);
        List<Node> nodeMissed = findMissedGraphNodes(graph, nodeByLevels);
        drawMissedNodes(isolatedNodes, nodeMissed, nodeByLevels);
    }

    private TreeMap<String, List<Node>> verifyDirectedGraph(DirectedGraph graph, TreeMap<String, List<Node>> nodeByLevels) {
        if (nodeByLevels.isEmpty() && !graph.nodes.isEmpty()) {
            // still no roots but elements exists in graph add all elements as possible roots.
            nodeByLevels.put(String.valueOf(0), graph.nodes);
        }
        return nodeByLevels;
    }

    private TreeMap<String, List<Node>> balanceRoots(List<Node> islands, TreeMap<String, List<Node>> nodeByLevels) {
        List<Node> listOfNodes = nodeByLevels.get(String.valueOf(0));
        if (listOfNodes != null) {
            listOfNodes.removeAll(islands);
            nodeByLevels.put(String.valueOf(0), listOfNodes);
        }
        return nodeByLevels;
    }

    private void drawIsolatedNodes(List<Node> islands, TreeMap<String, List<Node>> nodeByLevels) {
        // considered to have a islands one - to -one
        Entry<String, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int currentX = 0;
        if (lastEntry.getValue() == null || lastEntry.getValue().isEmpty()) {
            currentX = DEFAULT_ISO_OFFSET_HORZ;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + lastNode.width + DEFAULT_ISO_OFFSET_HORZ;
        }

        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        for (Node nodeSource : islands) {
            nodeSource.x = currentX;
            nodeSource.y = currentY;
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                nodeTarget.x = currentX + DEFAULT_OFFSET_BY_X;
                nodeTarget.y = currentY;
                currentY += nodeTarget.height + DEFAULT_OFFSET_BY_Y;
            }
        }
    }

    private void drawMissedNodes(List<Node> islands, List<Node> missedNodes, TreeMap<String, List<Node>> nodeByLevels) {
        Entry<String, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int currentX = 0;
        if (lastEntry.getValue() == null || lastEntry.getValue().isEmpty()) {
            currentX = DEFAULT_ISO_OFFSET_HORZ;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + lastNode.width + DEFAULT_ISO_OFFSET_HORZ;
        }
        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        for (Node nodeSource : islands) {
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                currentY += nodeTarget.height + DEFAULT_OFFSET_BY_Y;
            }
        }
        for (Node node : missedNodes) {
            node.x = currentX;
            node.y = currentY;
            currentY += node.height + DEFAULT_OFFSET_BY_Y;
        }
    }

    private boolean isRebalanceRequire(Map<String, Integer> heightByLevels) {
        Rectangle size = getMonitorSize();
        for (Entry<String, Integer> entry : heightByLevels.entrySet()) {
            if (entry.getValue() > size.height) {
                return true;
            }
        }
        return false;
    }

    private TreeMap<String, List<Node>> rebalanceGraph(TreeMap<String, List<Node>> nodeByLevels) {
        Rectangle size = getMonitorSize();
        Map<String, Integer> heightByLevels = computeHeight(nodeByLevels);
        while (isRebalanceRequire(heightByLevels)) {
            for (Entry<String, Integer> entry : heightByLevels.entrySet()) {
                if (entry.getValue() > size.height) {
                    int index = Integer.valueOf(entry.getKey());
                    List<Node> list = nodeByLevels.get(entry.getKey());
                    Node node = list.remove(0);
                    nodeByLevels.put(String.valueOf(index), list);
                    nodeByLevels.computeIfAbsent(String.valueOf(index + 1), n -> new ArrayList<Node>()).add(node);
                }
            }
            heightByLevels = computeHeight(nodeByLevels);
        }
        return nodeByLevels;
    }

    private Rectangle getMonitorSize() {
        Rectangle size = Display.getDefault().getBounds();
        if (size == null) {
            size = new Rectangle(0, 0, 1080, 2040); // default resolution
        }
        return size;
    }

    private void drawGraphNodes(TreeMap<String, List<Node>> nodeByEdges) {
        int currentX = DEFAUL_OFFSET_FROM_TOP_LINE;
        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        // here we place all elements from middle line
        Map<String, Integer> height2Level = computeHeight(nodeByLevels);
        int middle = Collections.max(height2Level.values()) / 2;
        int index = 0;
        for (Entry<String, List<Node>> entry : nodeByEdges.entrySet()) {
            Integer integer = height2Level.get(String.valueOf(index));
            if (integer != null) {
                int offset = middle - integer / 2;
                if (offset <= DEFAULT_OFFSET_BY_Y) {
                    offset = DEFAULT_OFFSET_BY_Y * 2;
                }
                currentY = offset;
            } else {
                currentY = DEFAULT_OFFSET_BY_Y;
            }
            List<Node> nodes = entry.getValue();
            for (Node n : nodes) {
                n.x = currentX;
                n.y = currentY;
                currentY += n.height + DEFAULT_OFFSET_BY_Y;
            }
            if (!nodes.isEmpty()) {
                // next increase X
                currentX += DEFAULT_OFFSET_BY_X;
            }
            index++;
        }
    }

    private List<Node> computeIsolatedNodes(DirectedGraph graph) {
        List<Node> isolated = new LinkedList<>();
        for (int i = 0; i < graph.nodes.size(); i++) {
            Node node = graph.nodes.get(i);
            if (node.outgoing.size() == 1 && node.incoming.size() == 0) {
                boolean hasNoFurtherConnections = false;
                Node nodeTarget = null;
                for (Edge edge : node.outgoing) {
                    nodeTarget = edge.target;
                    if (nodeTarget != null &&
                        nodeTarget.outgoing.size() == 0 &&
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

    private void computeRootNodes(DirectedGraph graph, TreeMap<String, List<Node>> nodeByEdges) {
        List<Node> firstLineOutput = new LinkedList<>();
        for (int i = 0; i < graph.nodes.size(); i++) {
            Node node = graph.nodes.get(i);
            if (node.outgoing.size() > 0 && node.incoming.size() == 0) {
                firstLineOutput.add(node);
            }
        }
        if (!firstLineOutput.isEmpty()) {
            nodeByEdges.put(String.valueOf(0), firstLineOutput);
        }
    }

    private TreeMap<String, List<Node>> computeGraph(TreeMap<String, List<Node>> graph) {
        int idx = 0;
        while (idx < graph.keySet().size()) {
            createGraphLayers(graph, idx);
            idx++;
        }
        return graph;
    }

    private List<Node> findMissedGraphNodes(DirectedGraph graph, TreeMap<String, List<Node>> nodeByEdges) {
        List<Node> missedNodes = new ArrayList<>();
        for (Node node : graph.nodes) {
            boolean isContains = false;
            for (Entry<String, List<Node>> entry : nodeByEdges.entrySet()) {
                if (entry.getValue().contains(node) ||
                    isolatedNodes.contains(node)) {
                    isContains = true;
                    break;
                }
                // find as a target in isolated
                for (Node isoNode : isolatedNodes) {
                    for (Edge edge : isoNode.outgoing) {
                        Node nodeTarget = edge.target;
                        if (nodeTarget != null && node.equals(nodeTarget)) {
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

    private void createGraphLayers(TreeMap<String, List<Node>> nodeByEdges, int idx) {
        Map<String, Node> duplicatedNode2Position = new HashMap<>();
        List<Node> nodesLine = nodeByEdges.get(String.valueOf(idx));
        for (Node inNode : nodesLine) {
            for (Edge edge : inNode.outgoing) {
                // parent
                Node src = edge.source;
                if (src != null && !src.equals(inNode)) {
                    // set on the next level
                    nodeByEdges.computeIfAbsent(String.valueOf(idx + 2), n -> new ArrayList<Node>()).add(src);
                }
                // next
                Node trg = edge.target;
                if (trg != null) {
                    // a way to paste node in deepest level
                    Map<String, Node> duplication = getDuplication(trg, nodeByEdges);
                    duplicatedNode2Position.putAll(duplication);
                    nodeByEdges.computeIfAbsent(String.valueOf(idx + 1), n -> new ArrayList<Node>()).add(trg);
                }
            }
        }
        duplicatedNode2Position.entrySet().stream().forEach(c -> {
            nodeByEdges.get(c.getKey()).remove(c.getValue());
        });
    }

    private Map<String, Node> getDuplication(Node trg, TreeMap<String, List<Node>> nodeByEdges) {
        Map<String, Node> mapNode2Position = new HashMap<>();
        for (Entry<String, List<Node>> entry : nodeByEdges.entrySet()) {
            if (entry.getValue().contains(trg)) {
                mapNode2Position.put(entry.getKey(), trg);
            }
        }
        return mapNode2Position;
    }

    private Map<String, Integer> computeHeight(TreeMap<String, List<Node>> nodeByEdges) {
        Map<String, Integer> mapOfHeight = new HashMap<>();
        for (Entry<String, List<Node>> entry : nodeByEdges.entrySet()) {
            int height = 0;
            for (Node node : entry.getValue()) {
                height += node.height + DEFAULT_OFFSET_BY_Y;
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