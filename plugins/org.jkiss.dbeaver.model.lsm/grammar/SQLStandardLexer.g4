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
lexer grammar SQLStandardLexer;

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
    package org.jkiss.dbeaver.model.lsm.sql.impl.syntax;
}

// letters to support case-insensitivity for keywords
fragment A:[aA];
fragment B:[bB];
fragment C:[cC];
fragment D:[dD];
fragment E:[eE];
fragment F:[fF];
fragment G:[gG];
fragment H:[hH];
fragment I:[iI];
fragment J:[jJ];
fragment K:[kK];
fragment L:[lL];
fragment M:[mM];
fragment N:[nN];
fragment O:[oO];
fragment P:[pP];
fragment Q:[qQ];
fragment R:[rR];
fragment S:[sS];
fragment T:[tT];
fragment U:[uU];
fragment V:[vV];
fragment W:[wW];
fragment X:[xX];
fragment Y:[yY];
fragment Z:[zZ];


// keywords
ABSOLUTE: A B S O L U T E ;
ACTION: A C T I O N ;
ADA: A D A;
ADD: A D D ;
ALL: A L L ;
ALLOCATE: A L L O C A T E ;
ALTER: A L T E R ;
AND: A N D ;
ANY: A N Y ;
ARE: A R E ;
AS: A S ;
ASC: A S C ;
ASSERTION: A S S E R T I O N ;
AT: A T ;
AUTHORIZATION: A U T H O R I Z A T I O N ;
AVG: A V G ;
BETWEEN: B E T W E E N ;
BIT: B I T ;
BIT_LENGTH: B I T '_'L E N G T H ;
BOTH: B O T H ;
BY: B Y ;
CASCADE: C A S C A D E ;
CASCADED: C A S C A D E D ;
CASE: C A S E ;
CAST: C A S T ;
CATALOG: C A T A L O G ;
CATALOG_NAME: C A T A L O G '_'N A M E ;
CHAR: C H A R ;
CHARACTER: C H A R A C T E R ;
CHARACTER_LENGTH: C H A R A C T E R '_'L E N G T H ;
CHARACTER_SET_CATALOG: C H A R A C T E R '_'S E T '_'C A T A L O G ;
CHARACTER_SET_NAME: C H A R A C T E R '_'S E T '_'N A M E ;
CHARACTER_SET_SCHEMA: C H A R A C T E R '_'S E T '_'S C H E M A ;
CHAR_LENGTH: C H A R '_'L E N G T H ;
CHECK: C H E C K ;
CLASS_ORIGIN: C L A S S '_'O R I G I N ;
CLOSE: C L O S E ;
COALESCE: C O A L E S C E ;
COBOL: C O B O L;
COLLATE: C O L L A T E ;
COLLATION: C O L L A T I O N ;
COLLATION_CATALOG: C O L L A T I O N '_'C A T A L O G ;
COLLATION_NAME: C O L L A T I O N '_'N A M E ;
COLLATION_SCHEMA: C O L L A T I O N '_'S C H E M A ;
COLUMN: C O L U M N ;
COLUMN_NAME: C O L U M N '_'N A M E ;
COMMAND_FUNCTION: C O M M A N D '_'F U N C T I O N ;
COMMIT: C O M M I T ;
COMMITTED: C O M M I T T E D ;
CONDITION_NUMBER: C O N D I T I O N '_'N U M B E R ;
CONNECT: C O N N E C T ;
CONNECTION: C O N N E C T I O N ;
CONNECTION_NAME: C O N N E C T I O N '_'N A M E ;
CONSTRAINT: C O N S T R A I N T ;
CONSTRAINTS: C O N S T R A I N T S ;
CONSTRAINT_CATALOG: C O N S T R A I N T '_'C A T A L O G ;
CONSTRAINT_NAME: C O N S T R A I N T '_'N A M E ;
CONSTRAINT_SCHEMA: C O N S T R A I N T '_'S C H E M A ;
CONVERT: C O N V E R T ;
CORRESPONDING: C O R R E S P O N D I N G ;
COUNT: C O U N T ;
CREATE: C R E A T E ;
CROSS: C R O S S ;
CURRENT: C U R R E N T ;
CURRENT_DATE: C U R R E N T '_'D A T E ;
CURRENT_TIME: C U R R E N T '_'T I M E ;
CURRENT_TIMESTAMP: C U R R E N T '_'T I M E S T A M P ;
CURRENT_USER: C U R R E N T '_'U S E R ;
CURSOR: C U R S O R ;
CURSOR_NAME: C U R S O R '_'N A M E ;
DATA: D A T A ;
DATE: D A T E ;
DATETIME_INTERVAL_CODE: D A T E T I M E '_'I N T E R V A L '_'C O D E ;
DATETIME_INTERVAL_PRECISION: D A T E T I M E '_'I N T E R V A L '_'P R E C I S I O N ;
DAY: D A Y ;
DEALLOCATE: D E A L L O C A T E ;
DEC: D E C ;
DECIMAL: D E C I M A L ;
DECLARE: D E C L A R E ;
DEFAULT: D E F A U L T ;
DEFERRABLE: D E F E R R A B L E ;
DEFERRED: D E F E R R E D ;
DELETE: D E L E T E ;
DESC: D E S C ;
DESCRIBE: D E S C R I B E ;
DESCRIPTOR: D E S C R I P T O R ;
DIAGNOSTICS: D I A G N O S T I C S ;
DISCONNECT: D I S C O N N E C T ;
DISTINCT: D I S T I N C T ;
DOMAIN: D O M A I N ;
DOUBLE: D O U B L E ;
DROP: D R O P ;
DYNAMIC_FUNCTION: D Y N A M I C '_'F U N C T I O N ;
ELSE: E L S E ;
END: E N D ;
ESCAPE: E S C A P E ;
EXCEPT: E X C E P T ;
EXCEPTION: E X C E P T I O N ;
EXECUTE: E X E C U T E ;
EXISTS: E X I S T S ;
EXTERNAL: E X T E R N A L ;
EXTRACT: E X T R A C T ;
FALSE: F A L S E ;
FETCH: F E T C H ;
FIRST: F I R S T ;
FLOAT: F L O A T ;
FOR: F O R ;
FOREIGN: F O R E I G N ;
FORTRAN: F O R T R A N;
FROM: F R O M ;
FULL: F U L L ;
GET: G E T ;
GLOBAL: G L O B A L ;
GRANT: G R A N T ;
GROUP: G R O U P ;
HAVING: H A V I N G ;
HOUR: H O U R ;
IDENTITY: I D E N T I T Y ;
IMMEDIATE: I M M E D I A T E ;
IN: I N ;
INDICATOR: I N D I C A T O R ;
INITIALLY: I N I T I A L L Y ;
INNER: I N N E R ;
INPUT: I N P U T ;
INSENSITIVE: I N S E N S I T I V E ;
INSERT: I N S E R T ;
INT: I N T ;
INTEGER: I N T E G E R ;
INTERSECT: I N T E R S E C T ;
INTERVAL: I N T E R V A L ;
INTO: I N T O ;
IS: I S ;
ISOLATION: I S O L A T I O N ;
JOIN: J O I N ;
KEY: K E Y ;
LANGUAGE: L A N G U A G E ;
LAST: L A S T ;
LEADING: L E A D I N G ;
LEFT: L E F T ;
LENGTH: L E N G T H ;
LEVEL: L E V E L ;
LIKE: L I K E ;
LOCAL: L O C A L ;
LOWER: L O W E R ;
MATCH: M A T C H ;
MAX: M A X ;
MESSAGE_LENGTH: M E S S A G E '_'L E N G T H ;
MESSAGE_OCTET_LENGTH: M E S S A G E '_'O C T E T '_'L E N G T H ;
MESSAGE_TEXT: M E S S A G E '_'T E X T ;
MIN: M I N ;
MINUTE: M I N U T E ;
MODULE: M O D U L E ;
MONTH: M O N T H ;
MUMPS: M U M P S;
MORE_KW: M O R E;
NAME: N A M E ;
NAMES: N A M E S ;
NATIONAL: N A T I O N A L ;
NATURAL: N A T U R A L ;
NCHAR: N C H A R ;
NEXT: N E X T ;
NO: N O ;
NOT: N O T ;
NULL: N U L L ;
NULLABLE: N U L L A B L E ;
NULLIF: N U L L I F ;
NUMBER: N U M B E R ;
NUMERIC: N U M E R I C ;
OCTET_LENGTH: O C T E T '_'L E N G T H ;
OF: O F ;
ON: O N ;
ONLY: O N L Y ;
OPEN: O P E N ;
OPTION: O P T I O N ;
OR: O R ;
ORDER: O R D E R ;
OUTER: O U T E R ;
OUTPUT: O U T P U T ;
OVERLAPS: O V E R L A P S ;
PAD: P A D ;
PASCAL: P A S C A L ;
PARTIAL: P A R T I A L ;
POSITION: P O S I T I O N ;
PLI: P L I ;
PRECISION: P R E C I S I O N ;
PREPARE: P R E P A R E ;
PRESERVE: P R E S E R V E ;
PRIMARY: P R I M A R Y ;
PRIOR: P R I O R ;
PRIVILEGES: P R I V I L E G E S ;
PROCEDURE: P R O C E D U R E ;
PUBLIC: P U B L I C ;
READ: R E A D ;
REAL: R E A L ;
REFERENCES: R E F E R E N C E S ;
RELATIVE: R E L A T I V E ;
REPEATABLE: R E P E A T A B L E ;
RESTRICT: R E S T R I C T ;
RETURNED_LENGTH: R E T U R N E D '_'L E N G T H ;
RETURNED_OCTET_LENGTH: R E T U R N E D '_'O C T E T '_'L E N G T H ;
RETURNED_SQLSTATE: R E T U R N E D '_'S Q L S T A T E ;
REVOKE: R E V O K E ;
RIGHT: R I G H T ;
ROLLBACK: R O L L B A C K ;
ROWS: R O W S ;
ROW_COUNT: R O W '_'C O U N T ;
SCALE: S C A L E ;
SCHEMA: S C H E M A ;
SCHEMA_NAME: S C H E M A '_'N A M E ;
SCROLL: S C R O L L ;
SECOND: S E C O N D ;
SELECT: S E L E C T ;
SERIALIZABLE: S E R I A L I Z A B L E ;
SERVER_NAME: S E R V E R '_'N A M E ;
SESSION: S E S S I O N ;
SESSION_USER: S E S S I O N '_'U S E R ;
SET: S E T ;
SIZE: S I Z E ;
SMALLINT: S M A L L I N T ;
SOME: S O M E ;
SPACE: S P A C E ;
SQL: S Q L ;
SQLCODE: S Q L C O D E ;
SQLSTATE: S Q L S T A T E ;
SUBCLASS_ORIGIN: S U B C L A S S '_'O R I G I N ;
SUBSTRING: S U B S T R I N G ;
SUM: S U M ;
SYSTEM_USER: S Y S T E M '_'U S E R ;
TABLE: T A B L E ;
TABLE_NAME: T A B L E '_'N A M E ;
TEMPORARY: T E M P O R A R Y ;
THEN: T H E N ;
TIME: T I M E ;
TIMESTAMP: T I M E S T A M P ;
TIMEZONE_HOUR: T I M E Z O N E '_'H O U R ;
TIMEZONE_MINUTE: T I M E Z O N E '_'M I N U T E ;
TO: T O ;
TRAILING: T R A I L I N G ;
TRANSACTION: T R A N S A C T I O N ;
TRANSLATE: T R A N S L A T E ;
TRANSLATION: T R A N S L A T I O N ;
TRIM: T R I M ;
TRUE: T R U E ;
TYPE: T Y P E ;
UNCOMMITTED: U N C O M M I T T E D ;
UNION: U N I O N ;
UNIQUE: U N I Q U E ;
UNKNOWN: U N K N O W N ;
UNNAMED: U N N A M E D ;
UPDATE: U P D A T E ;
UPPER: U P P E R ;
USAGE: U S A G E ;
USER: U S E R ;
USING: U S I N G ;
VALUE: V A L U E ;
VALUES: V A L U E S ;
VARCHAR: V A R C H A R ;
VARYING: V A R Y I N G ;
VIEW: V I E W ;
WHEN: W H E N ;
WHERE: W H E R E ;
WITH: W I T H ;
WORK: W O R K ;
WRITE: W R I T E ;
YEAR: Y E A R ;
ZONE: Z O N E ;


// symbols
EqualsOperator: '=';
NotEqualsOperator: '<>';
RightParen: ')';
LeftParen: '(';
SingleQuote: '\'';
BackQuote: '`';
Comma: ',';
Colon: ':';
Semicolon: ';';
Ampersand: '&';
Asterisk: '*';
Solidus: '/';
ConcatenationOperator: '||';
Percent: '%';
Period: '.';
DoublePeriod: '..';
DoubleQuote: '"';
GreaterThanOperator: '>';
GreaterThanOrEqualsOperator: '>=';
LessThanOperator: '<';
LessThanOrEqualsOperator: '<=';
LeftBracket: '[';
RightBracket: ']';
MinusSign: '-';
PlusSign: '+';
QuestionMark: '?';
Underscore: '_';
VerticalBar: '|';


// characters
fragment SimpleLatinLetter: (SimpleLatinUpperCaseLetter|SimpleLatinLowerCaseLetter);
fragment SimpleLatinUpperCaseLetter: ('A'|'B'|'C'|'D'|'E'|'F'|'G'|'H'|'I'|'J'|'K'|'L'|'M'|'N'|'O'|'P'|'Q'|'R'|'S'|'T'|'U'|'V'|'W'|'X'|'Y'|'Z');
fragment SimpleLatinLowerCaseLetter: ('a'|'b'|'c'|'d'|'e'|'f'|'g'|'h'|'i'|'j'|'k'|'l'|'m'|'n'|'o'|'p'|'q'|'r'|'s'|'t'|'u'|'v'|'w'|'x'|'y'|'z');


// numeric fragments
fragment Digit: ('0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9');
fragment Hexit: (Digit|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f');
fragment Bit: ('0'|'1');


// numeric literals
DecimalLiteral: (UnsignedInteger Period UnsignedInteger)|(UnsignedInteger Period)|(Period UnsignedInteger);
UnsignedInteger: (Digit)+;
ApproximateNumericLiteral: (UnsignedInteger|DecimalLiteral) 'E' SignedInteger;
fragment SignedInteger: (Sign)? UnsignedInteger;
Sign: (PlusSign|MinusSign);


LineComment
   : '--' ~ [\r\n]* -> channel (HIDDEN)
   ;

// special characters and character sequences
fragment NonquoteCharacter: ~'~';
QuoteSymbol: SingleQuote SingleQuote;
Introducer: Underscore;
fragment NewLine: ([\r][\n])|[\n]|[\r];
Separator: (NewLine|Space)+ -> channel(HIDDEN);
Space: [ \t]+;


// identifiers
DelimitedIdentifier: IdentifierQuote DelimitedIdentifierBody IdentifierQuote;
fragment IdentifierQuote: (DoubleQuote|BackQuote);
fragment DelimitedIdentifierBody: (DelimitedIdentifierPart)+;
fragment DelimitedIdentifierPart: (NondoublequoteCharacter|DoublequoteSymbol);
fragment NondoublequoteCharacter: ~[`"];
fragment DoublequoteSymbol: IdentifierQuote IdentifierQuote;

Identifier: IdentifierBody;
fragment IdentifierBody: IdentifierStart ((Underscore|IdentifierPart)+)?;
fragment IdentifierStart: SimpleLatinLetter;
fragment IdentifierPart: (IdentifierStart|Digit);



// literals
NationalCharacterStringLiteral: 'N' SingleQuote ((CharacterRepresentation)+)? SingleQuote (((Separator)+ SingleQuote ((CharacterRepresentation)+)? SingleQuote)+)?;
CharacterRepresentation: (NonquoteCharacter|QuoteSymbol);
BitStringLiteral: 'B' SingleQuote ((Bit)+)? SingleQuote (((Separator)+ SingleQuote ((Bit)+)? SingleQuote)+)?;
HexStringLiteral: 'X' SingleQuote ((Hexit)+)? SingleQuote (((Separator)+ SingleQuote ((Hexit)+)? SingleQuote)+)?;

StringLiteralContent: SingleQuote ((CharacterRepresentation)+)? SingleQuote (((Separator)+ SingleQuote ((CharacterRepresentation)+)? SingleQuote)+)?;

C_: C;
WS: Separator;
