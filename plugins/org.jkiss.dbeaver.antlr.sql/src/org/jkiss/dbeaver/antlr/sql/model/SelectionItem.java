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
package org.jkiss.dbeaver.antlr.sql.model;

import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxTerm;

@SyntaxNode(name = "selectSublist")
public class SelectionItem extends AbstractSyntaxNode {
    
    @SyntaxTerm(xpath = ".//columnReference//catalogName/identifier")
    public String catalogName;
    @SyntaxTerm(xpath = ".//columnReference//schemaName/unqualifiedSchemaName/identifier")
    public String schemaName;
    @SyntaxTerm(xpath = ".//columnReference//tableName/qualifiedName/qualifiedIdentifier/identifier")
    public String tableName;
    @SyntaxTerm(xpath = ".//columnReference//columnName/identifier/actualIdentifier")
    public String columnName;
    // TODO:
    public ValueExpression expression;
    
    @SyntaxTerm(xpath = "./derivedColumn/asClause/columnName/identifier/actualIdentifier")
    public String alias;

}

