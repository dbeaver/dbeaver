/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql.indent;

public interface SQLIndentSymbols {
    
    int TokenEOF   = -1;
    int TokenOTHER = 0;

    int TokenBEGIN = 1001;
    int TokenEND = 1002;

    int TokenCASE = 1003;

    int TokenLOOP = 1004;

    int TokenIF = 1010;
    int TokenTHEN = 1011;

    int TokenIDENT = 2000;
    int TokenKeyword = 3000;
    int TokenKeywordStart = 3001;

    String StrBEGIN = "BEGIN";
    String StrEND = "END";

    String StrCASE = "CASE";

    String StrLOOP = "LOOP";

    String StrIF = "IF";
    String StrTHEN = "THEN";
}

