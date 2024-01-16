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
package org.jkiss.dbeaver.ext.postgresql.model.impls.cockroach;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSequence;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CockroachSequence extends PostgreSequence {

    private static final Log log = Log.getLog(CockroachSequence.class);

    public CockroachSequence(PostgreSchema schema, JDBCResultSet dbResult) {
        super(schema, dbResult);
    }

    public CockroachSequence(PostgreSchema catalog) {
        super(catalog);
    }

    @Nullable
    @Override
    public String getDescription() {
        // Not supported yet (Cockroach 22.1.2)
        return super.getDescription();
    }

    @Override
    public void loadAdditionalInfo(DBRProgressMonitor monitor) {
        // Cache and cycle options not supported correctly yet (Cockroach 22.1.2)
        AdditionalInfo additionalInfo = getAdditionalInfo();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load sequence additional info")) {
            try (JDBCPreparedStatement dbSeqStat = session.prepareStatement(
                "SELECT * from information_schema.\"sequences\" WHERE sequence_schema=? AND sequence_name=?")) {
                dbSeqStat.setString(1, getSchema().getName());
                dbSeqStat.setString(2, getName());
                try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                    if (seqResults.next()) {
                        additionalInfo.setStartValue(JDBCUtils.safeGetLong(seqResults, "start_value"));
                        additionalInfo.setMinValue(JDBCUtils.safeGetLong(seqResults, "minimum_value"));
                        additionalInfo.setMaxValue(JDBCUtils.safeGetLong(seqResults, "maximum_value"));
                        additionalInfo.setIncrementBy(JDBCUtils.safeGetLong(seqResults, "increment"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading sequence values", e);
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load sequence last value")) {
            try (JDBCPreparedStatement dbSeqStat = session.prepareStatement(
                "SELECT * from " + getFullyQualifiedName(DBPEvaluationContext.DML))) {
                try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                    if (seqResults.next()) {
                        additionalInfo.setLastValue(JDBCUtils.safeGetLong(seqResults, "last_value"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading sequence las value", e);
        }
        additionalInfo.setLoaded(true);
    }

    @Override
    public boolean supportsCacheAndCycle() {
        return false;
    }
}
