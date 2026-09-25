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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndex;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Same shape as {@code GenericTableIndexConfigurator}, which it replaces for {@link
 * MimerTableIndex} - see the Indexes tree entry in {@code plugin.xml}, changed to declare that
 * type instead of the plain generic one, same "Create New" resolution requirement as every
 * other Mimer SQL-specific manager.
 * <p>
 * Also wires the "Ignore Nulls" checkbox, per-column "Include" choice, and per-column
 * "Algorithm" choice from {@link MimerCreateIndexPage} into {@link
 * MimerTableIndex#setIgnoreNulls}/{@link MimerTableIndex#setIncludedColumns}/{@link
 * MimerTableIndexColumn#getAlgorithm}. A column marked "Include" is added to {@code
 * includedColumns} instead of the index's own ordinal key-column list, since it isn't part of
 * the index key at all in Mimer SQL's grammar - just carried along in a separate {@code
 * INCLUDE (...)} clause (see {@link org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager}). So
 * {@code colIndex} is only advanced from a real key column, not simply the first selected one.
 * <p>
 * The index's own name is no longer built here at all - {@link
 * MimerCreateIndexPage#getIndexName()} owns that: an always-editable Name field, auto-suggested
 * from every selected key column and kept in sync until the user types into it themselves (see
 * that field's own Javadoc for the "{@code idxtst_id_IDX} instead of {@code idxtst_id_c2_IDX},
 * with no way to rename around the collision" bug this replaced).
 * <p>
 * Defensively normalizes a {@code CLUSTERED} index's Ignore Nulls/Include/Algorithm choices back
 * to "none" here too - on top of {@link MimerCreateIndexPage}'s own UI-level enforcement and
 * {@link org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager}'s DDL-level re-check, see the
 * {@code clustered} local below - so the invalid combination can never reach the DDL builder no
 * matter which layer a future change might otherwise miss.
 * <p>
 * Also keeps only the *first* selected column whose algorithm is non-Simple (see the {@code
 * algorithmUsed} local). Mimer SQL allows at most one such column per index, independent of
 * clustering - a per-index limit, not a per-table one. Different indexes on the same table can
 * each have their own algorithm; only two special columns within one index is actually rejected.
 * <p>
 * A column's Collation choice is passed straight through, with no clustered-related
 * normalization - unlike Ignore Nulls/Include/Algorithm, nothing suggests {@code COLLATE} is
 * incompatible with a {@code CLUSTERED} index.
 *
 * @author Mimer Information Technology
 */
public class MimerIndexConfigurator implements DBEObjectConfigurator<MimerTableIndex> {

    @NotNull
    @Override
    public MimerTableIndex configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object table,
        @NotNull MimerTableIndex index,
        @NotNull Map<String, Object> options
    ) {
        GenericTableBase tableBase = (GenericTableBase) table;
        boolean supportUniqueIndexes = tableBase.supportUniqueIndexes();
        Collection<DBSIndexType> tableIndexTypes = tableBase.getTableIndexTypes();
        boolean showIgnoreNulls = tableBase.getDataSource() instanceof MimerDataSource ds && ds.supportsClusteredIndexes();
        boolean showInclude = tableBase.getDataSource() instanceof MimerDataSource ds2 && ds2.supportsIndexInclude();
        return new UITask<MimerTableIndex>() {
            @Override
            protected MimerTableIndex runTask() {
                MimerCreateIndexPage editPage = new MimerCreateIndexPage(
                    "Create index",
                    index,
                    tableIndexTypes, supportUniqueIndexes, showIgnoreNulls, showInclude);
                if (!editPage.edit()) {
                    return null;
                }
                index.setIndexType(editPage.getIndexType());
                // Belt-and-suspenders: a CLUSTERED index cannot carry Ignore Nulls, Include, or a
                // non-Simple column Algorithm (see the class javadoc) - MimerCreateIndexPage
                // already keeps the UI from setting any of these while Clustered is selected, but
                // this also normalizes them away here regardless.
                boolean clustered = index.getIndexType() == DBSIndexType.CLUSTERED;
                // Mimer SQL allows at most one column per index to carry a non-Simple algorithm
                // ("... contains multiple type clauses which is not allowed" otherwise, confirmed
                // live even for two DIFFERENT algorithms on two columns) - MimerCreateIndexPage
                // already resets every other column the moment one is chosen, but this keeps only
                // the first regardless, same belt-and-suspenders reasoning as the clustered flag.
                boolean algorithmUsed = false;
                int colIndex = 1;
                List<GenericTableColumn> includedColumns = new ArrayList<>();
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    if (!clustered && Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, MimerCreateIndexPage.PROP_INCLUDED))) {
                        includedColumns.add((GenericTableColumn) tableColumn);
                        continue;
                    }
                    String algorithm = clustered ? null
                        : (String) editPage.getAttributeProperty(tableColumn, MimerCreateIndexPage.PROP_ALGORITHM);
                    if (algorithm != null) {
                        if (algorithmUsed) {
                            algorithm = null;
                        } else {
                            algorithmUsed = true;
                        }
                    }
                    String collation = (String) editPage.getAttributeProperty(tableColumn, MimerCreateIndexPage.PROP_COLLATION);
                    index.addColumn(
                        new MimerTableIndexColumn(
                            index,
                            (GenericTableColumn) tableColumn,
                            colIndex++,
                            !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                            algorithm,
                            collation));
                }
                index.setName(DBObjectNameCaseTransformer.transformObjectName(index, editPage.getIndexName()));
                index.setUnique(editPage.isUnique());
                index.setIgnoreNulls(!clustered && editPage.isIgnoreNulls());
                index.setIncludedColumns(includedColumns);
                return index;
            }
        }.execute();
    }
}
