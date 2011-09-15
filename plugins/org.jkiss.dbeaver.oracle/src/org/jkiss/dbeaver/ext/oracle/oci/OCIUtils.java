/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.utils.WinRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OCIUtils {

    public static final String WIN_32 = "win32";

    private static void addOraHome(List<String> homes, String oraHome) {
        String sep = System.getProperty("file.separator");
        if (oraHome.endsWith(sep)) {
            oraHome = oraHome.substring(0, oraHome.length() - 1);
        }
        // file name case insensitivity on Windows platform
        if (Platform.getOS().equals(WIN_32)) {
            List<String> list = new ArrayList<String>();
            for (String s : homes) {
                list.add(s.toLowerCase());
            }
            if (!list.contains(oraHome.toLowerCase())) {
                homes.add(oraHome);
            }
        }
        else {
            homes.add(oraHome);
        }
    }

    /**
     * Searches Oracle home locations.
     * @return a list of Oracle home locations pathes
     */
    public static List<String> findOraHomes() {
        List<String> homes = new ArrayList<String>();
        String sep = System.getProperty("file.separator");

        // read system environment variables
        String oraHome = System.getenv("ORA_HOME");
        if (oraHome != null) {
            addOraHome(homes, oraHome);
        }

        String path = System.getenv("PATH");
        if (path != null) {
            for (String token : path.split(System.getProperty("path.separator"))) {
                if (token.toLowerCase().contains("oracle")) {
                    if (token.endsWith(sep)) {
                        token = token.substring(0, token.length() - 1);
                    }
                    if (token.toLowerCase().endsWith("bin")) {
                        oraHome = token.substring(0, token.length() - 3);
                        addOraHome(homes, oraHome);
                    }
                }
            }
        }

        // find Oracle homes in Windows registry
        if (Platform.getOS().equals(WIN_32)) {
            try {
                List<String> oracleKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE");
                for (String oracleKey : oracleKeys) {
                    Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE\\" + oracleKey);
                    for (String key : valuesMap.keySet()) {
                        if ("ORACLE_HOME".equals(key)) {
                            addOraHome(homes, valuesMap.get(key));
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
        // remove duplicate entries
        Set<String> s = new LinkedHashSet<String>(homes);
        homes.clear();
        homes.addAll(s);

        return homes;
    }

    /**
     * Reads TNS aliaces from TNSNAMES.ORA in specified Oracle home.
     * @param oraHome path of Oracle home location
     * @return TNS aliases list
     */
    public static ArrayList<String> getOraServiceNames(String oraHome) {
        ArrayList<String> aliases = new ArrayList<String>();

        // parse TNSNAMES.ORA file
        if (oraHome != null) {
            String sep = System.getProperty("file.separator");
            if (!oraHome.endsWith(sep)) {
                oraHome += sep;
            }
            String tnsnamesPath = oraHome + "Network" + sep + "Admin" + sep + "TNSNAMES.ORA";
            File tnsnamesOra = new File (tnsnamesPath);
            if (tnsnamesOra != null && tnsnamesOra.exists()) {
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
        }
        Collections.sort(aliases);
        return aliases;
    }

    public static boolean isOciDriver(DBPDriver driver) {
        return "oracle_oci".equals(driver.getId());
    }
}
