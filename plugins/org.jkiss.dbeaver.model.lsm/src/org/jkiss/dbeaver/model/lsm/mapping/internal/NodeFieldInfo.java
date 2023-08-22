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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnodeLookupMode;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathExpression;

public class NodeFieldInfo {

    public static class SubnodeInfo {
        public final XPathExpression scopeExpr;
        public final Class<? extends AbstractSyntaxNode> subnodeType;
        public final SyntaxSubnodeLookupMode lookupMode;
        
        private NodeTypeInfo nodeTypeInfo;
        
        public SubnodeInfo(
            @NotNull XPathExpression scopeExpr,
            @NotNull Class<? extends AbstractSyntaxNode> subnodeType,
            @NotNull SyntaxSubnodeLookupMode lookupMode
        ) {
            this.scopeExpr = scopeExpr;
            this.subnodeType = subnodeType;
            this.lookupMode = lookupMode;
        }

        public void fixup(@NotNull SyntaxModel syntaxModel) {
            nodeTypeInfo = syntaxModel.findNodeTypeInfo(subnodeType);
        }

        @NotNull
        public NodeTypeInfo getNodeTypeInfo() {
            return nodeTypeInfo;
        }
    }
    
    public final FieldTypeKind kind;
    private final Field info;
    public final List<XPathExpression> termExprs;
    public final List<SubnodeInfo> subnodesInfo;
    
    private LiteralTypeInfo literalTypeInfo;
    
    public NodeFieldInfo(
        @NotNull FieldTypeKind kind,
        @NotNull Field info,
        @NotNull List<XPathExpression> termExprs,
        @NotNull List<SubnodeInfo> subnodesInfo
    ) {
        this.kind = kind;
        this.info = info;
        this.termExprs = Collections.unmodifiableList(termExprs);
        this.subnodesInfo = Collections.unmodifiableList(subnodesInfo);
    }

    @NotNull
    public String getFieldName() {
        return this.info.getName();
    }

    @NotNull
    public String getDeclaringClassName() {
        return this.info.getDeclaringClass().getName();
    }

    public void fixup(@NotNull SyntaxModel syntaxModel) {
        if (kind == FieldTypeKind.Enum) {
            literalTypeInfo = syntaxModel.findLiteralTypeInfo(info.getType());
        } else {
            literalTypeInfo = null;
        }
        
        for (SubnodeInfo subnodeInfo : subnodesInfo) {
            subnodeInfo.fixup(syntaxModel);
        }   
    }

    public LiteralTypeInfo getLiteralTypeInfo() {
        if (literalTypeInfo == null) {
            throw new IllegalStateException();
        } else {
            return literalTypeInfo;
        }
    }

    public Object getValue(AbstractSyntaxNode model) throws IllegalArgumentException, IllegalAccessException {
        return this.info.get(model);
    }

    public void setValue(AbstractSyntaxNode model, Object value) throws IllegalArgumentException, IllegalAccessException {
        this.info.set(model, value);
    }
}
