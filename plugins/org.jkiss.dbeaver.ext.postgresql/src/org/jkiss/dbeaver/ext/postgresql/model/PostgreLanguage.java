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
 * PostgreCharset
 */
public class PostgreLanguage extends PostgreInformation {

    private long oid;
    private String name;
    private long ownerId;
    private boolean userDefined;
    private boolean trusted;
    private String handlerId;
    private String inlineHandlerId;
    private String validatorId;

    public PostgreLanguage(PostgreDatabase database, ResultSet dbResult)
        throws SQLException
    {
        super(database);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "lanname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "lanowner");
        this.userDefined = JDBCUtils.safeGetBoolean(dbResult, "lanispl");
        this.trusted = JDBCUtils.safeGetBoolean(dbResult, "lanpltrusted");
        this.handlerId = JDBCUtils.safeGetString(dbResult, "lanplcallfoid");
        this.inlineHandlerId = JDBCUtils.safeGetString(dbResult, "laninline");
        this.validatorId = JDBCUtils.safeGetString(dbResult, "lanvalidator");
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
    public long getOwnerId() {
        return ownerId;
    }

    @Property(order = 10)
    public boolean isUserDefined() {
        return userDefined;
    }

    @Property(order = 11)
    public boolean isTrusted() {
        return trusted;
    }

    @Property(order = 12)
    public String getHandlerId() {
        return handlerId;
    }

    @Property(order = 13)
    public String getInlineHandlerId() {
        return inlineHandlerId;
    }

    @Property(order = 14)
    public String getValidatorId() {
        return validatorId;
    }
}

