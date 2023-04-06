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
grammar Sql92MySqlExtensionParser;

import Sql92Parser;

options {
    tokenVocab=Sql92MySqlExtensionLexer;
    superClass=org.jkiss.dbeaver.antlr.model.internal.ParserOverrides;
    contextSuperClass=org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode;
}

// See https://dev.mysql.com/doc/refman/8.0/en/extensions-to-ansi.html

querySpecification: SELECT STRAIGHT_JOIN?
    SQL_SMALL_RESULT? SQL_BIG_RESULT? SQL_BUFFER_RESULT?
    SQL_NO_CACHE? SQL_CALC_FOUND_ROWS?
    (setQuantifier)? selectList tableExpression;
