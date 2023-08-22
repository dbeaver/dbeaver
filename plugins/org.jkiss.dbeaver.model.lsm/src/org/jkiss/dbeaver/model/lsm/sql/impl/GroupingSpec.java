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
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxTerm;

@SyntaxNode(name = "groupByClause")
public class GroupingSpec extends AbstractSyntaxNode{
    
    @SyntaxNode(name = "groupingColumnReference")
    public static class GroupingColumnSpec extends AbstractSyntaxNode {
        @SyntaxSubnode(xpath = "./columnReference")
        public ColumnReference column;
        @SyntaxTerm(xpath = "./collateClause/collationName/qualifiedName/qualifiedIdentifier/identifier/actualIdentifier")
        public String collation;
    }
    
    @SyntaxSubnode(type = GroupingColumnSpec.class, xpath = "./groupingColumnReferenceList/groupingColumnReference")
    public List<GroupingColumnSpec> columns;
}
