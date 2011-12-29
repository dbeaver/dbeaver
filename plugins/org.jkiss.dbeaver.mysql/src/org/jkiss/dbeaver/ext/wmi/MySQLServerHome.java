/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * MySQLServerHome
 */
public class MySQLServerHome extends JDBCClientHome {

    static final Log log = LogFactory.getLog(MySQLServerHome.class);

    private String name;
    private String version;

    protected MySQLServerHome(String path, String name)
    {
        super(path, path);
        this.name = name == null ? path : name;
    }

    public String getDisplayName()
    {
        return name;
    }

    public String getProductName() throws DBException
    {
        return "MySQL";
    }

    public String getProductVersion() throws DBException
    {
        if (version == null) {
            this.version = getFullServerVersion();
            if (version == null) {
                version = "Unknown";
            }
        }
        return version;
    }

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
}
