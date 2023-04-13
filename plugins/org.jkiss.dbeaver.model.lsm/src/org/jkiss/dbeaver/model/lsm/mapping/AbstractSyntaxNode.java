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
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.mapping.internal.TreeRuleNode;

import java.util.HashMap;
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
    
    public static final int UNDEFINED_POSITION = -1;
    
    // TODO consider revising
    private final String name;
    private int startPos = UNDEFINED_POSITION;
    private int endPos = UNDEFINED_POSITION;
    
    private TreeRuleNode astNode; // TODO support this
    // private List<Pair<XTreeNode, NodeFieldInfo>> fieldsMapping;
    
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
        return this.startPos;
    }
    
    public int getEndPosition() {
        return this.endPos;
    }
    
    void setStartPosition(int value) {
        this.startPos = value;
    }

    void setEndPosition(int value) {
        this.endPos = value;
    }
}
