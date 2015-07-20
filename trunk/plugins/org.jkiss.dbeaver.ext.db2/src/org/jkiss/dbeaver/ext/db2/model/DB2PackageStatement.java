/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.io.UnsupportedEncodingException;
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
        try {
            this.uniqueId = new String(JDBCUtils.safeGetBytes(resultSet, "UNIQUE_ID"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
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
