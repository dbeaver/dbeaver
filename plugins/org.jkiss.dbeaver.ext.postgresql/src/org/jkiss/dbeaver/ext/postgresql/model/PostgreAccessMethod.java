/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreAccessMethod
 */
public class PostgreAccessMethod extends PostgreInformation {

    public static final String CAT_ROUTINES = "Routines";
    public static final String CAT_FLAGS = "Flags";

    private long oid;
    private String name;
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

}

