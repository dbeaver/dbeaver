/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class OracleHomeDescriptor extends JDBCClientHome
{
    static final Log log = LogFactory.getLog(OracleHomeDescriptor.class);

    private static final String JAR_OJDBC6 = "ojdbc6.jar";
    private static final String JAR_OJDBC5 = "ojdbc5.jar";
    private static final String JAR_OJDBC4 = "ojdbc14.jar";
    private static final String JAR_OJDBC2 = "classes12.zip";
    private static final String JAR_OJDBC2_ZIP = "classes12.jar";

    private Integer oraVersion; // short version (9, 10, 11...)
    private String fullOraVersion;
    private boolean isInstantClient;
    private String displayName;
    private List<String> tnsNames;

    public OracleHomeDescriptor(String oraHome)
    {
        super(CommonUtils.removeTrailingSlash(oraHome), oraHome);
        this.isInstantClient = OCIUtils.isInstantClient(oraHome);
        this.oraVersion = getOracleVersion();
        if (oraVersion == null) {
            log.warn("Unrecognized Oracle client version at " + oraHome);
        }
        this.displayName = OCIUtils.readWinRegistry(oraHome, OCIUtils.WIN_REG_ORA_HOME_NAME);
    }

    private Integer getOracleVersion()
    {
        File oraHome = getHomePath();
        File folder = isInstantClient ? oraHome : new File(oraHome, "bin");
        if (!folder.exists()) {
            return null;
        }
        for (int counter = 7; counter <= 15; counter++) {
            String dllName = System.mapLibraryName("ocijdbc" + counter);
            File ociLibFile = new File(folder, dllName);
            if (ociLibFile.exists()) {
                return counter;
            }
        }
        return null;
    }

    public Integer getOraVersion()
    {
        return oraVersion;
    }

    @Override
    public String getProductName()
    {
        return "Oracle" + (oraVersion == null ? "" : " " + oraVersion);
    }

    @Override
    public String getProductVersion()
    {
        if (fullOraVersion == null) {
            this.fullOraVersion = OCIUtils.getFullOraVersion(getHomeId(), isInstantClient);
            if (fullOraVersion == null) {
                fullOraVersion = "Unknown";
            }
        }
        return fullOraVersion;
    }

    public boolean isInstantClient()
    {
        return isInstantClient;
    }

    @Override
    public String getDisplayName()
    {
        if (displayName != null) {
            return displayName;
        }
        else {
            return getHomeId();
        }
    }

    public Collection<String> getOraServiceNames()
    {
        if (tnsNames == null) {
            tnsNames = OCIUtils.readTnsNames(getHomePath(), true);
        }
        return tnsNames;
    }

    private Collection<File> getRequiredJars()
    {
        List<File> list = new ArrayList<File>();
        FileFilter jarFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname)
            {
                final String name = pathname.getName();
                return name.endsWith(".jar") || name.endsWith(".zip");
            }
        };
        File oraHome = getHomePath();
        list.addAll(CommonUtils.safeArray(oraHome.listFiles(jarFilter)));
        File jdbcPath = new File(oraHome, "jdbc/lib");
        if (jdbcPath.exists()) {
            list.addAll(CommonUtils.safeArray(jdbcPath.listFiles(jarFilter)));
        }
        File libPath = new File(oraHome, "lib");
        if (libPath.exists()) {
            list.addAll(CommonUtils.safeArray(libPath.listFiles(jarFilter)));
        }
        Map<String, File> libMap = new TreeMap<String, File>();
        for (File lib : list) {
            libMap.put(lib.getName(), lib);
        }
        removeExtraLibraies(libMap, "_g");
        removeExtraLibraies(libMap, "dms");
        if (libMap.containsKey(JAR_OJDBC2)) {
            libMap.remove(JAR_OJDBC2_ZIP);
        }
        if (libMap.containsKey(JAR_OJDBC4)) {
            libMap.remove(JAR_OJDBC2);
            libMap.remove(JAR_OJDBC2_ZIP);
        }
        if (libMap.containsKey(JAR_OJDBC5)) {
            libMap.remove(JAR_OJDBC4);
            libMap.remove(JAR_OJDBC2);
            libMap.remove(JAR_OJDBC2_ZIP);
        }
        if (libMap.containsKey(JAR_OJDBC6)) {
            libMap.remove(JAR_OJDBC5);
            libMap.remove(JAR_OJDBC4);
            libMap.remove(JAR_OJDBC2);
            libMap.remove(JAR_OJDBC2_ZIP);
        }
/*
        switch (oraVersion) {
            case 8:
                list.add(oraHome + jdbcPathPrefix + "classes12.zip"); // JDBC drivers to connect to a 9i database. (JDK 1.2.x)
                list.add(oraHome + jdbcPathPrefix + "nls_charset12.zip"); // Additional National Language character set support
                break;
            case 9:
                list.add(oraHome + jdbcPathPrefix + "ojdbc14.jar"); // JDBC classes (JDK 1.4)
                list.add(oraHome + jdbcPathPrefix + "ocrs12.jar"); // Additional RowSet support  
                break;
            case 10:
                list.add(oraHome + jdbcPathPrefix + "ojdbc14.jar"); // JDBC classes (JDK 1.4 and 1.5)
                list.add(oraHome + libPath + "xml.jar");
                list.add(oraHome + libPath + "xmlcomp.jar");
                list.add(oraHome + libPath + "xmlcomp2.jar");
                list.add(oraHome + libPath + "xmlmesg.jar");
                list.add(oraHome + libPath + "xmlparserv2.jar");
                break;
            case 11:
                list.add(oraHome + jdbcPathPrefix + "ojdbc6.jar"); // Classes for use with JDK 1.6. It contains the JDBC driver classes except classes for NLS support in Oracle Object and Collection types.
                list.add(oraHome + jdbcPathPrefix + "ojdbc5.jar"); // Classes for use with JDK 1.5. It contains the JDBC driver classes, except classes for NLS support in Oracle Object and Collection types.
                list.add(oraHome + jdbcPathPrefix + "orai18n.jar"); //NLS classes for use with JDK 1.5, and 1.6. It contains classes for NLS support in Oracle Object and Collection types. This jar file replaces the old nls_charset jar/zip files.
                list.add(oraHome + libPath + "xml.jar");
                list.add(oraHome + libPath + "xmlcomp2.jar");
                list.add(oraHome + libPath + "xmlmesg.jar");
                list.add(oraHome + libPath + "xmlparserv2.jar");
                list.add(oraHome + libPath + "xschema.jar");
                list.add(oraHome + libPath + "xsu12.jar");
                break;
        }
*/
        return libMap.values();
    }

    private void removeExtraLibraies(Map<String, File> libMap, String suffix)
    {
        for (Iterator<String> nameIter = libMap.keySet().iterator(); nameIter.hasNext(); ) {
            String libName = nameIter.next();
            if (libName.endsWith(suffix + ".jar")) {
                if (libMap.containsKey(libName.substring(0, libName.length() - (suffix.length() + 4)) + ".jar")) {
                    nameIter.remove();
                }
            }
        }
    }

    /**
     * Returns an array of urls of jar-libraries required for the driver.
     */
    public URL[] getLibraries()
    {
        Collection<File> libraries = getRequiredJars();
        int i = 0;
        URL[] urls = new URL[libraries.size()];
        for (File file : libraries) {
            try {
                urls[i++] = file.toURI().toURL();
            } catch (MalformedURLException e) {
                log.warn("File '" + file.getAbsolutePath() + "' can't be converted to url.");
            }
        }
        return urls;
    }
}
