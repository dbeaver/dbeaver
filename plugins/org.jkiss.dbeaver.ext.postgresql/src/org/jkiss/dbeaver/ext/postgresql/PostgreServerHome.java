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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;

/**
 * PostgreServerHome
 */
public class PostgreServerHome extends JDBCClientHome {

    private static final Log log = Log.getLog(PostgreServerHome.class);

    private String name;
    private String version;
    private String branding;
    private String dataDirectory;

    protected PostgreServerHome(String id, String path, String version, String branding, String dataDirectory)
    {
        super(id, path);
        this.name = branding == null ? id : branding;
        this.version = version;
        this.branding = branding;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public String getProductName() throws DBException
    {
        return branding;
    }

    @Override
    public String getProductVersion() throws DBException
    {
        return version;
    }

    public String getBranding() {
        return branding;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

}
