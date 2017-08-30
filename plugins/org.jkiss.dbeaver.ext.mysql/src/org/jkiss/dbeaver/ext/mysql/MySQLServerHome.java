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
package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;
import org.jkiss.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * MySQLServerHome
 */
public class MySQLServerHome extends JDBCClientHome {

    private static final Log log = Log.getLog(MySQLServerHome.class);

    private String name;
    private String version;

    protected MySQLServerHome(String path, String name)
    {
        super(path, path);
        this.name = name == null ? path : name;
    }

    @Override
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public String getProductName() throws DBException
    {
        return "MySQL";
    }

    @Override
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
        File binPath = getHomePath();
        File binSubfolder = new File(binPath, "bin");
        if (binSubfolder.exists()) {
            binPath = binSubfolder;
        }

        String cmd = new File(
            binPath,
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
