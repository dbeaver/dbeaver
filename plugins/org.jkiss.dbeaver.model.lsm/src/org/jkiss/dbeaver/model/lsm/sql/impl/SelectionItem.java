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

import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxNode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxSubnode;
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxTerm;

@SyntaxNode(name = "selectSublist")
public class SelectionItem extends AbstractSyntaxNode {
    
    @SyntaxSubnode(xpath = ".//columnReference")
    public ColumnReference columnName;
    // TODO: create model for conditions
    public ValueExpression expression;
    
    @SyntaxTerm(xpath = "./derivedColumn/asClause/columnName/identifier/actualIdentifier")
    public String alias;

}

