/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.model.impl.admin.locks;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;

import java.util.*;

public abstract class LockGraphManager {

    public static final String keyType = "type";
    public static final String typeWait = "wait";
    public static final String typeHold = "hold";

    private Map<Object, LockGraphNode> nodes = new HashMap<>();
    private Map<Object, LockGraph> graphIndex = new HashMap<>();

    public LockGraph getGraph(DBAServerLock curLock) {

        LockGraphNode selection = nodes.get(curLock.getId());

        LockGraph graph = graphIndex.get(curLock.getId());

        if (graph != null && selection != null) {
            graph.setSelection(selection);
        }

        return graph;
    }

    @SuppressWarnings("unchecked")
    private LockGraph createGraph(DBAServerLock root) {
        LockGraph graph = new LockGraph(root);

        int maxWidth = 1;
        int level = 1;
        LockGraphNode nodeRoot = nodes.get(root.getId());

        nodeRoot.setLevel(0);
        nodeRoot.setSpan(1);

        graph.getNodes().add(nodeRoot);
        graphIndex.put(root.getId(), graph);

        List<DBAServerLock> current = new ArrayList<>();
        Set<DBAServerLock> touched = new HashSet<>(); //Prevent Cycle

        current.add(root);
        touched.add(root);

        Map<Object, DBAServerLock> childs = new HashMap<>();

        while (current.size() > 0) {
            if (maxWidth < current.size()) {
                maxWidth = current.size();
            }

            for (int index = 0; index < current.size(); index++) {
                DBAServerLock l = current.get(index);
                LockGraphNode node = nodes.get(l.getId());

                if (index == 0) {
                    node.setLevelPosition(LockGraphNode.LevelPosition.LEFT);
                } else if (index == current.size() - 1) {
                    node.setLevelPosition(LockGraphNode.LevelPosition.RIGHT);
                } else {
                    node.setLevelPosition(LockGraphNode.LevelPosition.CENTER);
                }

                node.setSpan(current.size());


                for (DBAServerLock c : l.waitThis()) {

                    if (touched.contains(c)) continue;

                    touched.add(c);

                    childs.put(c.getId(), c);

                    graphIndex.put(c.getId(), graph);

                    LockGraphNode nodeChild = nodes.get(c.getId());

                    graph.getNodes().add(nodeChild);

                    nodeChild.setLevel(level);

                    LockGraphEdge edge = new LockGraphEdge();
                    edge.setSource(node);
                    edge.setTarget(nodeChild);

                }


            }

            level++;

            current = new ArrayList<>(childs.values());

            childs.clear();


        }

        graph.setMaxWidth(maxWidth);
        return graph;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void buildGraphs(Map<Object, ? extends DBAServerLock> locks) {

        Set<DBAServerLock> roots = new HashSet<>();

        this.nodes.clear();

        this.graphIndex.clear();

        for (DBAServerLock l : locks.values()) {

            if (locks.containsKey(l.getHoldID()) && (!l.getHoldID().equals(l.getId()))) {

                DBAServerLock holder = locks.get(l.getHoldID());
                l.setHoldBy(holder);
                holder.waitThis().add(l);

            } else {
                roots.add(l);
            }

            nodes.put(l.getId(), new LockGraphNode(l));
        }

        for (DBAServerLock root : roots) {
            createGraph(root);
        }

    }

}
