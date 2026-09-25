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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Mimer SQL sequence. Adds the Mimer SQL-specific {@code AS <type>} and
 * {@code CYCLE} attributes and builds the Mimer SQL {@code CREATE SEQUENCE} /
 * {@code ALTER SEQUENCE ... RESTART WITH} DDL.
 *
 * @author Mimer Information Technology
 */
public class MimerSequence extends GenericSequence {

    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();
    private String dataType;
    private boolean cycle;
    private String databank;
    private String comment;

    public MimerSequence(@NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        super(
            container,
            JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME"),
            null,
            // Deliberately not START_VALUE - see getLastValue()'s Javadoc for why that catalog
            // column can never stand in for "the sequence's current value".
            null,
            JDBCUtils.safeGetLong(dbResult, "MINIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "MAXIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "INCREMENT"));
        this.dataType = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
        this.cycle = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "CYCLE_OPTION"));
        this.databank = JDBCUtils.safeGetStringTrimmed(dbResult, "DATABANK_NAME");
    }

    public MimerSequence(@NotNull GenericStructContainer container, @NotNull String name) {
        super(container, name);
        this.dataType = "INTEGER";
        // Mimer SQL defaults for a fresh sequence: START WITH 1, no explicit bounds, NO CYCLE.
        setLastValue(1L);
        setMinValue(null);
        setMaxValue(null);
        setIncrementBy(1L);
    }

    @Property(viewable = true, editable = true, order = 6, listProvider = DataTypeListProvider.class)
    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Property(viewable = true, editable = true, order = 7)
    public boolean isCycle() {
        return cycle;
    }

    public void setCycle(boolean cycle) {
        this.cycle = cycle;
    }

    @Property(viewable = true, editable = true, order = 8)
    public String getDatabank() {
        return databank;
    }

    public void setDatabank(String databank) {
        this.databank = databank;
    }

    /**
     * Hides the inherited {@code GenericSequence.getDescription()} property - always {@code
     * null}, but still a pointless "Description" row next to the real {@link #getComment} below.
     */
    @Nullable
    @Override
    @Property(hidden = true)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * {@code COMMENT ON SEQUENCE "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerSequenceManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 9)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && isPersisted()) {
            comment = MimerUtils.readObjectComment(monitor, this, getParentObject().getName(), null, getName(), "SEQUENCE");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    /**
     * {@code USAGE} privileges granted on this sequence - see {@link MimerSequencePrivilege}.
     */
    @Association
    public Collection<MimerSequencePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    @NotNull
    public DBSObjectCache<MimerSequence, MimerSequencePrivilege> getPrivilegeCache() {
        return privilegeCache;
    }

    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    /**
     * Labeled "Restart With" (see the l10n bundle), not "Last Value" - confirmed live there is no
     * catalog column that ever reflects a Mimer SQL sequence's real current position. {@code
     * INFORMATION_SCHEMA.SEQUENCES.START_VALUE}/{@code EXT_SEQUENCES.INITIAL_VALUE} both stay
     * frozen at whatever {@code CREATE SEQUENCE} originally said, unchanged by any later {@code
     * ALTER SEQUENCE ... RESTART WITH} or {@code NEXT VALUE FOR} call - confirmed live restarting
     * a sequence to 100 still leaves {@code START_VALUE = 1} in the catalog, even though {@code
     * NEXT VALUE FOR} correctly then returns 100. The only way to read the real current value,
     * {@code CURRENT VALUE FOR}, requires a prior {@code NEXT VALUE FOR} in the very same session
     * - not something safe to do just to populate a property (it would consume a real value).
     * <p>
     * So for a persisted sequence this is write-only, no readback - always blank on load (the
     * constructor passes {@code null}, not {@code START_VALUE}), same shape as {@link
     * MimerUser#getPassword()}/{@link MimerDatabank#getFile()}'s file size. Typing a value and
     * saving emits {@code ALTER SEQUENCE ... RESTART WITH <value>} (see {@code
     * MimerSequenceManager#addObjectModifyActions}) - which does take effect server-side even
     * though this field (and the catalog it would otherwise read from) can never show it
     * afterward. For a not-yet-persisted sequence, unrelated to any of this, it's simply the
     * {@code START WITH} value the create dialog collects - see {@code
     * MimerCreateSequencePage#setLastValue}.
     * <p>
     * Carries an explicit {@code id = "restartWith"} so {@link
     * MimerSequenceManager#addObjectModifyActions}'s change-tracking can check {@code
     * hasProperty("restartWith")} instead of the inherited, equally-misleading {@code
     * "lastValue"}. This only renames the property's internal id, not its l10n label - core's
     * {@code ObjectPropertyDescriptor#getLocalizedString} builds that key from the getter method's own
     * name ({@code getLastValue}, inherited from {@link GenericSequence}, can't be renamed
     * without breaking the override) regardless of {@code id()}, so the bundle entry for this
     * property's "Restart With" text must stay keyed as {@code MimerSequence.lastValue.name} -
     * confirmed live that keying it {@code .restartWith.name} instead just made the label
     * fall through to {@code GenericSequence}'s own {@code lastValue} entry ("Value") instead of
     * this class's own.
     */
    @Override
    @Property(id = "restartWith", viewable = true, editable = true, updatable = true, order = 2)
    public Long getLastValue() {
        return toLong(super.getLastValue());
    }

    @Override
    @Property(viewable = true, editable = true, order = 3)
    public Long getMinValue() {
        return toLong(super.getMinValue());
    }

    @Override
    @Property(viewable = true, editable = true, order = 4)
    public Long getMaxValue() {
        return toLong(super.getMaxValue());
    }

    @Override
    @Property(viewable = true, editable = true, order = 5)
    public Long getIncrementBy() {
        return toLong(super.getIncrementBy());
    }

    @Nullable
    private static Long toLong(@Nullable Number value) {
        return value == null ? null : value.longValue();
    }

    /**
     * {@code CREATE SEQUENCE} DDL, in whichever form the connected server actually supports
     * - see {@link MimerDataSource#supportsModernSequenceSyntax}.
     */
    @NotNull
    public String buildCreateDDL() {
        boolean modern = !(getDataSource() instanceof MimerDataSource ds) || ds.supportsModernSequenceSyntax();
        return modern ? buildModernCreateDDL() : buildLegacyCreateDDL();
    }

    /**
     * Mimer SQL 11.0+ {@code CREATE SEQUENCE} statement.
     */
    @NotNull
    private String buildModernCreateDDL() {
        StringBuilder sb = new StringBuilder("CREATE SEQUENCE ");
        sb.append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        sb.append(" AS ").append(dataType == null || dataType.isEmpty() ? "INTEGER" : dataType);
        Long start = getLastValue();
        if (start != null) {
            sb.append("\n    START WITH ").append(start);
        }
        Long inc = getIncrementBy();
        if (inc != null) {
            sb.append("\n    INCREMENT BY ").append(inc);
        }
        Long min = getMinValue();
        sb.append(min != null ? "\n    MINVALUE " + min : "\n    NO MINVALUE");
        Long max = getMaxValue();
        sb.append(max != null ? "\n    MAXVALUE " + max : "\n    NO MAXVALUE");
        sb.append(cycle ? "\n    CYCLE" : "\n    NO CYCLE");
        if (databank != null && !databank.isEmpty()) {
            // Quote the databank name only when the dialect needs it, like every other identifier.
            sb.append("\n    IN ").append(DBUtils.getQuotedIdentifier(getDataSource(), databank));
        }
        return sb.toString();
    }

    /**
     * Mimer SQL 10.1's older {@code CREATE UNIQUE SEQUENCE} statement - {@code
     * INITIAL_VALUE}/{@code MAX_VALUE}/{@code MIN_VALUE}/{@code AS <type>} are each
     * independently optional. No {@code INCREMENT BY}, {@code CYCLE}, or databank clause exists
     * in this older grammar.
     */
    @NotNull
    private String buildLegacyCreateDDL() {
        StringBuilder sb = new StringBuilder("CREATE UNIQUE SEQUENCE ");
        sb.append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        Long start = getLastValue();
        if (start != null) {
            sb.append(" INITIAL_VALUE ").append(start);
        }
        Long max = getMaxValue();
        if (max != null) {
            sb.append(" MAX_VALUE ").append(max);
        }
        Long min = getMinValue();
        if (min != null) {
            sb.append(" MIN_VALUE ").append(min);
        }
        sb.append(" AS ").append(dataType == null || dataType.isEmpty() ? "INTEGER" : dataType);
        return sb.toString();
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<MimerSequence> {
        private static final String[] TYPES = {"SMALLINT", "INTEGER", "BIGINT"};

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(MimerSequence object) {
            return TYPES;
        }
    }

    /**
     * Mimer SQL only supports {@code RESTART WITH} on {@code ALTER SEQUENCE};
     * changing type / bounds / increment requires drop &amp; recreate.
     */
    @NotNull
    public String buildRestartDDL(@Nullable Number restartValue) {
        Number value = restartValue != null ? restartValue : getLastValue();
        return "ALTER SEQUENCE " + getFullyQualifiedName(DBPEvaluationContext.DDL)
            + " RESTART WITH " + (value != null ? value : 1);
    }

    /**
     * {@code GRANTOR = '_SYSTEM'} rows (Mimer SQL's synthetic "the owner implicitly holds USAGE"
     * grant) are excluded - {@code EXT_OBJECT_PRIVILEGES} carries these for a databank's owner
     * too (see {@code MimerDatabankPrivilege.PrivilegeCache}); same convention applied here on
     * the assumption it's a general Mimer SQL behaviour, not databank-specific.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerSequence, MimerSequencePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSequence owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = 'SEQUENCE' AND OBJECT_SCHEMA = ? AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'USAGE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, owner.getParentObject().getName());
            stmt.setString(2, owner.getName());
            return stmt;
        }

        @Override
        protected MimerSequencePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerSequence owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSequencePrivilege(owner, resultSet);
        }
    }

    static class UsedByCache extends JDBCObjectCache<MimerSequence, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSequence owner) throws SQLException {
            return MimerObjectUsedBy.prepareUsedByStatement(session, owner.getParentObject().getName(), owner.getName(), "SEQUENCE");
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerSequence owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerSequence, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSequence owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatement(session, owner.getParentObject().getName(), owner.getName(), "SEQUENCE");
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerSequence owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
