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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Firebird sequence (generator).
 * Firebird has no minimum, maximum or cycle options, the start value can only be set when the sequence is created.
 */
public class FireBirdSequence extends GenericSequence implements DBPSystemObject {

    private String description;
    private final boolean isSystem;
    @Nullable
    private Long startValue;

    public FireBirdSequence(
        @NotNull GenericStructContainer container,
        @NotNull String name,
        @Nullable String description,
        @Nullable Long startValue,
        @Nullable Long incrementBy,
        boolean isSystem
    ) {
        super(container, name, description, null, null, null, incrementBy);
        this.description = description;
        this.startValue = startValue;
        this.isSystem = isSystem;
    }

    /**
     * Creates a new sequence which does not exist in the database yet.
     */
    public FireBirdSequence(@NotNull GenericStructContainer container, @NotNull String name) {
        super(container, name);
        setLastValue(null);
        this.startValue = FireBirdConstants.SEQUENCE_DEFAULT_START_VALUE;
        this.isSystem = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return super.getName();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Override
    public Number getLastValue() {
        return super.getLastValue();
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public Number getLastValue(@NotNull DBRProgressMonitor monitor) throws DBCException {
        if (super.getLastValue() == null && isPersisted()) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read sequence last value")) {
                try (JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT GEN_ID(" + DBUtils.getQuotedIdentifier(this) + ", 0) from RDB$DATABASE",
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
                    // Extra ResultSet types - to avoid early SQLException: The result set is closed
                    try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                        if (seqResults.next()) {
                            setLastValue(JDBCUtils.safeGetLong(seqResults, 1));
                        }
                    }
                } catch (SQLException e) {
                    throw new DBCException("Error reading sequence last value", e);
                }
            }
        }
        return super.getLastValue();
    }

    /**
     * Start value of the sequence, i.e. the value returned by the first NEXT VALUE FOR. Not available before Firebird 3.
     */
    @Nullable
    @Property(viewable = true, editableExpr = "object.optionsSupported", order = 3)
    public Long getStartValue() {
        return startValue;
    }

    public void setStartValue(@Nullable Long startValue) {
        this.startValue = startValue;
    }

    @Override
    @Property(hidden = true)
    public Number getMinValue() {
        return super.getMinValue();
    }

    @Override
    @Property(hidden = true)
    public Number getMaxValue() {
        return super.getMaxValue();
    }

    @Nullable
    @Override
    @Property(viewable = true, editableExpr = "object.optionsSupported", updatableExpr = "object.optionsSupported", order = 5)
    public Long getIncrementBy() {
        Number incrementBy = super.getIncrementBy();
        return incrementBy == null ? null : incrementBy.longValue();
    }

    public void setIncrementBy(@Nullable Long incrementBy) {
        super.setIncrementBy(incrementBy);
    }

    /**
     * START WITH and INCREMENT BY clauses are supported since Firebird 3.
     */
    public boolean isOptionsSupported() {
        return getDataSource().isServerVersionAtLeast(3, 0);
    }

    /**
     * Firebird 3 treats START WITH as the current value, so the first NEXT VALUE FOR returns it plus the increment.
     * Firebird 4 returns the START WITH value itself.
     */
    public boolean isStartValueStoredAsCurrent() {
        return isOptionsSupported() && !getDataSource().isServerVersionAtLeast(4, 0);
    }

    @Override
    public boolean isSystem() {
        return isSystem;
    }
}
