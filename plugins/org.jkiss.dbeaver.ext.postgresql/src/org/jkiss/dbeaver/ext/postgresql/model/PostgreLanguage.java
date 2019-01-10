/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

