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
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.DBSObjectWithType;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Mimer SQL procedure / function. Parameters are read from {@code INFORMATION_SCHEMA.PARAMETERS}
 * since the bundled driver predates JDBC 4.1 and lacks {@code getFunctionColumns()}. Carries a
 * {@link PrivilegeCache} for its "Privileges" folder ({@code EXECUTE} grants, see
 * {@link MimerRoutinePrivilege}).
 * <p>
 * Implements {@link DBSObjectWithScript} so the Source tab's {@code GenericSourceViewEditor}
 * opens editable - its {@code isReadOnly()} checks {@code instanceof DBSObjectWithScript}
 * directly, independent of the manager's {@code canEditObject()}.
 * <p>
 * {@link #getProcedureType()}/{@link #setProcedureType} and {@link #setSpecificName} let the
 * create dialog mutate a not-yet-persisted stub in place (Type combo, specific name) instead of
 * replacing it - the pending {@code ObjectCreateCommand} must stay bound to the same instance
 * the Source editor's Save writes into.
 * <p>
 * Implements {@link DBPStatefulObject} purely for the tree-icon overlay it gives an external
 * (Mimer SQL 11.1+ CLR) routine for free - {@link DBIcon#OVER_EXTERNAL}, the same overlay
 * mechanism {@code PostgreConstants.STATE_UNAVAILABLE}/{@code MimerUdtMethodSpec}'s own {@code
 * DBSObjectState.UNKNOWN} already use. There's no real "state" concept otherwise - every
 * {@code MimerProcedure} is equally "normal" apart from this.
 * <p>
 * {@link #getObjectState()} has no monitor to query with, so it can only ever see whatever
 * {@link #externalInfo} already holds. That's why {@link MimerMetaModel#loadProcedures} eagerly
 * populates it for every routine in one bulk query (see {@link #setExternalInfo}) - so the
 * overlay renders correctly the first time the tree is expanded, not only after something else
 * separately triggers {@link #loadExternalInfo}.
 * <p>
 * Implements {@link DBSObjectWithType} purely so the "Create New Procedure"/"Create New
 * Function" dialog's window title reflects which one is actually being created, instead of
 * always saying "Procedure" even when creating a function.
 * <p>
 * Root cause: {@code EditObjectDialog} derives that title from {@code
 * DBUtils.getObjectTypeName(object)}, which - absent a {@link DBSObjectWithType} override -
 * falls back to a static, class-based lookup over {@code getSupportedObjectTypes()}. That's
 * fine for most object types (one class, one name), but Procedures and Functions share this one
 * {@code MimerProcedure} class (see the class Javadoc above), so the static lookup can only
 * ever return one fixed name regardless of which folder was actually clicked. {@link
 * #getObjectType()} instead reflects the real, per-instance {@link #getProcedureType()} - the
 * same distinction the Type combo itself already tracks.
 *
 * @author Mimer Information Technology
 */
public class MimerProcedure extends GenericProcedure implements DBSObjectWithScript, DBSObjectWithType, DBPStatefulObject {

    private static final DBSObjectType OBJECT_TYPE_PROCEDURE =
        new AbstractObjectType("Procedure", "Mimer SQL procedure", DBIcon.TREE_PROCEDURE, MimerProcedure.class);
    private static final DBSObjectType OBJECT_TYPE_FUNCTION =
        new AbstractObjectType("Function", "Mimer SQL function", DBIcon.TREE_FUNCTION, MimerProcedure.class);

    private final PrivilegeCache privilegeCache = new PrivilegeCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();
    private DBSProcedureType overriddenProcedureType;
    private String overriddenSpecificName;
    private String comment;
    private MimerExternalRoutineInfo externalInfo;

    public MimerProcedure(
        GenericStructContainer container,
        String procedureName,
        String specificName,
        String description,
        DBSProcedureType procedureType,
        GenericFunctionResultType functionResultType
    ) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 6)
    public DBSProcedureType getProcedureType() {
        return overriddenProcedureType != null ? overriddenProcedureType : super.getProcedureType();
    }

    /**
     * CREATE-only - see the class Javadoc.
     */
    public void setProcedureType(@NotNull DBSProcedureType procedureType) {
        this.overriddenProcedureType = procedureType;
    }

    /**
     * See the class Javadoc - only actually consumed today via {@link
     * org.jkiss.dbeaver.model.DBUtils#getObjectTypeName}, for the create dialog's window title.
     */
    @NotNull
    @Override
    public DBSObjectType getObjectType() {
        return getProcedureType() == DBSProcedureType.FUNCTION ? OBJECT_TYPE_FUNCTION : OBJECT_TYPE_PROCEDURE;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return overriddenSpecificName != null ? overriddenSpecificName : super.getUniqueName();
    }

    /**
     * Re-declares {@code @Property} so this is a real, settable property - {@code
     * GenericProcedure}'s own {@code getObjectDefinitionText} has none, so the Source tab's Save
     * would otherwise discard edits instead of calling {@link #setObjectDefinitionText}. Oracle's
     * {@code OracleProcedureStandalone} and PostgreSQL's {@code PostgreProcedure} do the same.
     */
    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        return super.getObjectDefinitionText(monitor, options);
    }

    /**
     * CREATE-only, same reasoning as {@link #setProcedureType} - {@code specificName} has no
     * public setter on the base class.
     */
    public void setSpecificName(@Nullable String specificName) {
        this.overriddenSpecificName = specificName;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        setSource(source);
    }

    /**
     * Hides the inherited {@code AbstractProcedure.getDescription()} ("Procedure Description")
     * property - it duplicates {@link #getComment} below (same server-side text, but read-only
     * via the driver's {@code REMARKS} column vs. read/write via {@code EXT_OBJECT_IDENT_USAGE}).
     */
    @Nullable
    @Override
    @Property(hidden = true)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * {@code COMMENT ON SPECIFIC FUNCTION/PROCEDURE "schema"."specificName" IS '...'} - Mimer SQL
     * keys a routine's comment by specific name, not plain name. See
     * {@link org.jkiss.dbeaver.ext.mimer.edit.MimerProcedureManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && isPersisted()) {
            String objectType = getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            comment = MimerUtils.readObjectComment(monitor, this, getContainer().getName(), getUniqueName(), getName(), objectType);
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    /**
     * {@code SQL} for a normal routine, {@code CLR} (or whatever future language a server
     * reports) for one implemented in an {@link MimerLibrary} - see {@link #getExternalName}/
     * {@link #getExternalLibrary}. Hidden entirely ({@link ExternalLibrarySupportValidator}) on
     * a pre-11.1 server, where the concept doesn't exist at all - not just always-"SQL", since a
     * property with nothing meaningful to say shouldn't clutter the grid.
     */
    @NotNull
    @Property(viewable = true, order = 101, visibleIf = ExternalLibrarySupportValidator.class)
    public String getLanguage(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerExternalRoutineInfo info = loadExternalInfo(monitor);
        return info.external() ? CommonUtils.notEmpty(info.language()) : "SQL";
    }

    /**
     * The fully qualified CLR routine name ({@code EXTERNAL NAME '...'}) - {@code "N/A"} for a
     * normal SQL-bodied routine, hidden entirely on a pre-11.1 server (see {@link #getLanguage}).
     */
    @NotNull
    @Property(viewable = true, order = 102, visibleIf = ExternalLibrarySupportValidator.class)
    public String getExternalName(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerExternalRoutineInfo info = loadExternalInfo(monitor);
        return info.external() ? CommonUtils.notEmpty(info.externalName()) : "N/A";
    }

    /**
     * The {@link MimerLibrary} this routine is implemented in ({@code IN library-name}) -
     * {@code "N/A"} for a normal SQL-bodied routine, hidden entirely on a pre-11.1 server (see
     * {@link #getLanguage}).
     */
    @NotNull
    @Property(viewable = true, order = 103, visibleIf = ExternalLibrarySupportValidator.class)
    public String getExternalLibrary(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerExternalRoutineInfo info = loadExternalInfo(monitor);
        return info.external() ? CommonUtils.notEmpty(info.library()) : "N/A";
    }

    /**
     * {@code visibleIf} backing for {@link #getLanguage}/{@link #getExternalName}/{@link
     * #getExternalLibrary}. Uses the proven {@code IPropertyValueValidator}-class mechanism
     * (matching e.g. {@code PostgreSequence.CacheAndCycleValidator}'s own version-gated
     * property), not the {@code hideExpr} JEXL-expression alternative on {@code @Property} - no
     * other class in this codebase was found actually consuming {@code getHideExpression()}, so
     * it wasn't trusted here.
     */
    public static class ExternalLibrarySupportValidator implements IPropertyValueValidator<MimerProcedure, Object> {
        @Override
        public boolean isValidValue(@NotNull MimerProcedure object, @Nullable Object value) {
            return object.getDataSource() instanceof MimerDataSource ds && ds.supportsExternalLibraries();
        }
    }

    @NotNull
    private MimerExternalRoutineInfo loadExternalInfo(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (externalInfo == null) {
            externalInfo = isPersisted()
                ? MimerUtils.readExternalRoutineInfo(monitor, this, getSchema().getName(), getUniqueName())
                : MimerExternalRoutineInfo.NOT_EXTERNAL;
        }
        return externalInfo;
    }

    /**
     * Bulk-set by {@link MimerMetaModel#loadProcedures} from its own one-query-per-schema read -
     * see the class Javadoc for why. Package-visible: nothing outside the model package should
     * set this directly.
     */
    void setExternalInfo(@NotNull MimerExternalRoutineInfo externalInfo) {
        this.externalInfo = externalInfo;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return externalInfo != null && externalInfo.external()
            ? new DBSObjectState("External routine", DBIcon.OVER_EXTERNAL)
            : DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try {
            externalInfo = null;
            loadExternalInfo(monitor);
        } catch (DBException e) {
            throw new DBCException("Error refreshing external routine state", e);
        }
    }

    @Association
    public Collection<MimerRoutinePrivilege> getPrivileges(DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerProcedure, MimerRoutinePrivilege> getPrivilegeCache() {
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

    @Override
    public void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException {
        MimerUtils.loadProcedureColumns(this, monitor);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        privilegeCache.clearCache();
        usedByCache.clearCache();
        usesCache.clearCache();
        comment = null;
        externalInfo = null;
        return super.refreshObject(monitor);
    }

    /**
     * {@code EXECUTE} grants - see {@link MimerRoutinePrivilege}. {@code GRANTOR = '_SYSTEM'}
     * rows (Mimer SQL's synthetic "the owner implicitly holds EXECUTE" grant) are excluded -
     * {@code EXT_OBJECT_PRIVILEGES} carries these for a databank's owner too (see {@code
     * MimerDatabankPrivilege.PrivilegeCache}); same convention applied here on the assumption
     * it's a general Mimer SQL behaviour, not databank-specific.
     */
    static class PrivilegeCache extends JDBCObjectCache<MimerProcedure, MimerRoutinePrivilege> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProcedure owner) throws SQLException {
            String objectType = owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT GRANTEE, GRANTOR, IS_GRANTABLE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_PRIVILEGES\n" +
                "WHERE OBJECT_TYPE = ? AND OBJECT_SCHEMA = ? AND OBJECT_NAME = ? AND PRIVILEGE_TYPE = 'EXECUTE'\n" +
                "  AND GRANTOR <> '_SYSTEM'\n" +
                "ORDER BY GRANTEE");
            stmt.setString(1, objectType);
            stmt.setString(2, owner.getSchema().getName());
            stmt.setString(3, owner.getName());
            return stmt;
        }

        @Override
        protected MimerRoutinePrivilege fetchObject(@NotNull JDBCSession session, @NotNull MimerProcedure owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerRoutinePrivilege(owner, resultSet);
        }
    }

    static class UsedByCache extends JDBCObjectCache<MimerProcedure, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProcedure owner) throws SQLException {
            String objectType = owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            return MimerObjectUsedBy.prepareUsedByStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), objectType);
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerProcedure owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerProcedure, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerProcedure owner) throws SQLException {
            String objectType = owner.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            return MimerObjectUses.prepareUsesStatementBySpecificName(session, owner.getSchema().getName(), owner.getUniqueName(), objectType);
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerProcedure owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
