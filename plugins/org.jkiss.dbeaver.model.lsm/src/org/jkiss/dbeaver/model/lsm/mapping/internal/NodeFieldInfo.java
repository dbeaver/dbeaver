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

import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnodeLookupMode;

import javax.xml.xpath.XPathExpression;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class NodeFieldInfo {

    public static class SubnodeInfo {
        public final XPathExpression scopeExpr;
        public final Class<? extends AbstractSyntaxNode> subnodeType;
        public final SyntaxSubnodeLookupMode lookupMode;
        
        private NodeTypeInfo nodeTypeInfo;
        
        public SubnodeInfo(XPathExpression scopeExpr, 
                           Class<? extends AbstractSyntaxNode> subnodeType, 
                           SyntaxSubnodeLookupMode lookupMode) {
            this.scopeExpr = scopeExpr;
            this.subnodeType = subnodeType;
            this.lookupMode = lookupMode;
        }

        public void fixup(SyntaxModel syntaxModel) {
            nodeTypeInfo = syntaxModel.findNodeTypeInfo(subnodeType);
        }

        public NodeTypeInfo getNodeTypeInfo() {
            if (nodeTypeInfo == null) {
                throw new IllegalStateException();
            } else {
                return nodeTypeInfo;
            }
        }
    }
    
    public final FieldTypeKind kind;
    private final Field info;
    public final List<XPathExpression> termExprs;
    public final List<SubnodeInfo> subnodesInfo;
    
    private LiteralTypeInfo literalTypeInfo;
    
    public NodeFieldInfo(FieldTypeKind kind, Field info, List<XPathExpression> termExprs, List<SubnodeInfo> subnodesInfo) {
        this.kind = kind;
        this.info = info;
        this.termExprs = Collections.unmodifiableList(termExprs);
        this.subnodesInfo = Collections.unmodifiableList(subnodesInfo);
    }
    
    public String getFieldName() {
        return this.info.getName();
    }
    
    public String getDeclaringClassName() {
        return this.info.getDeclaringClass().getName();
    }

    public void fixup(SyntaxModel syntaxModel) {
        if (kind == FieldTypeKind.Enum) {
            literalTypeInfo = syntaxModel.findLiteralTypeInfo(info.getType());
        } else {
            literalTypeInfo = null;
        }
        
        for (SubnodeInfo subnodeInfo: subnodesInfo) {
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
