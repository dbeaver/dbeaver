/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;

/**
 * DB2 Package Statement
 * 
 * @author Denis Forveille
 */
public class DB2PackageStatement extends DB2Object<DB2Package> {

    private static final int MAX_LENGTH_TEXT = 132;

    private Integer lineNumber;
    private String text;
    private String uniqueId;
    private String version;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2PackageStatement(DB2Package db2Package, ResultSet resultSet) throws DBException
    {
        super(db2Package, String.valueOf(JDBCUtils.safeGetInteger(resultSet, "SECTNO")), true);

        this.lineNumber = JDBCUtils.safeGetInteger(resultSet, "STMTNO");
        this.text = JDBCUtils.safeGetString(resultSet, "TEXT");
        this.version = JDBCUtils.safeGetString(resultSet, "VERSION");
        this.uniqueId = new String(JDBCUtils.safeGetBytes(resultSet, "UNIQUE_ID"), StandardCharsets.UTF_8);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public Integer getLineNumber()
    {
        return lineNumber;
    }

    @Property(viewable = true, order = 3)
    public String getUniqueId()
    {
        return uniqueId;
    }

    @Property(viewable = true, order = 4)
    public String getVersion()
    {
        return version;
    }

    @Property(viewable = true, order = 5)
    public String getTextPreview()
    {
        return text.substring(0, Math.min(MAX_LENGTH_TEXT, text.length()));
    }

    @Property(viewable = false, order = 6)
    public String getText()
    {
        return text;
    }

}
