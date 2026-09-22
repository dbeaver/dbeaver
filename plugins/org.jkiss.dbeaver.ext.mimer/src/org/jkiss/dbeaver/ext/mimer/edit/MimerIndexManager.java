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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericIndexManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndex;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndexColumn;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Adds Mimer SQL's {@code CREATE [UNIQUE] CLUSTERED INDEX ...} / {@code ... IGNORE NULLS} DDL
 * on top of generic's plain {@code CREATE INDEX}.
 * <p>
 * {@code IGNORE NULLS} and {@code INCLUDE (col, ...)} are trailing clauses with no hook on
 * {@link GenericIndexManager}, so {@link #addObjectCreateActions} is overridden in full rather
 * than partially. Both flags come from {@link MimerTableIndex#setIgnoreNulls}/{@code
 * #setIncludedColumns} (set by the create dialog), since there's no existing catalog row to read
 * them from yet. {@link #appendIndexColumnModifiers} adds a per-column {@code FOR <algorithm>}
 * clause (word search / PinYin, see {@link MimerTableIndexColumn}) before the inherited {@code
 * ASC}/{@code DESC} suffix.
 * <p>
 * A {@code CLUSTERED} index cannot carry {@code IGNORE NULLS}, {@code INCLUDE (...)}, or a
 * per-column {@code FOR <algorithm>} clause - all three are mutually exclusive with clustering.
 * {@link MimerCreateIndexPage}/{@code MimerIndexConfigurator} (.ui) already keep the create
 * dialog from producing that combination - resetting Ignore Nulls/Include/Algorithm choices the
 * moment Clustered is selected as the index Type, and disabling those controls while it stays
 * selected. {@link #addObjectCreateActions}/{@link #appendIndexColumnModifiers} still
 * defensively re-check {@code index.getIndexType() == DBSIndexType.CLUSTERED} at the point the
 * DDL is actually built, so an invalid combination can never reach the server regardless of
 * what the UI layer did or didn't catch.
 * <p>
 * {@code CLUSTERED}, {@code IGNORE NULLS}, and {@code INCLUDE} are all Mimer SQL 11.1+ features
 * (see {@link MimerDataSource#supportsClusteredIndexes}/{@link
 * MimerDataSource#supportsIndexInclude}) - the create dialog only offers them on a server that
 * supports them, and this class trusts that: no separate version re-check here, since a
 * pre-11.1 server rejecting an unsupported clause outright is an acceptable fallback for
 * something the UI already keeps from happening in practice.
 *
 * @author Mimer Information Technology
 */
public class MimerIndexManager extends GenericIndexManager {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return super.getMakerOptions(dataSource) | FEATURE_DELETE_CASCADE;
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(MimerCascadeDropUtil.dropAction("Drop index",
            "DROP INDEX " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL),
            options, "index", command.getObject().getName(), executionContext));
    }

    @Override
    protected void appendIndexModifiers(GenericTableIndex index, StringBuilder decl) {
        super.appendIndexModifiers(index, decl);
        if (index.getIndexType() == DBSIndexType.CLUSTERED) {
            decl.append(" CLUSTERED");
        }
    }

    /**
     * Adds {@code COLLATE <collation>} (per {@code CREATE_INDEX.htm}: right after the column
     * name, before {@code FOR <algorithm>}/{@code ASC}/{@code DESC}) and {@code FOR <algorithm>}
     * before the inherited {@code ASC}/{@code DESC} suffix, matching Mimer SQL's own clause order
     * (e.g. {@code "c2" COLLATE "s"."n" FOR WORD_SEARCH DESC}). {@code FOR <algorithm>} is never
     * emitted for a {@code CLUSTERED} index (see the class javadoc) or for {@link
     * MimerConstants#INDEX_ALGORITHM_SIMPLE}/no algorithm at all - both mean an ordinary column.
     * {@code COLLATE} carries no such restriction - nothing suggests it's incompatible with
     * clustering, unlike Ignore Nulls/Include/Algorithm.
     */
    @Override
    protected void appendIndexColumnModifiers(DBRProgressMonitor monitor, StringBuilder decl, DBSTableIndexColumn indexColumn) {
        if (indexColumn instanceof MimerTableIndexColumn mimerColumn) {
            if (!CommonUtils.isEmpty(mimerColumn.getCollation())) {
                decl.append(" COLLATE ").append(mimerColumn.getCollation());
            }
            if (!CommonUtils.isEmpty(mimerColumn.getAlgorithm())
                && !MimerConstants.INDEX_ALGORITHM_SIMPLE.equalsIgnoreCase(mimerColumn.getAlgorithm())
                && indexColumn.getIndex().getIndexType() != DBSIndexType.CLUSTERED
            ) {
                decl.append(" FOR ").append(mimerColumn.getAlgorithm());
            }
        }
        super.appendIndexColumnModifiers(monitor, decl, indexColumn);
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        GenericTableBase table = command.getObject().getTable();
        GenericTableIndex index = command.getObject();

        final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
        if (index instanceof DBPNamedObject2) {
            ((DBPNamedObject2) index).setName(indexName);
        } else {
            log.error("Cannot set index name");
        }

        final String tableName = DBUtils.getEntityScriptName(table, options);

        StringBuilder decl = new StringBuilder(40);
        decl.append("CREATE");
        appendIndexModifiers(index, decl);
        decl.append(" INDEX ").append(indexName);
        appendIndexType(index, decl);
        decl.append(" ON ").append(tableName);
        appendIndexTypeAfterOn(index, decl);
        decl.append(" (");
        boolean firstColumn = true;
        for (DBSTableIndexColumn indexColumn : CommonUtils.safeCollection(index.getAttributeReferences(new VoidProgressMonitor()))) {
            if (!firstColumn) decl.append(",");
            firstColumn = false;
            decl.append(DBUtils.getQuotedIdentifier(indexColumn));
            appendIndexColumnModifiers(monitor, decl, indexColumn);
        }
        decl.append(")");
        if (index instanceof MimerTableIndex mimerIndex) {
            // Defensive re-check, independent of whatever the create dialog did or didn't catch -
            // see the class javadoc: a CLUSTERED index can never carry IGNORE NULLS or INCLUDE.
            boolean clustered = index.getIndexType() == DBSIndexType.CLUSTERED;
            try {
                if (!clustered && mimerIndex.isIgnoreNulls(monitor)) {
                    decl.append(" IGNORE NULLS");
                }
            } catch (DBException e) {
                log.error(e);
            }
            // IGNORE NULLS comes before INCLUDE, not the other way around.
            List<GenericTableColumn> includedColumns = clustered ? List.of() : mimerIndex.getIncludedColumns();
            if (!includedColumns.isEmpty()) {
                decl.append(" INCLUDE (");
                boolean firstIncluded = true;
                for (GenericTableColumn includedColumn : includedColumns) {
                    if (!firstIncluded) decl.append(",");
                    firstIncluded = false;
                    decl.append(DBUtils.getQuotedIdentifier(includedColumn));
                }
                decl.append(")");
            }
        }

        actions.add(
            new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString())
        );
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!command.hasProperty("comment") || !(command.getObject() instanceof MimerTableIndex index)) {
            return;
        }
        String name = "\"" + index.getTable().getSchema().getName() + "\".\"" + index.getName() + "\"";
        actionList.add(new SQLDatabasePersistAction("Comment index",
            MimerUtils.buildCommentDDL(index, "INDEX", name, index.getComment(monitor))));
    }
}
