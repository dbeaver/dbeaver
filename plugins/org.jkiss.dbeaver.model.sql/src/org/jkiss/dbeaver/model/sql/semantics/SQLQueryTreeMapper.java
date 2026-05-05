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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.*;

class SQLQueryTreeMapper<T, C> {

    @FunctionalInterface
    protected interface TreeMapperCallback<T, C> {
        T apply(STMTreeNode node, List<T> children, C context);
    }

    private interface MapperFrame {
        void doWork();
    }

    private interface MapperResultFrame<T> extends MapperFrame {
        void aggregate(@NotNull T result);
    }

    private abstract class MapperNodeFrame implements MapperFrame {
        @NotNull
        public final STMTreeNode node;
        @NotNull
        public final MapperResultFrame<T> parent;

        public MapperNodeFrame(@NotNull STMTreeNode node, @NotNull MapperResultFrame<T> parent) {
            this.node = node;
            this.parent = parent;
        }
    }

    private class MapperQueuedNodeFrame extends MapperNodeFrame {

        public MapperQueuedNodeFrame(@NotNull STMTreeNode node, @NotNull MapperResultFrame<T> parent) {
            super(node, parent);
        }

        @Override
        public void doWork() {
            TreeMapperCallback<T, C> translation = translations.get(node.getNodeName());
            MapperResultFrame<T> aggregator = translation == null ? parent : new MapperDataPendingNodeFrame(node, parent, translation);

            if (translation != null) {
                stack.push(aggregator);
            }
            List<STMTreeNode> children = node.findNonErrorChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                if (transparentNodeNames.contains(node.getNodeName())) {
                    stack.push(new MapperQueuedNodeFrame(children.get(i), aggregator));
                }
            }
        }
    }

    private class MapperDataPendingNodeFrame extends MapperNodeFrame implements MapperResultFrame<T> {
        @NotNull
        public final List<T> childrenData = new LinkedList<>();
        @NotNull
        public final TreeMapperCallback<T, C> translation;

        public MapperDataPendingNodeFrame(
            @NotNull STMTreeNode node,
            @NotNull MapperResultFrame<T> parent,
            @NotNull TreeMapperCallback<T, C> translation
        ) {
            super(node, parent);
            this.translation = translation;
        }

        @Override
        public void aggregate(@NotNull T result) {
            this.childrenData.add(result);
        }

        @Override
        public void doWork() {
            this.parent.aggregate(this.translation.apply(this.node, this.childrenData, SQLQueryTreeMapper.this.context));
        }
    }

    private class MapperRootFrame implements MapperResultFrame<T> {
        @NotNull
        public final STMTreeNode node;
        @Nullable
        public T result = null;

        public MapperRootFrame(@NotNull STMTreeNode node) {
            this.node = node;
        }

        @Override
        public void aggregate(@NotNull T result) {
            this.result = result;
        }

        @Override
        public void doWork() {
            stack.push(new MapperQueuedNodeFrame(node, this));
        }
    }

    @NotNull
    private final Class<T> mappingResultType;
    @NotNull
    private final Set<String> transparentNodeNames;
    @NotNull
    private final Map<String, TreeMapperCallback<T, C>> translations;
    @NotNull
    private final Stack<MapperFrame> stack = new Stack<>();
    @NotNull
    private final C context;

    public SQLQueryTreeMapper(
        @NotNull Class<T> mappingResultType,
        @NotNull Set<String> transparentNodeNames,
        @NotNull Map<String, TreeMapperCallback<T, C>> translations,
        @NotNull C context
    ) {
        this.mappingResultType = mappingResultType;
        this.transparentNodeNames = transparentNodeNames;
        this.translations = translations;
        this.context = context;
    }

    @NotNull
    public T translate(@NotNull STMTreeNode root) {
        MapperRootFrame rootFrame = new MapperRootFrame(root);
        stack.push(rootFrame);
        while (!stack.isEmpty()) {
            stack.pop().doWork();
        }
        return rootFrame.result;
    }
}
