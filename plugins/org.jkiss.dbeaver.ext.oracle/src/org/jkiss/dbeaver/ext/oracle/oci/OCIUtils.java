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

package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OCIUtils
{
    static final Log log = Log.getLog(OCIUtils.class);

    public static final String WIN_REG_ORACLE = "SOFTWARE\\ORACLE";
    public static final String WIN_REG_ORA_HOME = "ORACLE_HOME";
    public static final String WIN_REG_ORA_HOME_NAME = "ORACLE_HOME_NAME";

    public static final String VAR_ORA_HOME = "ORA_HOME";
    public static final String VAR_ORACLE_HOME = "ORACLE_HOME";
    public static final String VAR_TNS_ADMIN = "TNS_ADMIN";
    public static final String VAR_PATH = "PATH";
    public static final String TNSNAMES_FILE_NAME = "tnsnames.ora";
    public static final String TNSNAMES_FILE_PATH = "network/admin/";

    //public static final String DRIVER_NAME_OCI = "oracle_oci";

    /**
     * A list of Oracle client homes found in the system.
     * The first one is always a current Oracle home (from PATH) 
     */
    private static final List<OracleHomeDescriptor> oraHomes = new ArrayList<>();
    private static boolean oraHomesSearched = false;

/*
    static {
        findOraHomes();
    }
*/

    public static List<OracleHomeDescriptor> getOraHomes()
    {
        checkOraHomes();
        return oraHomes;
    }

    private static boolean checkOraHomes() {
        if (!oraHomesSearched) {
            findOraHomes();
            oraHomesSearched = true;
        }
        return !oraHomes.isEmpty();
    }

    public static OracleHomeDescriptor getOraHome(String oraHome) {
        if (CommonUtils.isEmpty(oraHome) || !checkOraHomes()) {
            return null;
        }
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getHomeId(), oraHome)) {
                return home;
            }
        }
        return null;
    }

    public static OracleHomeDescriptor getOraHomeByName(String oraHomeName) {
        if (CommonUtils.isEmpty(oraHomeName) || !checkOraHomes()) {
            return null;
        }
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getDisplayName(), oraHomeName)) {
                return home;
            }
        }
        return null;
    }

    private static boolean equalsFileName(String file1, String file2) {
        if (DBeaverCore.getInstance().getLocalSystem().isWindows()) {
            return file1.equalsIgnoreCase(file2);
        }
        else {
            return file1.equals(file2);
        }
    }

    public static OracleHomeDescriptor addOraHome(String oraHome) throws DBException
    {
        if (CommonUtils.isEmpty(oraHome)) {
            return null;
        }
        oraHome = CommonUtils.removeTrailingSlash(oraHome);

        boolean contains = false;
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getHomeId(), oraHome)) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            OracleHomeDescriptor homeDescriptor = new OracleHomeDescriptor(oraHome);
            oraHomes.add(0, homeDescriptor);
            return homeDescriptor;
        }
        return null;
    }

    /**
     * Searches Oracle home locations.
     */
    private static void findOraHomes()
    {
        // read system environment variables
        String path = System.getenv(VAR_PATH);
        if (path != null) {
            for (String token : path.split(System.getProperty("path.separator"))) {
                if (token.toLowerCase().contains("oracle")) {
                    token = CommonUtils.removeTrailingSlash(token);
                    if (token.toLowerCase().endsWith("bin")) {
                        String oraHome = token.substring(0, token.length() - 3);
                        try {
                            addOraHome(oraHome);
                        } catch (DBException ex) {
                            log.warn("Wrong Oracle client home " + oraHome, ex);
                        }
                    }
                }
            }
        }

        String oraHome = System.getenv(VAR_ORA_HOME);
        if (oraHome == null) {
            oraHome = System.getenv(VAR_ORACLE_HOME);
        }
        if (oraHome != null) {
            try {
                addOraHome(oraHome);
            } catch (DBException ex) {
                log.warn("Wrong Oracle client home " + oraHome, ex);
            }
        }

        // find Oracle homes in Windows registry
        if (DBeaverCore.getInstance().getLocalSystem().isWindows()) {
            try {
                List<String> oracleKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, WIN_REG_ORACLE);
                if (oracleKeys != null) {
                    for (String oracleKey : oracleKeys) {
                        Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, WIN_REG_ORACLE + "\\" + oracleKey);
                        if (valuesMap != null) {
                            for (String key : valuesMap.keySet()) {
                                if (WIN_REG_ORA_HOME.equals(key)) {
                                    try {
                                        oraHome = valuesMap.get(key);
                                        addOraHome(oraHome);
                                    } catch (DBException ex) {
                                        log.warn("Wrong Oracle client home " + oraHome, ex);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error reading Windows registry", e);
            }
        }
    }

    public static String readWinRegistry(String oraHome, String name) {
        if (DBeaverCore.getInstance().getLocalSystem().isWindows()) {
            try {
                List<String> oracleKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, WIN_REG_ORACLE);
                if (oracleKeys != null) {
                    for (String oracleKey : oracleKeys) {
                        String home = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, WIN_REG_ORACLE + "\\" + oracleKey, WIN_REG_ORA_HOME);
                        if (oraHome.equals(home)) {
                            String value = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, WIN_REG_ORACLE + "\\" + oracleKey, name);
                            if (value != null) {
                                return value;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error reading Windows registry", e);
            }
        }
        return null;
    }

/*
    public static boolean isOciDriver(DBPDriver driver)
    {
        return DRIVER_NAME_OCI.equals(driver.getId());
    }
*/

    /**
     * Returns an installed Oracle client full version
     * @return oracle version
     */
    public static String getFullOraVersion(String oraHome, boolean isInstantClient)
    {
        String sqlplus =
                (isInstantClient ? CommonUtils.makeDirectoryName(oraHome) : CommonUtils.makeDirectoryName(oraHome) + "bin/") +
                        "sqlplus -version";
        try {
            Process p = Runtime.getRuntime().exec(sqlplus);
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (line.startsWith("SQL*Plus: Release ")) {
                            return line.substring(18, line.indexOf(" ", 19));
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
            log.warn("Error reading Oracle client version from " + sqlplus, ex);
        }
        return null;
    }

    /**
     * Reads TNS names from a specified Oracle home or system variable TNS_ADMIN.
     */
    public static List<String> readTnsNames(@Nullable File oraHome, boolean checkTnsAdmin)
    {
        File tnsNamesFile = null;
        if (checkTnsAdmin) {
            String tnsAdmin = System.getenv(VAR_TNS_ADMIN);
            if (tnsAdmin != null) {
                tnsNamesFile = new File (CommonUtils.removeTrailingSlash(tnsAdmin) + "/" + TNSNAMES_FILE_NAME);
            }
        }
        if ((tnsNamesFile == null || !tnsNamesFile.exists()) && oraHome != null) {
            tnsNamesFile = new File (oraHome, TNSNAMES_FILE_PATH + TNSNAMES_FILE_NAME);
        }
        if (tnsNamesFile != null && tnsNamesFile.exists()) {
            return parseTnsNames(tnsNamesFile.getAbsolutePath());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Reads TNS names from a specified file.
     */
    public static List<String> parseTnsNames(String tnsnamesPath)
    {
        ArrayList<String> aliases = new ArrayList<>();

        File tnsnamesOra = new File (tnsnamesPath);
        if (tnsnamesOra.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tnsnamesOra));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("\t") && !line.startsWith("(") && !line.startsWith("#") && line.contains("=")) {
                        final int divPos = line.indexOf("=");
                        if (divPos < 0) {
                            continue;
                        }
                        final String alias = line.substring(0, divPos);
                        if (alias.equalsIgnoreCase("IFILE")) {
                            String filePath = line.substring(divPos + 1).trim();
                            File extFile = new File(filePath);
                            if (!extFile.exists()) {
                                extFile = new File(tnsnamesOra.getParent(), filePath);
                            }
                            aliases.addAll(parseTnsNames(extFile.getAbsolutePath()));
                        } else {
                            aliases.add(alias.trim());
                        }
                    }
                }
            } catch (IOException e) {
                // do nothing
                log.debug(e);
            }
        }
        else {
            // do nothing
            log.debug("TNS names file '" + tnsnamesOra + "' doesn't exist");
        }
        Collections.sort(aliases);
        return aliases;
    }

    public static boolean isInstantClient(String oraHome)
    {
        File root = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "oci"));
        File bin = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "bin/" + "oci"));
        return root.exists() && !bin.exists();
    }

}
