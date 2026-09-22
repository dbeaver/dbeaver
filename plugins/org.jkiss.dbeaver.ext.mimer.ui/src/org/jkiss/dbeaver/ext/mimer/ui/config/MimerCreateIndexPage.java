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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerCollation;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * {@link EditIndexPage} plus a few Mimer-specific controls.
 * <p>
 * Adds a "Name" field (see {@link #getIndexName()}) - {@code EditIndexPage} itself deliberately
 * omits one, on the theory that a name is "usually" auto-generated from the column list, which
 * isn't good enough here (see the field's own Javadoc). Also adds an "Ignore Nulls" checkbox for
 * Mimer SQL 11.1's {@code CREATE INDEX ... IGNORE NULLS}, a per-column "Include" combo (Key /
 * Include) for its {@code INCLUDE (col, ...)} clause (both shown only when the server supports
 * them, see {@link MimerIndexConfigurator}), a per-column "Algorithm" combo for the {@code
 * "col" FOR <algorithm>} word-search/PinYin clause (see {@link
 * MimerConstants#INDEX_ALGORITHMS_FOR_CLAUSE} - always shown, not version-gated), and a
 * per-column "Collation" combo for its {@code "col" COLLATE <collation>} clause - offered only
 * on a character-type column (see {@link MimerConstants#isCharacterType}), unrestricted by
 * server version or by Clustered (unlike Algorithm/Include, nothing suggests {@code COLLATE} is
 * incompatible with clustering). The Include/Algorithm/Collation combos reuse {@link
 * EditIndexPage}'s own key-column picker table rather than a separate one, modeled on its
 * ASC/DESC "Order" column.
 * <p>
 * A {@code CLUSTERED} index cannot carry Ignore Nulls, Include, or a non-Simple per-column
 * Algorithm. {@link #wireIndexTypeCombo} attaches a listener to the (otherwise inaccessible, see
 * below) Type combo: the moment Clustered is selected, it unchecks+disables Ignore Nulls and
 * resets every row's Include/Algorithm choice back to its default. {@link #createCellEditor}
 * also refuses to open an editor for either column while Clustered stays selected, so the user
 * can't set them again in the meantime. Choosing a non-Simple algorithm (or Include) is
 * therefore only ever possible while a non-Clustered type is selected - which is why no reverse
 * guard (algorithm/include -&gt; Type) is needed.
 * <p>
 * Mimer SQL also allows at most one column per index to carry a non-Simple algorithm. This is a
 * per-index limit, not a table-wide one - different indexes on the same table can each freely
 * have their own algorithm, only two special columns within a single index is actually rejected
 * server-side ({@code "... contains multiple type clauses which is not allowed"}). {@link
 * #clearOtherAlgorithms} resets every other column in this dialog back to Simple the moment one
 * is chosen.
 * <p>
 * Each algorithm is also only valid on certain column data types. {@code WORD_SEARCH} needs a
 * character column, national or not ({@code CHARACTER}/{@code CHARACTER VARYING}/{@code
 * NATIONAL CHARACTER}/{@code NATIONAL CHARACTER VARYING}); {@code PINYIN_START}/{@code
 * PINYIN_START_T9} need a national character column specifically. {@link
 * #allowedAlgorithmValues} filters the per-column combo's choices accordingly - a column of
 * neither kind gets no Algorithm editor at all, same as a Clustered index. Type-name matching is
 * a plain {@code String} prefix/substring check against {@link DBSEntityAttribute#getTypeName()}.
 * <p>
 * Mimer SQL doesn't allow indexing a LOB column at all - {@code BINARY LARGE OBJECT}/{@code
 * CHARACTER LARGE OBJECT}/{@code NATIONAL CHARACTER LARGE OBJECT} (BLOB/CLOB/NCLOB). That's not
 * just excluded from carrying an algorithm - it's excluded as a key or Include column entirely,
 * since a LOB column's type name otherwise shares the exact prefix the character-type checks
 * above look for (e.g. {@code CHARACTER LARGE OBJECT} vs. plain {@code CHARACTER}). {@link
 * #getAttributes} filters these out of the column picker entirely, rather than leaving them
 * selectable and only refusing them an Algorithm choice - see {@link #isLargeObjectType}.
 * <p>
 * Also relabels the Type combo's {@link DBSIndexType#OTHER} entry from "Other" to "Standard",
 * for clarity now that a real second choice ({@code CLUSTERED}) sits next to it. Only the
 * combo's displayed text changes (via {@link #wireIndexTypeCombo}) - {@code DBSIndexType} is a
 * shared, non-enum class keyed by display name, so the underlying {@code OTHER} instance and its
 * identity comparisons elsewhere are left untouched.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateIndexPage extends EditIndexPage {

    private static final Log log = Log.getLog(MimerCreateIndexPage.class);

    public static final String PROP_INCLUDED = "included";
    public static final String PROP_ALGORITHM = "algorithm";
    public static final String PROP_COLLATION = "collation";

    /**
     * Key under which {@link #createCellEditor} stashes the actual (type-filtered) list of
     * algorithm values a given cell's combo was populated with, for {@link #saveCellValue} to
     * map the combo's selection index back through - the combo's contents vary per column (see
     * {@link #allowedAlgorithmValues}), unlike {@link #ALGORITHM_VALUES}, which is every choice
     * that exists at all.
     */
    private static final String ALGORITHM_VALUES_DATA_KEY = "mimerAllowedAlgorithmValues";

    /**
     * Display labels for the per-column Algorithm combo, index-aligned with {@link
     * #ALGORITHM_VALUES}. "Simple" (index 0) isn't itself a DDL keyword - see {@link
     * MimerConstants#INDEX_ALGORITHM_SIMPLE}.
     */
    private static final List<String> ALGORITHM_LABELS = buildAlgorithmLabels();

    /**
     * DDL keyword for each entry in {@link #ALGORITHM_LABELS} - {@code null} at index 0 (Simple)
     * means "no FOR clause at all", not the literal keyword {@code SIMPLE}.
     */
    private static final List<String> ALGORITHM_VALUES = buildAlgorithmValues();

    private final List<DBSIndexType> indexTypes;
    private final boolean showIgnoreNulls;
    private final boolean showInclude;
    private final int clusteredTypePosition;
    private boolean ignoreNulls;
    private boolean clusteredSelected;
    private Button ignoreNullsButton;
    private int includeColumnIndex = -1;
    private int algorithmColumnIndex = -1;
    private int collationColumnIndex = -1;

    /**
     * Every collation in the datasource, loaded once (see {@link #loadCollationNames()}) rather
     * than re-queried each time a Collation cell editor opens.
     */
    private List<String> collationNames;

    /**
     * A real, always-editable Name {@link Text} field (see {@link #getIndexName()}), instead of
     * building the name silently in {@code MimerIndexConfigurator}. That silent version only
     * ever included the *first* selected column - {@code idxtst_id_IDX} instead of {@code
     * idxtst_id_c2_IDX} for an index on {@code (id, c2)} - and gave no way to rename around a
     * collision with another index, so the create action would just fail server-side ({@code
     * "Index 'idxtst_id_IDX' already exists"}).
     * <p>
     * {@link #buildSuggestedName()} (correctly including every selected key column, with
     * {@link #showInclude Include}-only columns excluded) keeps the field in sync with the
     * column picker via {@link #handleColumnsChange()}, only until the user actually types into
     * the field ({@link #nameEdited}) - at which point it's entirely theirs. Same "auto-suggest
     * until touched" shape used for column-Include/Algorithm defaults elsewhere in this class.
     * {@link #suppressNameSync} distinguishes our own programmatic {@code setText} calls from
     * the user's own typing, so the sync doesn't immediately disable itself.
     */
    private Text nameText;
    private String indexName = "";
    private boolean nameEdited;
    private boolean suppressNameSync;

    public MimerCreateIndexPage(
        String title,
        DBSTableIndex index,
        Collection<DBSIndexType> indexTypes,
        boolean supportUniqueIndexes,
        boolean showIgnoreNulls,
        boolean showInclude
    ) {
        super(title, index, indexTypes, supportUniqueIndexes);
        this.indexTypes = new ArrayList<>(indexTypes);
        this.showIgnoreNulls = showIgnoreNulls;
        this.showInclude = showInclude;
        this.clusteredTypePosition = this.indexTypes.indexOf(DBSIndexType.CLUSTERED);
    }

    private static List<String> buildAlgorithmLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("Simple");
        labels.addAll(MimerConstants.INDEX_ALGORITHMS_FOR_CLAUSE.values());
        return labels;
    }

    private static List<String> buildAlgorithmValues() {
        List<String> values = new ArrayList<>();
        values.add(null);
        values.addAll(MimerConstants.INDEX_ALGORITHMS_FOR_CLAUSE.keySet());
        return values;
    }

    @NotNull
    private static String algorithmLabel(@Nullable String algorithm) {
        int index = ALGORITHM_VALUES.indexOf(algorithm);
        return ALGORITHM_LABELS.get(Math.max(index, 0));
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel) {
        createNameField(panel);
        super.createContentsBeforeColumns(panel);
        wireIndexTypeCombo(panel);
        if (showIgnoreNulls) {
            ignoreNullsButton = UIUtils.createLabelCheckbox(panel, "Ignore Nulls", false);
            ignoreNullsButton.addSelectionListener(
                SelectionListener.widgetSelectedAdapter(e -> ignoreNulls = ignoreNullsButton.getSelection()));
        }
    }

    private void createNameField(@NotNull Composite panel) {
        nameText = UIUtils.createLabelText(panel, "Name", "");
        nameText.addModifyListener(e -> {
            indexName = nameText.getText();
            if (!suppressNameSync) {
                nameEdited = true;
            }
            validateProperties();
            updatePageState();
        });
    }

    /**
     * The index name - user-typed if {@link #nameEdited}, otherwise whatever {@link
     * #buildSuggestedName()} last computed. Never {@code null}, but can be blank (an empty
     * field) - {@link #getEditError()}/{@link #isPageComplete()} keep the dialog's OK button
     * disabled until it isn't.
     */
    @NotNull
    public String getIndexName() {
        return indexName == null ? "" : indexName.trim();
    }

    /**
     * Recomputes the suggested name from every currently-selected key column (see the {@link
     * #nameText} field javadoc) and pushes it into the field - but only while the user hasn't
     * typed a name themselves.
     */
    @Override
    protected void handleColumnsChange() {
        super.handleColumnsChange();
        updateSuggestedName();
    }

    private void updateSuggestedName() {
        if (nameEdited || nameText == null || nameText.isDisposed()) {
            return;
        }
        String suggested = buildSuggestedName();
        suppressNameSync = true;
        nameText.setText(suggested);
        suppressNameSync = false;
        indexName = suggested;
    }

    @NotNull
    private String buildSuggestedName() {
        StringBuilder name = new StringBuilder();
        if (getObject() instanceof DBSTableIndex tableIndex) {
            name.append(CommonUtils.escapeIdentifier(tableIndex.getTable().getName()));
        }
        for (DBSEntityAttribute attribute : getSelectedAttributes()) {
            if (Boolean.TRUE.equals(getAttributeProperty(attribute, PROP_INCLUDED))) {
                // Not part of the index key - see MimerIndexConfigurator's own reasoning.
                continue;
            }
            name.append("_").append(CommonUtils.escapeIdentifier(attribute.getName()));
        }
        name.append("_IDX");
        return name.toString();
    }

    @Nullable
    @Override
    protected String getEditError() {
        if (CommonUtils.isEmpty(getIndexName())) {
            return MimerUIMessages.page_create_index_name_error;
        }
        return super.getEditError();
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete() && !CommonUtils.isEmpty(getIndexName());
    }

    /**
     * {@code EditIndexPage} keeps its type {@code Combo} private with no label-customization or
     * selection-listener hook, so it's found by walking {@code panel}'s children (relabeling by
     * position, matching this class's own copy of the same index-type list in the same order -
     * the selection index itself, what {@link #getIndexType()} reads, is untouched) and, when
     * Clustered is one of the choices, given a selection listener that enforces the
     * clustered/Ignore-Nulls/Include/Algorithm mutual exclusion described in the class javadoc.
     */
    private void wireIndexTypeCombo(@NotNull Composite panel) {
        for (Control control : panel.getChildren()) {
            if (control instanceof Combo combo) {
                for (int i = 0; i < indexTypes.size() && i < combo.getItemCount(); i++) {
                    if (indexTypes.get(i) == DBSIndexType.OTHER) {
                        combo.setItem(i, "Standard");
                    }
                }
                if (clusteredTypePosition >= 0) {
                    combo.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                        e -> setClusteredSelected(combo.getSelectionIndex() == clusteredTypePosition)));
                }
                break;
            }
        }
    }

    private void setClusteredSelected(boolean clustered) {
        if (clustered == clusteredSelected) {
            return;
        }
        clusteredSelected = clustered;
        if (clustered) {
            if (ignoreNullsButton != null) {
                ignoreNullsButton.setSelection(false);
                ignoreNullsButton.setEnabled(false);
                ignoreNulls = false;
            }
            resetSpecialColumnChoices();
            updateSuggestedName();
        } else if (ignoreNullsButton != null) {
            ignoreNullsButton.setEnabled(true);
        }
    }

    /**
     * Clears every row's Include/Algorithm choice back to its default - called the moment
     * Clustered is selected, since neither is compatible with it (see the class javadoc).
     */
    private void resetSpecialColumnChoices() {
        for (TableItem item : columnsTable.getItems()) {
            if (!(item.getData() instanceof AttributeInfo<?> attributeInfo)) {
                continue;
            }
            if (showInclude) {
                attributeInfo.setProperty(PROP_INCLUDED, null);
                item.setText(includeColumnIndex, "Key");
            }
            attributeInfo.setProperty(PROP_ALGORITHM, null);
            item.setText(algorithmColumnIndex, algorithmLabel(null));
        }
    }

    @Override
    protected void createAttributeColumns(@NotNull Table columnsTable) {
        super.createAttributeColumns(columnsTable);
        if (showInclude) {
            TableColumn colInclude = UIUtils.createTableColumn(columnsTable, SWT.NONE, "Include");
            colInclude.setToolTipText(
                "Carry this column in the index for lookup-only queries, without making it part of the index key");
        }
        TableColumn colAlgorithm = UIUtils.createTableColumn(columnsTable, SWT.NONE, "Algorithm");
        colAlgorithm.setToolTipText(
            "Special access-path algorithm for word search or PinYin lookups on this column - "
                + "\"Simple\" is an ordinary index. Word Search needs a character column, PinYin a "
                + "national character one. Only one column per index may use a non-Simple "
                + "algorithm, and none is available together with a Clustered index");
        TableColumn colCollation = UIUtils.createTableColumn(columnsTable, SWT.NONE, "Collation");
        colCollation.setToolTipText("Collation for this column, if not the column's own default - character types only");
    }

    @Override
    protected int fillAttributeColumns(
        @NotNull DBSEntityAttribute attribute,
        @NotNull AttributeInfo<DBSEntityAttribute> attributeInfo,
        @NotNull TableItem columnItem
    ) {
        int lastIndex = super.fillAttributeColumns(attribute, attributeInfo, columnItem);
        if (showInclude) {
            includeColumnIndex = lastIndex + 1;
            columnItem.setText(includeColumnIndex, "Key");
            lastIndex = includeColumnIndex;
        }
        algorithmColumnIndex = lastIndex + 1;
        columnItem.setText(algorithmColumnIndex, algorithmLabel(null));
        collationColumnIndex = algorithmColumnIndex + 1;
        columnItem.setText(collationColumnIndex, "");
        return collationColumnIndex;
    }

    @Nullable
    @Override
    protected Control createCellEditor(
        @NotNull Table table,
        int index,
        @NotNull TableItem item,
        @NotNull AttributeInfo<DBSEntityAttribute> attributeInfo
    ) {
        if (showInclude && index == includeColumnIndex) {
            if (clusteredSelected) {
                // Include isn't valid on a Clustered index - see the class javadoc.
                return null;
            }
            boolean included = Boolean.TRUE.equals(attributeInfo.getProperty(PROP_INCLUDED));
            CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
            combo.add("Key");
            combo.add("Include");
            combo.select(included ? 1 : 0);
            return combo;
        }
        if (index == algorithmColumnIndex) {
            if (clusteredSelected) {
                // No special algorithm is valid on a Clustered index - see the class javadoc.
                return null;
            }
            List<String> allowedValues = allowedAlgorithmValues(attributeInfo.getAttribute());
            if (allowedValues.size() <= 1) {
                // Only Simple applies to this column's data type - nothing to choose.
                return null;
            }
            CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (String value : allowedValues) {
                combo.add(algorithmLabel(value));
            }
            combo.setData(ALGORITHM_VALUES_DATA_KEY, allowedValues);
            combo.select(Math.max(allowedValues.indexOf(attributeInfo.getProperty(PROP_ALGORITHM)), 0));
            return combo;
        }
        if (index == collationColumnIndex) {
            if (!MimerConstants.isCharacterType(attributeInfo.getAttribute().getTypeName())) {
                // Collation only applies to character types - see the class javadoc.
                return null;
            }
            List<String> names = loadCollationNames();
            CCombo combo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
            combo.add("");
            for (String name : names) {
                combo.add(name);
            }
            String current = (String) attributeInfo.getProperty(PROP_COLLATION);
            combo.select(current == null ? 0 : Math.max(names.indexOf(current) + 1, 0));
            return combo;
        }
        return super.createCellEditor(table, index, item, attributeInfo);
    }

    /**
     * Every collation in the datasource, quoted {@code "schema"."name"} - same convention as the
     * Domain/Collation create dialogs' own Collation pickers - plus {@link
     * MimerConstants#CURRENT_COLLATION_NAMES}, the three server-session-level dynamic collation
     * bindings (not real catalog rows, so {@code getCollations} never returns them). Loaded once
     * and cached, since the Collation column's cell editor can open many times while the dialog
     * is open.
     */
    @NotNull
    private List<String> loadCollationNames() {
        if (collationNames == null) {
            collationNames = new ArrayList<>(MimerConstants.CURRENT_COLLATION_NAMES);
            try {
                if (getObject() instanceof DBSTableIndex tableIndex && tableIndex.getDataSource() instanceof MimerDataSource ds) {
                    for (MimerCollation collation : ds.getCollations(new VoidProgressMonitor())) {
                        collationNames.add(collation.getReference());
                    }
                }
            } catch (DBException e) {
                log.debug("Can't load collation list for the create-index dialog", e);
            }
        }
        return collationNames;
    }

    /**
     * The algorithm choices valid for one column, always starting with {@code null} (Simple) -
     * see the class javadoc for the data-type rule this implements.
     * <p>
     * {@code CHARACTER LARGE OBJECT}/{@code NATIONAL CHARACTER LARGE OBJECT} (CLOB/NCLOB) share
     * the exact same {@code "CHARACTER"}/{@code "NATIONAL CHARACTER"} prefix
     * as the ordinary fixed/varying character types this method means to match, but Mimer SQL
     * doesn't allow indexing a LOB column at all, let alone with a special algorithm - excluded
     * explicitly ({@link #isLargeObjectType}) rather than just prefix-matched.
     */
    @NotNull
    private static List<String> allowedAlgorithmValues(@NotNull DBSEntityAttribute attribute) {
        String typeName = CommonUtils.notEmpty(attribute.getTypeName()).toUpperCase(Locale.ROOT);
        boolean largeObject = isLargeObjectType(typeName);
        boolean nationalCharacter = !largeObject
            // "NATIONAL CHAR" (not "NATIONAL CHARACTER") is deliberate - Mimer SQL's catalog
            // echoes NATIONAL CHAR LARGE OBJECT back abbreviated even though CREATE TABLE
            // itself accepts the spelled-out form too; "NATIONAL CHAR" as a prefix matches both,
            // since "NATIONAL CHARACTER..." already starts with the characters "NATIONAL CHAR".
            // Not confirmed whether plain NATIONAL CHARACTER/NATIONAL CHARACTER VARYING (not LOB)
            // get the same abbreviated treatment - this covers that possibility regardless.
            && (typeName.startsWith("NATIONAL CHAR") || typeName.startsWith("NCHAR") || typeName.startsWith("NVARCHAR"));
        boolean character = !largeObject
            && (nationalCharacter || typeName.startsWith("CHARACTER") || typeName.startsWith("CHAR") || typeName.startsWith("VARCHAR"));
        List<String> values = new ArrayList<>();
        values.add(null);
        if (character) {
            values.add(MimerConstants.INDEX_ALGORITHM_WORD_SEARCH);
        }
        if (nationalCharacter) {
            values.add(MimerConstants.INDEX_ALGORITHM_PINYIN_START);
            values.add(MimerConstants.INDEX_ALGORITHM_PINYIN_START_T9);
        }
        return values;
    }

    /**
     * {@code typeName} is already upper-cased by the caller. Matches every Mimer SQL LOB type -
     * {@code BINARY LARGE OBJECT}/{@code BLOB}, {@code CHARACTER LARGE OBJECT}/{@code CLOB}, and
     * {@code NATIONAL CHARACTER LARGE OBJECT}/{@code NCLOB} - both the full SQL-standard spelling
     * (all three share the {@code "... LARGE OBJECT"} suffix) and the abbreviation, in case the
     * driver reports either - both are harmless to check for. See {@link #getAttributes}: Mimer
     * SQL doesn't allow indexing a LOB column at all, key, Include, or algorithm-bearing, so
     * these are filtered out of the column picker entirely rather than just excluded from the
     * Algorithm choices.
     */
    private static boolean isLargeObjectType(@NotNull String typeName) {
        return typeName.contains("LARGE OBJECT") || typeName.contains("BLOB") || typeName.contains("CLOB");
    }

    /**
     * Excludes LOB columns (see {@link #isLargeObjectType}) from the picker entirely - Mimer SQL
     * doesn't allow indexing one in any capacity, not just with a special algorithm.
     */
    @NotNull
    @Override
    protected List<? extends DBSEntityAttribute> getAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity object
    ) throws DBException {
        List<DBSEntityAttribute> attributes = new ArrayList<>();
        for (DBSEntityAttribute attribute : super.getAttributes(monitor, object)) {
            if (!isLargeObjectType(CommonUtils.notEmpty(attribute.getTypeName()).toUpperCase(Locale.ROOT))) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }

    @Override
    protected void saveCellValue(
        @NotNull Control control,
        int index,
        @NotNull TableItem item,
        @NotNull AttributeInfo<DBSEntityAttribute> attributeInfo
    ) {
        if (showInclude && index == includeColumnIndex) {
            CCombo combo = (CCombo) control;
            boolean included = combo.getSelectionIndex() == 1;
            item.setText(index, included ? "Include" : "Key");
            attributeInfo.setProperty(PROP_INCLUDED, included);
            // A column moving to/from Include changes which columns the suggested name should
            // cover - see buildSuggestedName().
            updateSuggestedName();
        } else if (index == algorithmColumnIndex) {
            CCombo combo = (CCombo) control;
            @SuppressWarnings("unchecked")
            List<String> allowedValues = (List<String>) combo.getData(ALGORITHM_VALUES_DATA_KEY);
            String algorithm = allowedValues.get(combo.getSelectionIndex());
            item.setText(index, algorithmLabel(algorithm));
            attributeInfo.setProperty(PROP_ALGORITHM, algorithm);
            if (algorithm != null) {
                clearOtherAlgorithms(item);
            }
        } else if (index == collationColumnIndex) {
            CCombo combo = (CCombo) control;
            int selectionIndex = combo.getSelectionIndex();
            String collation = selectionIndex <= 0 ? null : loadCollationNames().get(selectionIndex - 1);
            item.setText(index, CommonUtils.notEmpty(collation));
            attributeInfo.setProperty(PROP_COLLATION, collation);
        } else {
            super.saveCellValue(control, index, item, attributeInfo);
        }
    }

    /**
     * Mimer SQL allows at most one column per index to carry a non-Simple algorithm ({@code "The
     * definition for the index ... contains multiple type clauses which is not allowed"} when two
     * columns each had one, e.g. one WORD_SEARCH and one PINYIN_START - even two of the same
     * algorithm on different columns isn't allowed; Simple is the one exception, compatible with
     * everything since it isn't a "type clause" at all). Picking a special algorithm for one
     * column resets every other column's back to Simple - same "auto-correct on conflict" shape
     * as {@link #setClusteredSelected}.
     */
    private void clearOtherAlgorithms(@NotNull TableItem chosenItem) {
        for (TableItem item : columnsTable.getItems()) {
            if (item == chosenItem || !(item.getData() instanceof AttributeInfo<?> attributeInfo)) {
                continue;
            }
            if (attributeInfo.getProperty(PROP_ALGORITHM) != null) {
                attributeInfo.setProperty(PROP_ALGORITHM, null);
                item.setText(algorithmColumnIndex, algorithmLabel(null));
            }
        }
    }

    public boolean isIgnoreNulls() {
        return ignoreNulls;
    }
}
