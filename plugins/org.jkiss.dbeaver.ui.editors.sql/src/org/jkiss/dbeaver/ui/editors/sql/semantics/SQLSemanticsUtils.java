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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.dbeaver.model.sql.SQLDialect;

public class SQLSemanticsUtils {

    public static String identifierToCanonicalForm(SQLDialect dialect, String rawIdentifierString, boolean forceUnquotted) {
        boolean isQuotted = dialect.isQuotedIdentifier(rawIdentifierString);
        String unquottedIdentifier = isQuotted ? dialect.getUnquotedIdentifier(rawIdentifierString) : rawIdentifierString;
        String actualIdentifierString = dialect.mustBeQuoted(unquottedIdentifier, isQuotted) 
            ? (forceUnquotted ? unquottedIdentifier : dialect.getQuotedIdentifier(unquottedIdentifier, isQuotted, false)) 
            : unquottedIdentifier.toLowerCase();
        return actualIdentifierString;
    }
}
