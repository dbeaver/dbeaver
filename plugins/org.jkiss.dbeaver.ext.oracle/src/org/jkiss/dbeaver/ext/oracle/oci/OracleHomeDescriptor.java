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
package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCNativeClientLocation;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OracleHomeDescriptor extends JDBCNativeClientLocation
{
    private static final Log log = Log.getLog(OracleHomeDescriptor.class);

/*
    private static final String JAR_OJDBC6 = "ojdbc6.jar";
    private static final String JAR_OJDBC5 = "ojdbc5.jar";
    private static final String JAR_OJDBC4 = "ojdbc14.jar";
    private static final String JAR_OJDBC2 = "classes12.zip";
    private static final String JAR_OJDBC2_ZIP = "classes12.jar";
*/

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
            log.debug("Unrecognized Oracle client version at " + oraHome);
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
            tnsNames = new ArrayList<>(OCIUtils.readTnsNames(getHomePath(), true).keySet());
        }
        return tnsNames;
    }

/*
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
     *
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
*/
}
