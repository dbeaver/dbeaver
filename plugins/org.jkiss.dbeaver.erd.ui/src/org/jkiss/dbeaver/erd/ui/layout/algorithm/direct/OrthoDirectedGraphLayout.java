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
    private TreeMap<String, List<Node>> nodeByLevels;
    private List<Node> isolatedNodes = new LinkedList<>();
    private static final int DEFAUL_OFFSET_FROM_TOP_LINE = 20;
    private static final int DEFAULT_ISO_OFFSET_HORZ = 140;
    private static final int DEFAULT_OFFSET_BY_X = 250;
    private static final int DEFAULT_OFFSET_BY_Y = 80;
    private static final int VIRTUAL_COLUMNS = 5;

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

    private TreeMap<String, List<Node>> removeIslandNodesFromRoots(List<Node> islands, TreeMap<String, List<Node>> nodeByLevels) {
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
        int offsetX = DEFAULT_ISO_OFFSET_HORZ;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = 0;
        if (lastEntry.getValue() == null || lastEntry.getValue().isEmpty()) {
            currentX = DEFAULT_ISO_OFFSET_HORZ;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + offsetX + DEFAULT_ISO_OFFSET_HORZ;
        }

        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        for (Node nodeSource : islands) {
            nodeSource.x = currentX;
            nodeSource.y = currentY;
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                nodeTarget.x = currentX + nodeSource.width + DEFAULT_OFFSET_BY_X / 2;
                nodeTarget.y = currentY;
                if (nodeSource.height > nodeTarget.height) {
                    currentY += nodeSource.height + DEFAULT_OFFSET_BY_Y;
                } else {
                    currentY += nodeTarget.height + DEFAULT_OFFSET_BY_Y;
                }
            }
        }
    }

    private void drawMissedNodes(List<Node> islands, List<Node> missedNodes, TreeMap<String, List<Node>> nodeByLevels) {
        Entry<String, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int offsetX = DEFAULT_ISO_OFFSET_HORZ;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = 0;
        if (lastEntry.getValue() == null || lastEntry.getValue().isEmpty()) {
            currentX = DEFAULT_ISO_OFFSET_HORZ;
        } else {
            Node lastNode = nodeByLevels.lastEntry().getValue().get(0);
            currentX = lastNode.x + offsetX + DEFAULT_ISO_OFFSET_HORZ;
        }
        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        for (Node nodeSource : islands) {
            currentY += nodeSource.height + DEFAULT_OFFSET_BY_Y;
        }
        int virtColumns = 0;
        offsetX = currentX;
        int offsetY = currentY;
        int width = DEFAULT_OFFSET_BY_X;
        int height = DEFAULT_OFFSET_BY_Y;
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
                offsetY += height + DEFAULT_OFFSET_BY_Y;
                offsetX = currentX;
                width = DEFAULT_OFFSET_BY_X;
                height = DEFAULT_OFFSET_BY_Y;
            } else {
                offsetX += width + DEFAULT_ISO_OFFSET_HORZ / 7;
            }
        }
    }

    private void drawGraphNodes(TreeMap<String, List<Node>> nodeByEdges) {
        int currentX = DEFAUL_OFFSET_FROM_TOP_LINE;
        int currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
        // here we place all elements from middle line
        Map<String, Integer> height2Level = computeHeight(nodeByLevels);
        int middle = Collections.max(height2Level.values()) / 2;
        int index = 0;
        for (Entry<String, List<Node>> entry : nodeByEdges.entrySet()) {
            Integer height = height2Level.get(String.valueOf(index));
            if (height / 2 > middle) {
                currentY = DEFAUL_OFFSET_FROM_TOP_LINE;
            } else {
                currentY = DEFAUL_OFFSET_FROM_TOP_LINE + middle - height / 2;
            }
            List<Node> nodes = entry.getValue();
            int offsetX = DEFAULT_ISO_OFFSET_HORZ;
            for (Node n : nodes) {
                n.x = currentX;
                n.y = currentY;
                currentY += n.height + DEFAULT_OFFSET_BY_Y / 2 + (DEFAULT_OFFSET_BY_Y / VIRTUAL_COLUMNS * (index % VIRTUAL_COLUMNS));
                if (offsetX < n.width) {
                    offsetX = n.width;
                }
            }
            if (!nodes.isEmpty()) {
                // next increase X
                currentX += DEFAULT_ISO_OFFSET_HORZ + offsetX;
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

    private TreeMap<String, List<Node>> computeRootNodes(DirectedGraph graph) {
        TreeMap<String, List<Node>> nodeByLevels = new TreeMap<>();
        List<Node> firstLineOutput = new LinkedList<>();
        for (int i = 0; i < graph.nodes.size(); i++) {
            Node node = graph.nodes.get(i);
            if (node.outgoing.size() > 0 && node.incoming.size() == 0) {
                firstLineOutput.add(node);
            }
        }
        if (!firstLineOutput.isEmpty()) {
            nodeByLevels.put(String.valueOf(0), firstLineOutput);
        }
        return nodeByLevels;
    }

    private TreeMap<String, List<Node>> recomputeGraph(TreeMap<String, List<Node>> graph) {
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

    private TreeMap<String, List<Node>> verifyNodesOnGraph(DirectedGraph graph, TreeMap<String, List<Node>> nodeByLevels) {
        if (nodeByLevels.isEmpty() && !graph.nodes.isEmpty()) {
            // still no roots but elements exists in graph add all elements as possible
            // roots.
            nodeByLevels.put(String.valueOf(0), graph.nodes);
        }
        // 
        return nodeByLevels;
    }

    private void createGraphLayers(TreeMap<String, List<Node>> nodeByEdges, int idx) {
        Map<Node, String> duplicationNode2index = new HashMap<>();
        List<Node> nodesLine = nodeByEdges.get(String.valueOf(idx));
        for (Node inNode : nodesLine) {
            for (Edge edge : inNode.outgoing) {
                // parent
                Node src = edge.source;
                if (src != null && !src.equals(inNode)) {
                    String nodeIndex = getNodeIndex(nodeByEdges, src);
                    if (!nodeIndex.isEmpty()) {
                        duplicationNode2index.put(src, nodeIndex);
                    }
                    String index = String.valueOf(idx + 2);
                    nodeByEdges.computeIfAbsent(index, n -> new ArrayList<Node>()).add(src);
                }
                // next
                Node trg = edge.target;
                if (trg != null) {
                    String nodeIndex = getNodeIndex(nodeByEdges, trg);
                    if (!nodeIndex.isEmpty()) {
                        duplicationNode2index.put(trg, nodeIndex);
                    }
                    String index = String.valueOf(idx + 1);
                    nodeByEdges.computeIfAbsent(index, n -> new ArrayList<Node>()).add(trg);
                }
            }
        }
        duplicationNode2index.entrySet().stream().forEach(node2index -> {
            nodeByEdges.get(node2index.getValue()).remove(node2index.getKey());
        });
    }

    private String getNodeIndex(TreeMap<String, List<Node>> nodeByEdges, Node src) {
        for (Entry<String, List<Node>> nodeOnLevel : nodeByEdges.entrySet()) {
            if (nodeOnLevel.getValue().contains(src)) {
                return nodeOnLevel.getKey();
            }
        }
        return "";
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