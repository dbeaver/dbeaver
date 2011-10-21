/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OCIUtils
{
    static final Log log = LogFactory.getLog(OCIUtils.class);

    public static final String WIN_REG_ORACLE = "SOFTWARE\\ORACLE";
    public static final String WIN_REG_ORA_HOME = "ORACLE_HOME";
    public static final String WIN_REG_ORA_HOME_NAME = "ORACLE_HOME_NAME";

    /**
     * A list of Oracle client homes found in the system.
     * The first one is always a current Oracle home (from PATH) 
     */
    private static final List<OracleHomeDescriptor> oraHomes = new ArrayList<OracleHomeDescriptor>();
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
        if (CommonUtils.isEmpty(oraHome)) {
            return null;
        }
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getOraHome(), oraHome)) {
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
            if (equalsFileName(home.getOraHomeName(), oraHomeName)) {
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
            if (equalsFileName(home.getOraHome(), oraHome)) {
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
     * @return a list of Oracle home locations pathes
     */
    private static void findOraHomes()
    {
        // read system environment variables
        String path = System.getenv("PATH");
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

        String oraHome = System.getenv("ORA_HOME");
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
            } catch (IllegalAccessException e) {
                log.warn("Error reading Windows registry", e);
            } catch (InvocationTargetException e) {
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
            } catch (IllegalAccessException e) {
                log.warn("Error reading Windows registry", e);
            } catch (InvocationTargetException e) {
                log.warn("Error reading Windows registry", e);
            }
        }
        return null;
    }

    public static boolean isOciDriver(DBPDriver driver)
    {
        return "oracle_oci".equals(driver.getId());
    }

    public static Integer getOracleVersion(String oraHome, boolean isInstantClient)
    {
        oraHome = CommonUtils.makeDirectoryName(oraHome);
        File folder = new File(isInstantClient ? oraHome : oraHome + "/BIN");
        if (!folder.exists()) {
            return null;
        }
        for (int counter = 1; counter <= 12; counter++) {
            String dllName = System.mapLibraryName((isInstantClient ? "oraociei" : "oraclient") + counter);
            File oraclient_dll = new File(folder, dllName);
            if (oraclient_dll.exists()) {
                return counter;
            }
        }
        return null;
    }

    /**
     * Returns an installed Oracle client full version
     * @return
     */
    public static String getFullOraVersion(String oraHome, boolean isInstantClient)
    {
        String version = null;
        String sqlplus = 
                (isInstantClient ? CommonUtils.makeDirectoryName(oraHome) : CommonUtils.makeDirectoryName(oraHome) + "BIN/") +
                        "sqlplus -version";
        try {
            Process p = Runtime.getRuntime().exec(sqlplus);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("SQL*Plus: Release ")) {
                    version = line.substring(18, line.indexOf(" ", 19));
                    break;
                }
            }
            input.close();
        }
        catch (Exception ex) {
            log.warn("Error reading Oracle client version from " + sqlplus, ex);
        }
        return version;
    }

    /**
     * Reads TNS names from a specified Oracle home or system variable TNS_ADMIN.
     */
    public static List<String> readTnsNames(String oraHome, boolean checkTnsAdmin)
    {
        File tnsNamesFile = null;
        if (checkTnsAdmin) {
            String tnsAdmin = System.getenv("TNS_ADMIN");
            if (tnsAdmin != null) {
                tnsNamesFile = new File (CommonUtils.removeTrailingSlash(tnsAdmin) + "/TNSNAMES.ORA");
            }
        }
        if ((tnsNamesFile == null || !tnsNamesFile.exists()) && oraHome != null) {
            tnsNamesFile = new File (oraHome + "/Network/Admin/TNSNAMES.ORA");
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
        ArrayList<String> aliases = new ArrayList<String>();

        File tnsnamesOra = new File (tnsnamesPath);
        if (tnsnamesOra.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(tnsnamesOra));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty() && !line.startsWith(" ") && !line.startsWith("#") && line.contains(" =")) {
                        aliases.add(line.substring(0, line.indexOf(" =")));
                    }
                }
            } catch (FileNotFoundException e) {
                // do nothing
            } catch (IOException e) {
                // do nothing
            }
        }
        else {
            // do nothing
        }
        Collections.sort(aliases);
        return aliases;
    }

    public static boolean isInstantClient(String oraHome)
    {
        File root = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "oci"));
        File bin = new File(System.mapLibraryName(CommonUtils.makeDirectoryName(oraHome) + "BIN/" + "oci"));
        return root.exists() && !bin.exists();
    }
}
