/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLStateType;

/**
 * Info
 */
public class WMIDialect extends BasicSQLDialect {

    private static final String[][] DEFAULT_QUOTE_STRINGS = {{"'", "'"}};

    public WMIDialect()
    {
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "WMI";
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings()
    {
        return DEFAULT_QUOTE_STRINGS;
    }

    @NotNull
    @Override
    public SQLStateType getSQLStateType()
    {
        return SQLStateType.UNKNOWN;
    }

}
