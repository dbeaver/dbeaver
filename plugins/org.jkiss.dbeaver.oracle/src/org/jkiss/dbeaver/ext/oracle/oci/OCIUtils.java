/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.utils.WinRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OCIUtils
{
    static final Log log = LogFactory.getLog(OCIUtils.class);

    public static final String WIN_32 = "win32";

    private static void addOraHome(List<String> homes, String oraHome)
    {
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
    public static List<String> findOraHomes()
    {
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
    public static ArrayList<String> getOraServiceNames(String oraHome)
    {
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

    public static boolean isOciDriver(DBPDriver driver)
    {
        return "oracle_oci".equals(driver.getId());
    }

    private static List<String> getRequiredJars(String oraHome)
    {
        String sep = System.getProperty("file.separator");
        if (!oraHome.endsWith(sep)) {
            oraHome = oraHome + sep;
        }
        List<String> list = new ArrayList<String>();
        Integer oracleVersion = getOracleVersion(oraHome);
        String jdbcDriverJar;
        switch (oracleVersion) {
            case 8:
                list.add(oraHome + "jdbc/lib/classes12.zip"); // JDBC drivers to connect to a 9i database. (JDK 1.2.x)
                list.add(oraHome + "jdbc/lib/nls_charset12.zip"); // Additional National Language character set support 
                break;
            case 9:
                list.add(oraHome + "jdbc/lib/ojdbc14.jar"); // JDBC classes (JDK 1.4)
                list.add(oraHome + "jdbc/lib/ocrs12.jar "); // Additional RowSet support  
                break;
            case 10:
                list.add(oraHome + "jdbc/lib/ojdbc14.jar"); // JDBC classes (JDK 1.4 and 1.5)
                list.add(oraHome + "lib/xml.jar");
                list.add(oraHome + "lib/xmlcomp.jar");
                list.add(oraHome + "lib/xmlcomp2.jar");
                list.add(oraHome + "lib/xmlmesg.jar");
                list.add(oraHome + "lib/xmlparserv2.jar");
                break;
            case 11:
                list.add(oraHome + "jdbc/lib/ojdbc6.jar"); // Classes for use with JDK 1.6. It contains the JDBC driver classes except classes for NLS support in Oracle Object and Collection types.
                //addDriverJar2list(list, oraHome, "ojdbc5.zip"); // Classes for use with JDK 1.5. It contains the JDBC driver classes, except classes for NLS support in Oracle Object and Collection types.
                list.add(oraHome + "jdbc/lib/orai18n.jar"); //NLS classes for use with JDK 1.5, and 1.6. It contains classes for NLS support in Oracle Object and Collection types. This jar file replaces the old nls_charset jar/zip files.
                list.add(oraHome + "lib/xml.jar");
                list.add(oraHome + "lib/xmlcomp.jar");
                list.add(oraHome + "lib/xmlcomp2.jar");
                list.add(oraHome + "lib/xmlmesg.jar");
                list.add(oraHome + "lib/xmlparserv2.jar");
                break;
        }
        return list;
    }

    public static URL[] getLibraries(String oraHome)
    {
        List<String> libraries = getRequiredJars(oraHome);
        List<File> files = new ArrayList<File>();
        for (String library : libraries) {
            File file  = new File(library);
            if (file != null && file.exists()) {
                files.add(file);
            }
            else {
                log.warn("Driver file '" + library + "' doesn't exist.");
            }
        }
        int i = 0;
        URL[] urls = new URL[files.size()];
        for (File file : files) {
            try {
                urls[i++] = file.toURI().toURL();
            } catch (MalformedURLException e) {
                log.warn("File '" + file.getAbsolutePath() + "' can't be converted to url.");
            }
        }
        return urls;
    }

    public static Integer getOracleVersion(String oraHome)
    {
        String sep = System.getProperty("file.separator");
        if (!oraHome.endsWith(sep)) {
            oraHome = oraHome + sep;
        }
        File binFolder = new File(oraHome + sep + "BIN");
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

}
