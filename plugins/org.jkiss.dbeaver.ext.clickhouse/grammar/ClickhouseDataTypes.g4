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
grammar ClickhouseDataTypes;

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
    package org.jkiss.dbeaver.ext.clickhouse;
}

// letters to support case-insensitivity for keywords
//fragment A:[aA];
//fragment B:[bB];
//fragment C:[cC];
//fragment D:[dD];
fragment E:[eE];
//fragment F:[fF];
//fragment G:[gG];
//fragment H:[hH];
//fragment I:[iI];
//fragment J:[jJ];
//fragment K:[kK];
//fragment L:[lL];
fragment M:[mM];
fragment N:[nN];
//fragment O:[oO];
//fragment P:[pP];
//fragment Q:[qQ];
//fragment R:[rR];
//fragment S:[sS];
//fragment T:[tT];
fragment U:[uU];
//fragment V:[vV];
//fragment W:[wW];
//fragment X:[xX];
//fragment Y:[yY];
//fragment Z:[zZ];

ENUM_KW: E N U M ('8'|'16');

RightParen: ')';
LeftParen: '(';

Space: [ \t]+ -> channel(HIDDEN);
Number: [-]?[0-9]+;

Comma: ',';
Eq: '=';
String: '\'' (~('\''|'\r'|'\n'))+ '\'';

enumType: ENUM_KW LeftParen enumEntryList RightParen;
enumEntryList: enumEntry (Comma enumEntry)*;
enumEntry: String Eq Number; // FIXME: string does not support escape sequences
