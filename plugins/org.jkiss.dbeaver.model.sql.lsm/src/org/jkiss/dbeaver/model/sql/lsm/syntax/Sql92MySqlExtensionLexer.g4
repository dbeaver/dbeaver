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
lexer grammar Sql92MySqlExtensionLexer;

import Sql92Lexer;

@header {
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
    package org.jkiss.dbeaver.model.sql.lsm.syntax;
}

STRAIGHT_JOIN: S T R A I G H T '_' J O I N;
SQL_SMALL_RESULT: S Q L '_' S M A L L '_' R E S U L T;
SQL_BIG_RESULT: S Q L '_' B I G '_' R E S U L T;
SQL_BUFFER_RESULT: S Q L '_' B U F F E R '_' R E S U L T;
SQL_NO_CACHE: S Q L '_' N O '_' C A C H E;
SQL_CALC_FOUND_ROWS: S Q L '_' C A L C '_' F O U N D '_' R O W S;

