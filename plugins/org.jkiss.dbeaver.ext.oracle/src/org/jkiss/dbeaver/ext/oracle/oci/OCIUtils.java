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

package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.util.*;

public class OCIUtils
{
    private static final Log log = Log.getLog(OCIUtils.class);

    public static final String WIN_REG_ORACLE = "SOFTWARE\\ORACLE";
    public static final String WIN_REG_ORA_HOME = "ORACLE_HOME";
    public static final String WIN_REG_ORA_HOME_NAME = "ORACLE_HOME_NAME";

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
            if (equalsFileName(home.getName(), oraHome)) {
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
            if (equalsFileName(home.getName(), oraHome)) {
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
        String path = System.getenv(OracleConstants.VAR_PATH);
        if (path != null) {
            for (String token : path.split(System.getProperty(StandardConstants.ENV_PATH_SEPARATOR))) {
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

        String oraHome = System.getenv(OracleConstants.VAR_ORA_HOME);
        if (oraHome == null) {
            oraHome = System.getenv(OracleConstants.VAR_ORACLE_HOME);
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

    @Nullable
    public static File findTnsNamesFile(@Nullable File oraHome, boolean checkTnsAdmin)
    {
        File tnsNamesFile = null;
        if (checkTnsAdmin) {
            String tnsAdmin = System.getenv(OracleConstants.VAR_TNS_ADMIN);
            if (tnsAdmin != null) {
                tnsNamesFile = new File (tnsAdmin, TNSNAMES_FILE_NAME);
            }
        }
        if ((tnsNamesFile == null || !tnsNamesFile.exists()) && oraHome != null) {
            tnsNamesFile = new File (oraHome, TNSNAMES_FILE_PATH + TNSNAMES_FILE_NAME);
            if (!tnsNamesFile.exists()) {
                tnsNamesFile = new File (oraHome, TNSNAMES_FILE_NAME);
            }
        }
        if (tnsNamesFile != null && tnsNamesFile.exists()) {
            return tnsNamesFile;
        } else {
            return null;
        }
    }

    /**
     * Reads TNS names from a specified Oracle home or system variable TNS_ADMIN.
     */
    public static Map<String, String> readTnsNames(@Nullable File oraHome, boolean checkTnsAdmin)
    {
        File tnsNamesFile = findTnsNamesFile(oraHome, checkTnsAdmin);
        if (tnsNamesFile != null) {
            return parseTnsNames(tnsNamesFile);
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Reads TNS names from a specified file.
     */
    private static Map<String, String> parseTnsNames(File tnsnamesOra)
    {
        Map<String, String> aliases = new TreeMap<>();

        if (tnsnamesOra.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tnsnamesOra));
                StringBuilder tnsDescription = new StringBuilder();
                String curAlias = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") ) {
                        continue;
                    }
                    if (!line.startsWith(" ") && !line.startsWith("\t") && !line.startsWith("(") && line.contains("=")) {
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
                            aliases.putAll(parseTnsNames(extFile));
                        } else {
                            if (curAlias != null) {
                                aliases.put(curAlias, getPlainTnsDescription(tnsDescription.toString()));
                            }
                            curAlias = alias.trim();
                            tnsDescription.setLength(0);
                            tnsDescription.append(line.substring(divPos + 1));
                        }
                    } else {
                        if (curAlias != null) {
                            tnsDescription.append(line);
                        }
                    }
                }
                if (curAlias != null) {
                    aliases.put(curAlias, getPlainTnsDescription(tnsDescription.toString()));
                }

            } catch (IOException e) {
                // do nothing
                log.debug(e);
            }
        } else {
            log.debug("TNS names file '" + tnsnamesOra + "' doesn't exist");
        }
        return aliases;
    }

    private static String getPlainTnsDescription(String line) {
        return CommonUtils.compactWhiteSpaces(line.trim());
    }

    public static boolean isInstantClient(String oraHome)
    {
        File root = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "oci"));
        File bin = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "bin/" + "oci"));
        return root.exists() && !bin.exists();
    }

}
