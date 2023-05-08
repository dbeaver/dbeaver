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
package org.jkiss.dbeaver.model.lsm.sql.impl;

import java.util.List;

import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxLiteral;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxTerm;

@SyntaxNode(name = "orderByClause")
public class OrderingSpec extends AbstractSyntaxNode {
    
    @SyntaxNode(name = "sortSpecification")
    public static class SortSpec extends AbstractSyntaxNode {
        @SyntaxSubnode(xpath = "./sortKey/columnReference")
        public ColumnReference columnName;
        @SyntaxTerm(xpath = "./sortKey/UnsignedInteger")
        public int columnNumber;
        @SyntaxTerm(xpath = "./orderingSpecification")
        public OrderKind ordering;
        @SyntaxTerm(xpath = "./collateClause/collationName/qualifiedName/qualifiedIdentifier/identifier/actualIdentifier")
        public String collation;
    }
    
    @SyntaxLiteral(name = "orderingSpecification")
    public enum OrderKind {
        ASC,
        DESC
    }
    
    @SyntaxSubnode(type = OrderingSpec.SortSpec.class, xpath = "./sortSpecificationList/sortSpecification")
    public List<SortSpec> sorting;
    
    

}
