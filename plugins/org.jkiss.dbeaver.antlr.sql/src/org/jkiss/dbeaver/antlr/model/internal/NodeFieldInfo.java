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
package org.jkiss.dbeaver.antlr.model.internal;

import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxSubnodeLookupMode;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathExpression;

public class NodeFieldInfo {

    public static class SubnodeInfo {
        public final XPathExpression scopeExpr;
        public final Class<? extends AbstractSyntaxNode> subnodeType;
        public final SyntaxSubnodeLookupMode lookupMode;
        
        public SubnodeInfo(XPathExpression scopeExpr, Class<? extends AbstractSyntaxNode> subnodeType, SyntaxSubnodeLookupMode lookupMode) {
            this.scopeExpr = scopeExpr;
            this.subnodeType = subnodeType;
            this.lookupMode = lookupMode;
        }
    }
    
    public final FieldTypeKind kind;
    public final Field info;
    public final List<XPathExpression> termExprs;
    public final List<SubnodeInfo> subnodesInfo;
    
    public NodeFieldInfo(FieldTypeKind kind, Field info, List<XPathExpression> termExprs, List<SubnodeInfo> subnodesInfo) {
        this.kind = kind;
        this.info = info;
        this.termExprs = Collections.unmodifiableList(termExprs);
        this.subnodesInfo = Collections.unmodifiableList(subnodesInfo);
    }
    
    public String getName() {
        return this.info.getName();
    }
}
