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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class VerticaSequence extends GenericSequence implements GenericScriptObject {

    private static final Log log = Log.getLog(VerticaSequence.class);

    private String name;
    private String identityTableName;
    private long cacheCount;
    private boolean isCycle;
    private VerticaSchema schema;
    private String description;
    private String source;
    private boolean isPersisted;

    public VerticaSequence(GenericStructContainer container, String name, String description, Number lastValue, Number minValue, Number maxValue, Number incrementBy, String identityTableName, long cacheCount, boolean isCycle) {
        super(container, name, description, lastValue, minValue, maxValue, incrementBy);
        this.name = name;
        this.identityTableName = identityTableName;
        this.cacheCount = cacheCount;
        this.isCycle = isCycle;
        this.schema = (VerticaSchema) container.getSchema();
        this.description = description;
        this.isPersisted = true;
    }

    public VerticaSequence(GenericStructContainer container, String name) {
        super(container, name, null, 0, 1, 9223372036854775807L, 1);
        this.schema = (VerticaSchema) container.getSchema();
        this.cacheCount = 25000;
        this.isPersisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Property(viewable = true, order = 11)
    public GenericTableBase getIdentityTableName(DBRProgressMonitor monitor) {
        GenericTableBase table = null;
        if (CommonUtils.isEmpty(identityTableName)) {
            return null;
        }
        try {
            table = schema.getTable(monitor, identityTableName);
        } catch (DBException e) {
            log.debug("Can't find identity table", e);
        }
        return table;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public Long getLastValue() {
        return super.getLastValue().longValue();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public Long getMinValue() {
        return super.getMinValue().longValue();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public Long getMaxValue() {
        return super.getMaxValue().longValue();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public Long getIncrementBy() {
        return super.getIncrementBy().longValue();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public long getCacheCount() {
        return cacheCount;
    }

    public void setCacheCount(long cacheCount) {
        this.cacheCount = cacheCount;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public boolean isCycle() {
        return isCycle;
    }

    public void setCycle(boolean cycle) {
        isCycle = cycle;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, editable = true, updatable = true, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPersisted() {
        return isPersisted;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            if (!isPersisted) {
                source = "CREATE SEQUENCE " + getFullyQualifiedName(DBPEvaluationContext.DML);
            } else {
                StringBuilder ddl = new StringBuilder();
                ddl.append("CREATE SEQUENCE ")
                    .append(getFullyQualifiedName(DBPEvaluationContext.DML))
                    .append("\n\tINCREMENT BY ").append(getIncrementBy())
                    .append("\n\tMINVALUE ").append(getMinValue())
                    .append("\n\tMAXVALUE ").append(getMaxValue())
                    .append("\n\tSTART WITH ").append(getLastValue());

                if (cacheCount <= 1) {
                    ddl.append("\n\tNO CACHE");
                } else {
                    ddl.append("\n\tCACHE ").append(cacheCount);
                }
                ddl.append("\n\t").append(isCycle ? "" : "NO ").append("CYCLE;");

                if (!CommonUtils.isEmpty(description)) {
                    ddl.append("\n\nCOMMENT ON SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DML)).append(" IS ")
                        .append(SQLUtils.quoteString(this, description)).append(";");
                }
                source = ddl.toString();
            }
        }
        return source;
    }
}
