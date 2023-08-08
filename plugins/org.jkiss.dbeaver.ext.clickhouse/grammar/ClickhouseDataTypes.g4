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

ENUM_KW: 'Enum' ('8'|'16');
MAP_KV: 'Map';
ARRAY_KV: 'Array';
TUPLE_KV: 'Tuple';

RightParen: ')';
LeftParen: '(';

Space: [ \t]+ -> channel(HIDDEN);
Number: [-]?[0-9]+;

Comma: ',';
Eq: '=';
String: '\'' (~('\''|'\r'|'\n'))+ '\''; // FIXME: string does not support escape sequences

type
 : simpleType
 | enumType
 | tupleType
 | mapType
 | arrayType;

simpleType
 : intType
 | stringType
 ;

enumType: ENUM_KW LeftParen enumEntryList RightParen;
enumEntryList: enumEntry (Comma enumEntry)*;
enumEntry: String Eq Number;

tupleType: TUPLE_KV LeftParen tupleElementList RightParen;
tupleElementList: tupleElement (Comma tupleElement)*;
tupleElement: type; // TODO support named elements (Name? type)

arrayType: ARRAY_KV LeftParen type RightParen;
mapType: MAP_KV LeftParen key=type Comma value=type RightParen;

intType: 'U'? 'Int' ('8' | '16' | '32' | '64' | '128' | '256');
stringType: 'String';