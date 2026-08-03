/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
grammar DataBricksDataTypes;

@header {
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databricks;
}

Whitespace: [ \t]+ -> channel(HIDDEN);
Dec: [0-9]+;
Name: [a-zA-Z_][a-zA-Z0-9_]*;
String: [rR]? '\'' (~'\'')* '\'';

bigIntType: 'BIGINT'|'LONG';
binaryType: 'BINARY';
dateType: 'DATE';
doubleType: 'DOUBLE';
floatType: 'FLOAT'|'REAL';
intType: 'INT'|'INTEGER';
smallintType: 'SMALLINT'|'SHORT';
stringType: 'STRING';
timestampType: 'TIMESTAMP'|'TIMESTAMP_LTZ';
timestampNtzType: 'TIMESTAMP_NTZ';
tinyIntType: 'TINYINT'|'BYTE';
variantType: 'VARIANT';
voidType: 'NULL'|'VOID';

decimalType: ('DECIMAL'|'DEC'|'NUMERIC') ('(' Dec (',' Dec)? ')')?;

intervalType: intervalYearMonthQualifier|intervalDayTimeQualifier;
intervalYearMonthQualifier: ('YEAR' ('TO' 'MONTH')?) | 'MONTH';
intervalDayTimeQualifier: (
        'DAY' ('TO' ('HOUR'|'MINUTE'|'SECOND'))?
    ) | (
        'HOUR' ('TO' ('MINUTE'|'SECOND') )?
    ) | (
        'MINUTE' ('TO' 'SECOND')?
    ) | (
        'SECOND'
    );

timeType: 'TIME' ('(' Dec ')')?;
geographyType: 'GEOGRAPHY' '(' (Dec|'ANY') ')';
geometryType: 'GEOMETRY' '(' (Dec|'ANY') ')';
arrayType: 'ARRAY' '<' anyType '>';
mapType: 'MAP' '<' nonMapType ',' anyType '>';

structType: 'STRUCT' '<' (structTypeField (',' structTypeField)*)? '>';
structTypeField: Name ':'? anyType ('NOT' 'NULL')? ('COLLATE' Name)? (COMMENT String)? ;

objectType: 'OBJECT' '<' (objectTypeField (',' objectTypeField)*)? '>';
objectTypeField: Name ':'? anyType ;

sridBasedType: geographyType
        | geometryType
        ;

trivialType: bigIntType
        | binaryType
        | dateType
        | doubleType
        | floatType
        | intType
        | smallintType
        | stringType
        | timestampType
        | timestampNtzType
        | tinyIntType
        | variantType
        | voidType
        ;

nonMapType: trivialType
        | sridBasedType
        | decimalType
        | intervalType
        | timeType
        | arrayType
        | mapType
        | structType
        | objectType
        ;

anyType: nonMapType
        | mapType
        ;
