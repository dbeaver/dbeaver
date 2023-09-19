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
ACTION: A C T I O N ;
ADD: A D D ;
ALL: A L L ;
ALTER: A L T E R ;
AND: A N D ;
ANY: A N Y ;
AS: A S ;
ASC: A S C ;
AUTHORIZATION: A U T H O R I Z A T I O N ;
BETWEEN: B E T W E E N ;
BY: B Y ;
CASCADE: C A S C A D E ;
CASCADED: C A S C A D E D ;
CASE: C A S E ;
CAST: C A S T ;
CATALOG: C A T A L O G ;
CHARACTER: C H A R A C T E R ;
CHECK: C H E C K ;
COALESCE: C O A L E S C E ;
COLUMN: C O L U M N ;
COMMIT: C O M M I T ;
COMMITTED: C O M M I T T E D ;
CONSTRAINT: C O N S T R A I N T ;
CONSTRAINTS: C O N S T R A I N T S ;
CORRESPONDING: C O R R E S P O N D I N G ;
COUNT: C O U N T ;
CREATE: C R E A T E ;
CROSS: C R O S S ;
CURRENT_USER: C U R R E N T '_'U S E R ;
DATE: D A T E ;
DAY: D A Y ;
DEFAULT: D E F A U L T ;
DEFERRABLE: D E F E R R A B L E ;
DEFERRED: D E F E R R E D ;
DELETE: D E L E T E ;
DESC: D E S C ;
DISTINCT: D I S T I N C T ;
DROP: D R O P ;
ELSE: E L S E ;
END: E N D ;
ESCAPE: E S C A P E ;
EXCEPT: E X C E P T ;
EXISTS: E X I S T S ;
EXTRACT: E X T R A C T ;
FALSE: F A L S E ;
FOREIGN: F O R E I G N ;
FROM: F R O M ;
FULL: F U L L ;
GLOBAL: G L O B A L ;
GROUP: G R O U P ;
HAVING: H A V I N G ;
HOUR: H O U R ;
IMMEDIATE: I M M E D I A T E ;
IN: I N ;
INDICATOR: I N D I C A T O R ;
INITIALLY: I N I T I A L L Y ;
INNER: I N N E R ;
INSERT: I N S E R T ;
INTERSECT: I N T E R S E C T ;
INTERVAL: I N T E R V A L ;
INTO: I N T O ;
IS: I S ;
ISOLATION: I S O L A T I O N ;
JOIN: J O I N ;
KEY: K E Y ;
LEFT: L E F T ;
LEVEL: L E V E L ;
LIKE: L I K E ;
LOCAL: L O C A L ;
MATCH: M A T C H ;
MINUTE: M I N U T E ;
MONTH: M O N T H ;
NAMES: N A M E S ;
NATURAL: N A T U R A L ;
NO: N O ;
NOT: N O T ;
NOTNULL: N O T N U L L;
NULL: N U L L ;
NULLIF: N U L L I F ;
ON: O N ;
ONLY: O N L Y ;
OPTION: O P T I O N ;
OR: O R ;
ORDER: O R D E R ;
OUTER: O U T E R ;
OVERLAPS: O V E R L A P S ;
PARTIAL: P A R T I A L ;
PARTITION: P A R T I T I O N ;
PRESERVE: P R E S E R V E ;
PRIMARY: P R I M A R Y ;
READ: R E A D ;
RECURSIVE: R E C U R S I V E;
REFERENCES: R E F E R E N C E S ;
REPEATABLE: R E P E A T A B L E ;
RESTRICT: R E S T R I C T ;
RIGHT: R I G H T ;
ROLLBACK: R O L L B A C K ;
ROWS: R O W S ;
SCHEMA: S C H E M A ;
SECOND: S E C O N D ;
SELECT: S E L E C T ;
SERIALIZABLE: S E R I A L I Z A B L E ;
SESSION: S E S S I O N ;
SESSION_USER: S E S S I O N '_'U S E R ;
SET: S E T ;
SOME: S O M E ;
SYSTEM_USER: S Y S T E M '_'U S E R ;
TABLE: T A B L E ;
TEMPORARY: T E M P O R A R Y ;
THEN: T H E N ;
TIME: T I M E ;
TIMESTAMP: T I M E S T A M P ;
TIMEZONE_HOUR: T I M E Z O N E '_'H O U R ;
TIMEZONE_MINUTE: T I M E Z O N E '_'M I N U T E ;
TO: T O ;
TRANSACTION: T R A N S A C T I O N ;
TRUE: T R U E ;
TYPE: T Y P E ;
UNCOMMITTED: U N C O M M I T T E D ;
UNION: U N I O N ;
UNIQUE: U N I Q U E ;
UNKNOWN: U N K N O W N ;
UPDATE: U P D A T E ;
USER: U S E R ;
USING: U S I N G ;
VALUE: V A L U E ;
VALUES: V A L U E S ;
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
NotEqualsOperator: '<>' | '!=';
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
fragment NonquoteCharacter: ~'\'';
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


// string literals
NationalCharacterStringLiteral: 'N' SingleQuote CharacterRepresentation* SingleQuote ((Separator)+ SingleQuote CharacterRepresentation* SingleQuote)*;
CharacterRepresentation: (NonquoteCharacter|QuoteSymbol);
BitStringLiteral: 'B' SingleQuote Bit* SingleQuote ((Separator)+ SingleQuote Bit* SingleQuote)*;
HexStringLiteral: 'X' SingleQuote Hexit* SingleQuote ((Separator)+ SingleQuote Hexit* SingleQuote)*;
StringLiteralContent: SingleQuote CharacterRepresentation* SingleQuote ((Separator)+ SingleQuote CharacterRepresentation* SingleQuote)*;

WS: Separator;
