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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FireBirdDataSource
 */
public class FireBirdSequence extends GenericSequence implements DBPSystemObject {

    private String description;
    private boolean isSystem;

    public FireBirdSequence(GenericStructContainer container, String name, String description, Number lastValue, Number minValue, Number maxValue, Number incrementBy, boolean isSystem) {
        super(container, name, description, lastValue, minValue, maxValue, incrementBy);
        this.description = description;
        this.isSystem = isSystem;
    }

    @Nullable
    @Override
    @Property(viewable = true, updatable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Number getLastValue() {
        return super.getLastValue();
    }

    @Property(viewable = true, order = 2)
    public Number getLastValue(DBRProgressMonitor monitor) throws DBCException {
        if (super.getLastValue() == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read sequence last value")) {
                try (JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT GEN_ID(\"" + getName() + "\", 0) from RDB$DATABASE",
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

    @Override
    public boolean isSystem() {
        return isSystem;
    }
}
