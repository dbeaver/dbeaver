/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;

/**
 * Handler for types that don't support modifiers.
 * <p>
 * Displays base type name without {@code (...)} after
 * it and does not accept any modifiers when editing.
 */
public class PostgreEmptyTypeHandler extends PostgreTypeHandler {

    public static final PostgreEmptyTypeHandler INSTANCE = new PostgreEmptyTypeHandler();

    private PostgreEmptyTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        if (typmod.length == 0) {
            return EMPTY_MODIFIERS;
        }
        return super.getTypeModifiers(type, typeName, typmod);
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        return "";
    }
}
