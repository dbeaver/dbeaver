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
package org.jkiss.dbeaver.model.lsm.mapping;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.mapping.internal.NodeFieldInfo;
import org.jkiss.dbeaver.model.lsm.mapping.internal.XTreeNodeBase;

import java.util.*;
import java.util.function.Function;


public abstract class AbstractSyntaxNode implements LSMElement {
    private static final Map<Class<? extends AbstractSyntaxNode>, String> syntaxNodeNameByType = new HashMap<>(
        Map.of(ConcreteSyntaxNode.class, "")
    );

    @NotNull
    private static String getNodeName(@NotNull Class<? extends AbstractSyntaxNode> type) {
        return syntaxNodeNameByType.computeIfAbsent(
            type,
            t -> Optional.of(t.getAnnotation(SyntaxNode.class).name()).orElse(t.getName())
        );
    }

    static class BindingInfo {
        public final NodeFieldInfo field;
        public final Object value;
        public final XTreeNodeBase astNode;
        
        public BindingInfo(@NotNull NodeFieldInfo field, @Nullable Object value, @NotNull XTreeNodeBase astNode) {
            this.field = field;
            this.value = value;
            this.astNode = astNode;
        }
    }
    
    public static final int UNDEFINED_POSITION = -1;
    
    // TODO consider revising
    private final String name;
    
    private XTreeNodeBase astNode = null;
    private List<BindingInfo> subnodeBindings = null;
    
    protected AbstractSyntaxNode() {
        this.name = getNodeName(this.getClass());
    }
    
    protected AbstractSyntaxNode(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public int getStartPosition() {
        return this.astNode != null ? this.astNode.getRealInterval().a : UNDEFINED_POSITION;
    }
    
    public int getEndPosition() {
        return this.astNode != null ? this.astNode.getRealInterval().b : UNDEFINED_POSITION;
    }
    
    void setAstNode(@NotNull XTreeNodeBase astNode) {
        this.astNode = astNode;
        this.subnodeBindings = null;
    }

    @NotNull
    XTreeNodeBase getAstNode() {
        return this.astNode;
    }

    void appendBinding(@NotNull BindingInfo binding) {
        if (subnodeBindings == null) {
            this.subnodeBindings = new LinkedList<>();
        }
        this.subnodeBindings.add(binding);
    }
    
    private static final Comparator<BindingInfo> BINDING_MY_POS_COMPARER = (a, b) -> {
        Interval x = a.astNode.getSourceInterval();
        Interval y = b.astNode.getSourceInterval();
        int rc = Integer.compare(x.a, y.a);
        if (rc == 0) {
            rc = Integer.compare(x.b, y.b);
        }
        return rc;
    };

    @NotNull
    List<BindingInfo> getBindings() {
        if (this.subnodeBindings == null) {
            return Collections.emptyList();
        } else if (this.subnodeBindings instanceof LinkedList) {
            ArrayList<BindingInfo> bindings = new ArrayList<>(this.subnodeBindings);
            bindings.sort(BINDING_MY_POS_COMPARER);
            this.subnodeBindings = bindings;
        }
        return this.subnodeBindings;
    }
    
    public static class SyntaxModelLookupResult {
        public final AbstractSyntaxNode node;
        final BindingInfo binding;
        final XTreeNodeBase astNode;
        
        public SyntaxModelLookupResult(@NotNull AbstractSyntaxNode node, @Nullable BindingInfo binding) {
            this.node = node;
            this.binding = binding;
            this.astNode = binding == null ? node.astNode : binding.astNode;
        }

        @NotNull
        public Interval getInterval() {
            return astNode.getRealInterval();
        }

        @Nullable
        public String getEntityName() {
            return node.getName();
        }

        @Nullable
        public String getEntityFieldName() {
            return binding == null ? null : binding.field.getFieldName();
        }

        @NotNull
        public String getAstNodeFullName() {
            return astNode.getFullPathName();
        }

        @NotNull
        @Override
        public String toString() {
            String element = binding == null ? this.getEntityName() : (this.getEntityFieldName() + " of " + this.getEntityName());
            return "SyntaxModelLookupResult[" + element + " @" + this.getInterval() + "]"; 
        }
    }

    @Nullable
    public SyntaxModelLookupResult findBoundSyntaxAt(int position) {
        Interval location = new Interval(position, position);
        AbstractSyntaxNode node = this;
        if (node.astNode.getRealInterval().properlyContains(location)) {
            BindingInfo nodeBinding = findBindingOfLocation(node, location);
            while (nodeBinding != null && nodeBinding.value instanceof AbstractSyntaxNode) {
                node = (AbstractSyntaxNode) nodeBinding.value;
                nodeBinding = findBindingOfLocation(node, location);
            }
            return new SyntaxModelLookupResult(node, nodeBinding);
        } else {
            return null;
        }
    }

    @Nullable
    private static BindingInfo findBindingOfLocation(@NotNull AbstractSyntaxNode node, @NotNull Interval location) {
        List<BindingInfo> bindings = node.getBindings();
        int index = binarySearchByKey(bindings, b -> b.astNode.getRealInterval(), location, (x, y) ->  {
            if (x.b < y.a) {
                return -1;
            } else if (x.a > y.b) {
                return 1;
            } else {
                return 0;
            }
        });
        if (index < 0) {
            return null;
        }
        
        int resultIndex = index;
        BindingInfo result = bindings.get(index);
        Interval interval = result.astNode.getRealInterval();
        
        while (index > 0) {
            index--;
            BindingInfo prev = bindings.get(index);
            Interval prevInterval = prev.astNode.getRealInterval();
            if (prevInterval.properlyContains(location) && prevInterval.length() < interval.length()) {
                result = prev;
                interval = prevInterval;
            } else {
                break;
            }
        }
        if (resultIndex == index) {
            int lastIndex = bindings.size() - 1;
            while (index < lastIndex) {
                index++;
                BindingInfo next = bindings.get(index);
                Interval nextInterval = next.astNode.getRealInterval();
                if (nextInterval.properlyContains(location) && nextInterval.length() < interval.length()) {
                    result = next;
                    interval = nextInterval;
                } else {
                    break;
                }
            }
        }
        
        return result;
    }

    private static <T, K> int binarySearchByKey(
        @NotNull List<T> list,
        @NotNull Function<T, K> keyGetter,
        @NotNull K key,
        @NotNull Comparator<K> comparator
    ) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            K midVal = keyGetter.apply(list.get(mid));
            int cmp = comparator.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }
}

