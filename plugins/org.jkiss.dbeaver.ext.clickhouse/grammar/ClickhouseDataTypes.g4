/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
 : anyType EOF
 ;

anyType
 : enumType
 | tupleType
 | mapType
 | arrayType
 | simpleType
 | markerType
 ;

markerType
 : 'Nullable' LeftParen anyType RightParen
 | 'LowCardinality' LeftParen anyType RightParen
 ;

simpleType
 : intType
 | floatType
 | decimalType
 | stringType
 | uuidType
 | boolType
 | ipv4Type
 | ipv6Type
 ;

enumType: ENUM_KW LeftParen enumEntryList RightParen;
enumEntryList: enumEntry (Comma enumEntry)*;
enumEntry: String Eq Number;

tupleType: TUPLE_KV LeftParen tupleElementList RightParen;
tupleElementList: tupleElement (Comma tupleElement)*;
tupleElement: anyType; // TODO support named elements (Name? type)

arrayType: ARRAY_KV LeftParen anyType RightParen;
mapType: MAP_KV LeftParen key=anyType Comma value=anyType RightParen;

stringType: 'String';
uuidType: 'UUID';
boolType: 'Boolean';
intType: unsigned='U'? 'Int' bits=('8' | '16' | '32' | '64' | '128' | '256');
floatType: 'Float' bits=('32' | '64');
ipv4Type: 'IPV4';
ipv6Type: 'IPV6';
decimalType
 : 'Decimal' bits=('32' | '64' | '128' | '256') LeftParen Comma scale=Number RightParen
 | 'Decimal' LeftParen precision=Number Comma scale=Number RightParen
 ;
