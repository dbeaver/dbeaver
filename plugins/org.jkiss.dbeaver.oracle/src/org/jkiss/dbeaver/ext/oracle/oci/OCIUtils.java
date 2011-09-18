/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OCIUtils
{
    static final Log log = LogFactory.getLog(OCIUtils.class);

    public static final String WIN_32 = "win32";

    /**
     * A list of Oracle client homes found in the system.
     * The first one is always a current Oracle home (from PATH) 
     */
    public static final List<OracleHomeDescriptor> oraHomes =
            new ArrayList<OracleHomeDescriptor>();

    static {
        findOraHomes();
    }

    public static OracleHomeDescriptor getOraHome(String oraHome) {
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getOraHome(), oraHome)) {
                return home;
            }
        }
        return null;
    }

    private static boolean equalsFileName(String file1, String file2) {
        if (Platform.getOS().equals(WIN_32)) {
            return file1.equalsIgnoreCase(file2);
        }
        else {
            return file1.equals(file2);
        }
    }

    public static void addOraHome(String oraHome, boolean isDefault)
    {
        oraHome = CommonUtils.removeSplashFileName(oraHome);

        boolean contains = false;
        for (OracleHomeDescriptor home : oraHomes) {
            // file name case insensitivity on Windows platform
            if (equalsFileName(home.getOraHome(), oraHome)) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            oraHomes.add(new OracleHomeDescriptor(oraHome, isDefault));
        }
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
                    token = CommonUtils.removeSplashFileName(token);
                    if (token.toLowerCase().endsWith("bin")) {
                        addOraHome(token.substring(0, token.length() - 3), true);
                    }
                }
            }
        }

        String oraHome = System.getenv("ORA_HOME");
        if (oraHome != null) {
            addOraHome(oraHome, false);
        }

        // find Oracle homes in Windows registry
        if (Platform.getOS().equals(WIN_32)) {
            try {
                List<String> oracleKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE");
                for (String oracleKey : oracleKeys) {
                    Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE\\" + oracleKey);
                    for (String key : valuesMap.keySet()) {
                        if ("ORACLE_HOME".equals(key)) {
                            addOraHome(valuesMap.get(key), false);
                            break;
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                // do nothing
            } catch (InvocationTargetException e) {
                // do nothing
            }
        }
    }

    public static boolean isOciDriver(DBPDriver driver)
    {
        return "oracle_oci".equals(driver.getId());
    }

    public static Integer getOracleVersion(String oraHome)
    {
        oraHome = CommonUtils.addSplashFileName(oraHome);
        File binFolder = new File(oraHome + "/BIN");
        if (binFolder != null && binFolder.exists()) {
            for (int counter = 1; counter <= 12; counter++) {
                File oraclient_dll = new File(binFolder, "oraclient" + counter +".dll");
                if (oraclient_dll != null && oraclient_dll.exists()) {
                    return counter;
                }
            }
        }
        else {
            log.warn("BIN folder isn't found in Oracle home " + oraHome);
        }
        return null;
    }

    /**
     * Returns an installed Oracle client full version (ora home is from PATH)
     * @return
     */
    public static String getOracleClientVersion()
    {
        String version = null;
        try {
            String line;
            Process p = Runtime.getRuntime().exec("sqlplus.exe -version");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (line.startsWith("SQL*Plus: Release ") && line.endsWith(" - Production")) {
                    version = line.substring(18, line.length() - 13);
                    break;
                }
            }
            input.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return version;
    }
}
