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
package org.jkiss.dbeaver.antlr.example.sql.model;

import java.util.List;

import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxSubnode;

@SyntaxNode(name = "queryExpression")
public class SelectStatement extends AbstractSyntaxNode {

    // TODO:
    public SelectQuantifier quantifier;
    
    @SyntaxSubnode(type = SelectionItem.class, xpath = "./nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification/selectList/selectSublist")
    public List<SelectionItem> columns;
    
    @SyntaxSubnode(type = SelectionSource.Table.class, xpath = "./nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification/tableExpression/fromClause/tableReference/nonjoinedTableReference")
    @SyntaxSubnode(type = SelectionSource.Table.class, xpath = "./nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification/tableExpression/fromClause/tableReference/joinedTable/nonjoinedTableReference")                                                                        
    @SyntaxSubnode(type = SelectionSource.CrossJoin.class, xpath = "./nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification/tableExpression/fromClause/tableReference/joinedTable/crossJoinTerm")                                                                        
    @SyntaxSubnode(type = SelectionSource.NaturalJoin.class, xpath = "./nonJoinQueryTerm/queryPrimary/nonJoinQueryPrimary/simpleTable/querySpecification/tableExpression/fromClause/tableReference/joinedTable/naturalJoinTerm")                                                                        
    public List<SelectionSource> sources;
}
