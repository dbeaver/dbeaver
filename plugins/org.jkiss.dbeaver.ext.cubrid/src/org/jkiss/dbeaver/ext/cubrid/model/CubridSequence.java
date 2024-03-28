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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class CubridSequence extends GenericSequence
{
    private String owner;
    private Number cyclic;
    private Number cachedNum;

    public CubridSequence(
            @NotNull GenericStructContainer container,
            @NotNull String name,
            @Nullable String description,
            @NotNull Number lastValue,
            @NotNull Number minValue,
            @NotNull Number maxValue,
            @NotNull Number incrementBy,
            @NotNull JDBCResultSet dbResult) {
        super(container, name, description, lastValue, minValue, maxValue, incrementBy);
        this.owner = JDBCUtils.safeGetString(dbResult, "owner.name");
        this.cyclic = JDBCUtils.safeGetInteger(dbResult, "cyclic");
        this.cachedNum = JDBCUtils.safeGetInteger(dbResult, "cached_num");
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getOwner() {
        return owner;
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public Number getCyclic() {
        return cyclic;
    }

    @NotNull
    @Property(viewable = true, order = 8)
    public Number getCachedNum() {
        return cachedNum;
    }
}
