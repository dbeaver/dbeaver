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

import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxTerm;
import org.jkiss.dbeaver.model.lsm.sql.LSMSelectStatement;

import java.util.List;

@SyntaxNode(name = "selectStatement")
public class SelectStatement extends AbstractSyntaxNode implements LSMSelectStatement, LSMElement {

    private static final String nonJoinSimpleQuerySpecPath = "./queryExpression/nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification";
    
    @SyntaxTerm(xpath = nonJoinSimpleQuerySpecPath + "/setQuantifier")
    public SelectQuantifier quantifier;
    
    @SyntaxSubnode(type = SelectionItem.class, xpath = nonJoinSimpleQuerySpecPath + "/selectList/selectSublist")
    public List<SelectionItem> columns;
    
    @SyntaxSubnode(type = SelectionSource.Table.class, xpath = nonJoinSimpleQuerySpecPath + "/tableExpression/fromClause/tableReference/nonjoinedTableReference")
    @SyntaxSubnode(type = SelectionSource.Table.class, xpath = nonJoinSimpleQuerySpecPath + "/tableExpression/fromClause/tableReference/joinedTable/nonjoinedTableReference")
    @SyntaxSubnode(type = SelectionSource.CrossJoin.class, xpath = nonJoinSimpleQuerySpecPath + "/tableExpression/fromClause/tableReference/joinedTable/crossJoinTerm")
    @SyntaxSubnode(type = SelectionSource.NaturalJoin.class, xpath = nonJoinSimpleQuerySpecPath + "/tableExpression/fromClause/tableReference/joinedTable/naturalJoinTerm")
    public List<SelectionSource> sources;

    @SyntaxSubnode(type = OrderingSpec.class, xpath = "./orderByClause")
    public OrderingSpec orderBy;
    @SyntaxSubnode(type = GroupingSpec.class, xpath = nonJoinSimpleQuerySpecPath + "/tableExpression/groupByClause")
    public GroupingSpec groupBy;
}
