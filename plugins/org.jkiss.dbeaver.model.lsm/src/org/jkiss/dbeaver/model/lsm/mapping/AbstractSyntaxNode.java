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

import org.antlr.v4.runtime.misc.InterpreterDataReader;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.SyntaxTree;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.mapping.internal.NodeFieldInfo;
import org.jkiss.dbeaver.model.lsm.mapping.internal.TreeRuleNode;
import org.jkiss.dbeaver.model.lsm.mapping.internal.XTreeNodeBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractSyntaxNode implements LSMElement {
    private static final Map<Class<? extends AbstractSyntaxNode>, String> syntaxNodeNameByType = new HashMap<>(
        Map.of(ConcreteSyntaxNode.class, "")
    );
    
    private static String getNodeName(Class<? extends AbstractSyntaxNode> type) {
        return syntaxNodeNameByType.computeIfAbsent(
            type,
            t -> Optional.ofNullable(t.getAnnotation(SyntaxNode.class).name()).orElse(t.getName())
        );
    }

    public static class BindingInfo {
        public final NodeFieldInfo field;
        public final Object value;
        public final SyntaxTree astNode;
        
        public BindingInfo(NodeFieldInfo field, Object value, SyntaxTree astNode) {
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
    
    protected AbstractSyntaxNode(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }

    public int getStartPosition() {
        return this.astNode != null ? this.astNode.getSourceInterval().a : UNDEFINED_POSITION;
    }
    
    public int getEndPosition() {
        return this.astNode != null ? this.astNode.getSourceInterval().b : UNDEFINED_POSITION;
    }
    
    void setAstNode(XTreeNodeBase astNode) {
        this.astNode = astNode;
        this.subnodeBindings = null;
    }

    XTreeNodeBase getAstNode() {
        return this.astNode;
    }

    void appendBinding(BindingInfo binding) {
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
}
