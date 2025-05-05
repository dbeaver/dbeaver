/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.athena.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

/**
 * Athena SQL dialect
 */
public class AthenaSQLDialect extends GenericSQLDialect {
    public AthenaSQLDialect() {
        super("Athena", "aws_athena");
    }

    // https://docs.aws.amazon.com/athena/latest/ug/tables-databases-columns-names.html#tables-databases-columns-names-complex-types
    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return SQLUtils.isLatinLetter(c) || Character.isDigit(c) || c == '_' || (quoted && validCharacters.indexOf(c) != -1);
    }
}
