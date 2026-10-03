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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Describes when successful DDL becomes visible outside its execution context.
 */
public enum DBCDDLTransactionBehavior {
    AUTO(ModelMessages.model_ddl_transaction_behavior_auto),
    IMMEDIATE(ModelMessages.model_ddl_transaction_behavior_immediate),
    TRANSACTIONAL(ModelMessages.model_ddl_transaction_behavior_transactional),
    IGNORED(ModelMessages.model_ddl_transaction_behavior_ignored);

    private static final Log log = Log.getLog(DBCDDLTransactionBehavior.class);

    @NotNull
    public final String displayName;

    DBCDDLTransactionBehavior(@NotNull String displayName) {
        this.displayName = displayName;
    }

    @NotNull
    public static DBCDDLTransactionBehavior parse(@Nullable String value) {
        return CommonUtils.valueOf(DBCDDLTransactionBehavior.class, value, AUTO);
    }

    @NotNull
    public static DBCDDLTransactionBehavior resolve(
        @Nullable Boolean supportsTransactions,
        @Nullable Boolean supportsDDLAndDMLTransactions,
        @Nullable Boolean supportsDMLTransactionsOnly,
        @Nullable Boolean ddlCausesCommit,
        @Nullable Boolean ddlIgnored
    ) {
        if (Boolean.FALSE.equals(supportsTransactions)) {
            return IMMEDIATE;
        }
        boolean supportsBoth = Boolean.TRUE.equals(supportsDDLAndDMLTransactions);
        boolean supportsDMLOnly = Boolean.TRUE.equals(supportsDMLTransactionsOnly);
        boolean causesCommit = Boolean.TRUE.equals(ddlCausesCommit);
        boolean ignored = Boolean.TRUE.equals(ddlIgnored);

        if (supportsBoth && (supportsDMLOnly || causesCommit || ignored) || causesCommit && ignored) {
            return TRANSACTIONAL;
        }
        if (ignored) {
            return IGNORED;
        }
        if (causesCommit || supportsDMLOnly && ddlCausesCommit != null && ddlIgnored != null) {
            return IMMEDIATE;
        }
        return TRANSACTIONAL;
    }

    @NotNull
    public static DBCDDLTransactionBehavior resolve(@NotNull DatabaseMetaData metaData) {
        return resolve(
            readMetadataFlag(metaData::supportsTransactions),
            readMetadataFlag(metaData::supportsDataDefinitionAndDataManipulationTransactions),
            readMetadataFlag(metaData::supportsDataManipulationTransactionsOnly),
            readMetadataFlag(metaData::dataDefinitionCausesTransactionCommit),
            readMetadataFlag(metaData::dataDefinitionIgnoredInTransactions)
        );
    }

    @Nullable
    private static Boolean readMetadataFlag(@NotNull MetadataFlagReader reader) {
        try {
            return reader.read();
        } catch (SQLException | RuntimeException | LinkageError e) {
            log.debug("Error reading JDBC transaction metadata", e);
            return null;
        }
    }

    @FunctionalInterface
    private interface MetadataFlagReader {
        boolean read() throws SQLException;
    }
}
