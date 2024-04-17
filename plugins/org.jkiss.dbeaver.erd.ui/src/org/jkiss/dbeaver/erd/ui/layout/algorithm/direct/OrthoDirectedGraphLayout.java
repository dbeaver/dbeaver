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

import org.eclipse.draw2d.graph.*;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;

import java.util.*;
import java.util.Map.Entry;

/**
 * The class represents node layout logic based on tree structure with max depth
 * edges equal of 2
 */
public class OrthoDirectedGraphLayout extends DirectedGraphLayout {

    private AbstractGraphicalEditPart diagram;
    private TreeMap<Integer, List<Node>> nodeByLevels;
    private List<Node> singleConnectedNodes = new LinkedList<>();
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
    private static final int DISTANCE_ENTITIES_X = 75;
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
    private static final int COLUMN_MAX = 8;
    /**
     * Pair of columns for islands representation 
     */
    private static final int COLUMN_ISLAND_MAX = 3;
    /**
     * Distance by X require to draw connection
     */
    private static final int DISTANCE_PER_EDGE_X = 7;

    /**
     * Distance by Y require to draw connection
     */
    private static final int DISTANCE_PER_EDGE_Y = 10;

    public OrthoDirectedGraphLayout(AbstractGraphicalEditPart diagram) {
        this.diagram = diagram;
    }

    /**
     * The visitor method
     */
    @Override
    public void visit(DirectedGraph graph) {
        nodeByLevels = computeRootNodes(graph);
        singleConnectedNodes = computeSingleConnectedNodes(graph);
        nodeByLevels = removeIslandNodesFromRoots(singleConnectedNodes, nodeByLevels);
        nodeByLevels = recomputeGraph(nodeByLevels);
        nodeByLevels = verifyNodesOnGraph(graph, nodeByLevels);
        drawGraphNodes(nodeByLevels);
        drawIsolatedNodes(singleConnectedNodes, nodeByLevels);
        List<Node> nodeMissed = findMissedGraphNodes(graph, nodeByLevels);
        Collections.sort(nodeMissed, (Node n1, Node n2) -> {
            if (n1.data instanceof EntityPart ep1 && n2.data instanceof EntityPart ep2) {
                return ep1.getName().compareTo(ep2.getName());
            }
            return 0;
        });
        drawMissedNodes(singleConnectedNodes, nodeMissed, nodeByLevels);
    }

    private int computeDistance(@NotNull Collection<Node> nodes) {
        int maxCountOfEdges = 0;
        int distance = DISTANCE_ENTITIES_X;
        for (Node node : nodes) {
            maxCountOfEdges += node.incoming.size() + node.outgoing.size();
        }
        if (maxCountOfEdges > 0) {
            distance += maxCountOfEdges * DISTANCE_PER_EDGE_X;
        }
        if (distance < DISTANCE_PER_EDGE_X) {
            distance = DISTANCE_ENTITIES_X;
        }
        return distance;
    }

    private int computeDistanceY(Node n) {
        int distance = (n.outgoing.size() + n.incoming.size()) * DISTANCE_PER_EDGE_Y;
        if (distance < DISTANCE_ENTITIES_Y) {
            distance = DISTANCE_ENTITIES_Y;
        }
        return distance;
    }

    private TreeMap<Integer, List<Node>> removeIslandNodesFromRoots(
        @NotNull List<Node> islands,
        @NotNull TreeMap<Integer, List<Node>> nodeByLevels
    ) {
        List<Node> listOfNodes = nodeByLevels.get(0);
        if (listOfNodes != null) {
            listOfNodes.removeAll(islands);
            nodeByLevels.put(0, listOfNodes);
        }
        return nodeByLevels;
    }

    private void drawIsolatedNodes(
        @NotNull List<Node> islandNodes,
        @NotNull TreeMap<Integer, List<Node>> mainNodes
    ) {
        Entry<Integer, List<Node>> lastEntry = mainNodes.lastEntry();
        int offsetX = OFFSET_FROM_LEFT;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = OFFSET_FROM_LEFT;
        int currentY = findBottomPosition(mainNodes);
        int distanceX = computeDistance(islandNodes);
        int offsetY = 0;
        for (Node nodeSource : islandNodes) {
            nodeSource.x = currentX;
            nodeSource.y = currentY;
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                nodeTarget.x = currentX + nodeSource.width + distanceX;
                nodeTarget.y = currentY;
                if (offsetY < nodeSource.height) {
                    offsetY = nodeSource.height;
                }
                if (offsetY < nodeTarget.height) {
                    offsetY = nodeTarget.height;
                }
                if ((islandNodes.indexOf(nodeSource) + 1) % COLUMN_ISLAND_MAX != 0) {
                    currentX += nodeSource.width + nodeTarget.width + distanceX + DISTANCE_ENTITIES_X;
                } else {
                    currentX = OFFSET_FROM_LEFT;
                    currentY += offsetY + DISTANCE_ENTITIES_Y / 2;
                }
            }
        }
    }

    private void drawMissedNodes(
        @NotNull List<Node> islands,
        @NotNull List<Node> missedNodes,
        @NotNull TreeMap<Integer, List<Node>> nodeByLevels
    ) {
        Entry<Integer, List<Node>> lastEntry = nodeByLevels.lastEntry();
        int offsetX = OFFSET_FROM_LEFT;
        for (Node n : lastEntry.getValue()) {
            if (offsetX < n.width) {
                offsetX = n.width;
            }
        }
        int currentX = OFFSET_FROM_LEFT;
        int currentY = findBottomPosition(nodeByLevels) + getBottomPositionIslands(islands);
        int curColumnIndex = 0;
        offsetX = currentX;
        int offsetY = currentY;
        int height = DISTANCE_ENTITIES_Y;
        for (Node node : missedNodes) {
            if (height < node.height) {
                height = node.height;
            }
            node.x = offsetX;
            node.y = offsetY;
            curColumnIndex++;
            if (curColumnIndex % COLUMN_MAX == 0) {
                // next row
                offsetY += height + DISTANCE_ENTITIES_Y;
                offsetX = currentX;
                height = 0;
            } else {
                offsetX += node.width + DISTANCE_ENTITIES_X / 2;
            }
        }
    }

    private int getBottomPositionIslands(List<Node> islands) {
        int positionY = 0;
        int offsetY = 0;
        for (Node nodeSource : islands) {
            for (Edge edge : nodeSource.outgoing) {
                Node nodeTarget = edge.target;
                if (offsetY < nodeSource.height) {
                    offsetY = nodeSource.height;
                }
                if (offsetY < nodeTarget.height) {
                    offsetY = nodeTarget.height;
                }
            }
            if ((islands.indexOf(nodeSource) + 1) % COLUMN_ISLAND_MAX == 0) {
                positionY += offsetY + DISTANCE_ENTITIES_Y / 2;
                offsetY = 0;
            }
        }
        if (offsetY != 0) {
            offsetY += DISTANCE_ENTITIES_Y;
        }
        return positionY + offsetY;
    }

    private void drawGraphNodes(@NotNull TreeMap<Integer, List<Node>> nodeByEdges) {
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
                    currentY += n.height + DISTANCE_ENTITIES_Y / nodes.size() + computeDistanceY(n);
                }
                if (nodeWidthMax < n.width) {
                    nodeWidthMax = n.width;
                }
            }
            if (!nodes.isEmpty()) {
                // next increase X
                currentX += nodeWidthMax + computeDistance(nodes);
            }
            index++;
        }
    }

    @NotNull
    private List<Node> computeSingleConnectedNodes(@NotNull DirectedGraph graph) {
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

    @NotNull
    private TreeMap<Integer, List<Node>> recomputeGraph(@NotNull TreeMap<Integer, List<Node>> graph) {
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

    @NotNull
    private List<Node> findMissedGraphNodes(
        @NotNull DirectedGraph graph,
        @NotNull TreeMap<Integer, List<Node>> nodeByEdges
    ) {
        List<Node> missedNodes = new ArrayList<>();
        for (Node node : graph.nodes) {
            boolean isContains = false;
            for (Entry<Integer, List<Node>> entry : nodeByEdges.entrySet()) {
                if (entry.getValue().contains(node) ||
                    singleConnectedNodes.contains(node)) {
                    isContains = true;
                    break;
                }
                // find as a target in isolated
                for (Node isoNode : singleConnectedNodes) {
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

    @NotNull
    private TreeMap<Integer, List<Node>> verifyNodesOnGraph(
        @NotNull DirectedGraph graph,
        @NotNull TreeMap<Integer, List<Node>> nodeByLevels
    ) {
        if (nodeByLevels.isEmpty() && !graph.nodes.isEmpty()) {
            // still no roots but elements exists in graph add all elements as possible
            // roots.
            nodeByLevels.put(0, graph.nodes);
        }
        return nodeByLevels;
    }

    private void createGraphLayers(
        @NotNull TreeMap<Integer, List<Node>> nodeByEdges, 
        int idx
    ) {
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
                    nodeByEdges.computeIfAbsent(idx + 1, n -> new ArrayList<>()).add(trg);
                }
            }
        }
        duplicationNode2index.forEach((key, value) -> nodeByEdges.get(value).remove(key));
    }

    @Nullable
    private Integer getNodeIndex(
        @NotNull TreeMap<Integer, List<Node>> nodeByEdges,
        @NotNull Node src
    ) {
        for (Entry<Integer, List<Node>> nodeOnLevel : nodeByEdges.entrySet()) {
            if (nodeOnLevel.getValue().contains(src)) {
                return nodeOnLevel.getKey();
            }
        }
        return null;
    }

    @NotNull
    private Map<Integer, Integer> computeHeight(@NotNull TreeMap<Integer, List<Node>> nodeByEdges) {
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

    /**
     * Compute last position value by X of main connected graph 
     */
    @SuppressWarnings("unused")
    private int findRightPosition(@NotNull TreeMap<Integer, List<Node>> nodeByEdges) {
        int positionByX;
        if (nodeByEdges.lastEntry().getValue().isEmpty()) {
            positionByX = OFFSET_FROM_LEFT;
        } else {
            Node lastNode = nodeByEdges.lastEntry().getValue().get(0);
            positionByX = lastNode.x + OFFSET_FROM_LEFT;
        }
        return positionByX;
    }

    /**
     * Compute last position value by Y of main connected graph
     */
    private int findBottomPosition(@NotNull TreeMap<Integer, List<Node>> nodeByEdges) {
        return OFFSET_FROM_TOP + DISTANCE_ENTITIES_Y + Collections.max(computeHeight(nodeByEdges).values());
    }
}