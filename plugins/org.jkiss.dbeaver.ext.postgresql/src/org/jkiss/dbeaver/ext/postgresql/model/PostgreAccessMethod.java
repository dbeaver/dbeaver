/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreAccessMethod
 */
public class PostgreAccessMethod extends PostgreInformation {

    public static final String CAT_ROUTINES = "Routines";
    public static final String CAT_FLAGS = "Flags";

    private long oid;
    private String name;

    private String handler;
    private String type;

    private int operatorStrategies;
    private int supportRoutines;
    private boolean canOrder;
    private boolean canOrderByOp;
    private boolean canBackward;
    private boolean canUnique;
    private boolean canMultiCol;
    private boolean optionalKey;
    private boolean searchArray;
    private boolean searchNulls;
    private boolean storage;
    private boolean clusterable;
    private boolean predLocks;
    private OperatorFamilyCache operatorFamilyCache = new OperatorFamilyCache();
    private OperatorClassCache operatorClassCache = new OperatorClassCache();
    
    public PostgreAccessMethod(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        super(database);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "amname");
        if (getDataSource().isServerVersionAtLeast(9, 6)) {
            // New simpler version of pg_am
            this.handler = JDBCUtils.safeGetString(dbResult, "amhandler");
            this.type = JDBCUtils.safeGetString(dbResult, "amtype");
        } else {
            this.operatorStrategies = JDBCUtils.safeGetInt(dbResult, "amstrategies");
            this.supportRoutines = JDBCUtils.safeGetInt(dbResult, "amsupport");
            this.canOrder = JDBCUtils.safeGetBoolean(dbResult, "amcanorder");
            this.canOrderByOp = JDBCUtils.safeGetBoolean(dbResult, "amcanorderbyop");
            this.canBackward = JDBCUtils.safeGetBoolean(dbResult, "amcanbackward");
            this.canUnique = JDBCUtils.safeGetBoolean(dbResult, "amcanunique");
            this.canMultiCol = JDBCUtils.safeGetBoolean(dbResult, "amcanmulticol");
            this.optionalKey = JDBCUtils.safeGetBoolean(dbResult, "amoptionalkey");
            this.searchArray = JDBCUtils.safeGetBoolean(dbResult, "amsearcharray");
            this.searchNulls = JDBCUtils.safeGetBoolean(dbResult, "amsearchnulls");
            this.storage = JDBCUtils.safeGetBoolean(dbResult, "amstorage");
            this.clusterable = JDBCUtils.safeGetBoolean(dbResult, "amclusterable");
            this.predLocks = JDBCUtils.safeGetBoolean(dbResult, "ampredlocks");
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 3)
    public String getHandler() {
        return handler;
    }

    @Property(viewable = true, order = 4)
    public String getType() {
        return type;
    }

    @Property(category = CAT_ROUTINES, order = 100)
    public int getOperatorStrategies() {
        return operatorStrategies;
    }

    @Property(category = CAT_ROUTINES, order = 101)
    public int getSupportRoutines() {
        return supportRoutines;
    }

    @Property(category = CAT_FLAGS, order = 200)
    public boolean isCanOrder() {
        return canOrder;
    }

    @Property(category = CAT_FLAGS, order = 201)
    public boolean isCanOrderByOp() {
        return canOrderByOp;
    }

    @Property(category = CAT_FLAGS, order = 202)
    public boolean isCanBackward() {
        return canBackward;
    }

    @Property(category = CAT_FLAGS, order = 203)
    public boolean isCanUnique() {
        return canUnique;
    }

    @Property(category = CAT_FLAGS, order = 204)
    public boolean isCanMultiCol() {
        return canMultiCol;
    }

    @Property(category = CAT_FLAGS, order = 205)
    public boolean isOptionalKey() {
        return optionalKey;
    }

    @Property(category = CAT_FLAGS, order = 206)
    public boolean isSearchArray() {
        return searchArray;
    }

    @Property(category = CAT_FLAGS, order = 207)
    public boolean isSearchNulls() {
        return searchNulls;
    }

    @Property(category = CAT_FLAGS, order = 208)
    public boolean isStorage() {
        return storage;
    }

    @Property(category = CAT_FLAGS, order = 209)
    public boolean isClusterable() {
        return clusterable;
    }

    @Property(category = CAT_FLAGS, order = 210)
    public boolean isPredLocks() {
        return predLocks;
    }

    @Association
    public Collection<PostgreOperatorClass> getOperatorClasses(DBRProgressMonitor monitor) throws DBException {
        return operatorClassCache.getAllObjects(monitor, this);
    }

    public PostgreOperatorClass getOperatorClass(DBRProgressMonitor monitor, long oid) throws DBException {
        return PostgreUtils.getObjectById(monitor, operatorClassCache, this, oid);
    }

    @Association
    public Collection<PostgreOperatorFamily> getOperatorFamilies(DBRProgressMonitor monitor) throws DBException {
        return operatorFamilyCache.getAllObjects(monitor, this);
    }

    public PostgreOperatorFamily getOperatorFamily(DBRProgressMonitor monitor, long oid) throws DBException {
        return PostgreUtils.getObjectById(monitor, operatorFamilyCache, this, oid);
    }

    static class OperatorClassCache extends JDBCObjectCache<PostgreAccessMethod, PostgreOperatorClass> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreAccessMethod owner)
                throws SQLException
        {
            return session.prepareStatement(
                    "SELECT oc.oid,oc.* FROM pg_catalog.pg_opclass oc " +
                            "\nORDER BY oc.oid"
            );
        }

        @Override
        protected PostgreOperatorClass fetchObject(@NotNull JDBCSession session, @NotNull PostgreAccessMethod owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new PostgreOperatorClass(owner, dbResult);
        }
    }

    static class OperatorFamilyCache extends JDBCObjectCache<PostgreAccessMethod, PostgreOperatorFamily> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreAccessMethod owner)
                throws SQLException
        {
            return session.prepareStatement(
                    "SELECT of.oid,of.* FROM pg_catalog.pg_opfamily of " +
                            "\nORDER BY of.oid"
            );
        }

        @Override
        protected PostgreOperatorFamily fetchObject(@NotNull JDBCSession session, @NotNull PostgreAccessMethod owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new PostgreOperatorFamily(owner, dbResult);
        }
    }

}

