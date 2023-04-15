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

import org.jkiss.dbeaver.model.lsm.mapping.*;

import java.util.List;


public abstract class SelectionSource extends AbstractSyntaxNode {
    @SyntaxNode(name = "nonjoinedTableReference")
    public static class Table extends SelectionSource {
        @SyntaxTerm(xpath = "./tableName//qualifiedName/schemaName/catalogName/identifier")
        public String catalogName;
        @SyntaxTerm(xpath = "./tableName//qualifiedName/schemaName/unqualifiedSchemaName/identifier")
        public String schemaName;
        @SyntaxTerm(xpath = "./tableName/qualifiedName/qualifiedIdentifier/identifier")
        public String tableName;
        @SyntaxTerm(xpath = "./correlationSpecification/correlationName//actualIdentifier")
        public String alias;
        @SyntaxTerm(xpath = "./correlationSpecification/derivedColumnList/columnNameList/columnName//actualIdentifier")
        public List<String> columnNames;
    }
    
    @SyntaxNode(name = "crossJoinTerm")
    public static class CrossJoin extends SelectionSource {
        @SyntaxSubnode(xpath = "./tableReference/nonjoinedTableReference")
        public Table table;
    }

    @SyntaxLiteral(name = "joinType", xstring = "x:joinStrings('_', ./parent::*[text()[1]='NATURAL']/text()[1], x:flatten(., \"./*|./text()\", true(), false()))")
    public enum JoinKind {
        INNER,
        LEFT,
        RIGHT,
        FULL,
        LEFT_OUTER,
        RIGHT_OUTER,
        FULL_OUTER,
        UNION,

        NATURAL_INNER,
        NATURAL_LEFT,
        NATURAL_RIGHT,
        NATURAL_FULL,
        NATURAL_LEFT_OUTER,
        NATURAL_RIGHT_OUTER,
        NATURAL_FULL_OUTER,
        NATURAL_UNION
    }

    @SyntaxNode(name = "naturalJoinTerm")
    public static class NaturalJoin extends SelectionSource {
        @SyntaxTerm(xpath = "./joinType")
        public JoinKind kind;
        @SyntaxSubnode(xpath = "./tableReference/nonjoinedTableReference")
        public Table table;
//        @SyntaxSubnode(xpath = "./joinSpecification/searchCondition")
//        public ValueExpression condition;
        @SyntaxTerm(xpath = "./joinSpecification/namedColumnsJoin/joinColumnList/columnNameList/columnName/identifier/actualIdentifier")
        public List<String> columnNames;
    }
}
