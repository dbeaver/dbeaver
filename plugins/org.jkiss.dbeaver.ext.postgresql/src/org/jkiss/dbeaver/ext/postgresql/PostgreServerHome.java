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

    static final Log log = Log.getLog(PostgreServerHome.class);

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
/*
    private String getFullServerVersion()
    {
        String cmd = new File(
            new File(getHomePath(), "bin"),
            MySQLUtils.getMySQLConsoleBinaryName()).getAbsolutePath();

        try {
            Process p = Runtime.getRuntime().exec(new String[] {cmd, "-V"});
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    String line;
                    while ((line = input.readLine()) != null) {
                        int pos = line.indexOf("Distrib ");
                        if (pos != -1) {
                            pos += 8;
                            int pos2 = line.indexOf(",", pos);
                            return line.substring(pos, pos2);
                        }
                    }
                } finally {
                    IOUtils.close(input);
                }
            } finally {
                p.destroy();
            }
        }
        catch (Exception ex) {
            log.warn("Error reading MySQL server version from " + cmd, ex);
        }
        return null;
    }
*/
}
