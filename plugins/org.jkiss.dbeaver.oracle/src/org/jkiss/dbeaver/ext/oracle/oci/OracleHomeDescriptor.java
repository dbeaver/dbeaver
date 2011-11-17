/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCClientHome;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OracleHomeDescriptor extends JDBCClientHome
{
    static final Log log = LogFactory.getLog(OracleHomeDescriptor.class);

    private Integer oraVersion; // short version (9, 10, 11...)
    private String fullOraVersion;
    private boolean isInstantClient;
    private String displayName;
    private List<String> tnsNames;

    public OracleHomeDescriptor(String oraHome)
    {
        super(CommonUtils.removeTrailingSlash(oraHome), oraHome);
        this.isInstantClient = OCIUtils.isInstantClient(oraHome);
        this.oraVersion = OCIUtils.getOracleVersion(oraHome, isInstantClient);
        if (oraVersion == null) {
            log.error("Unrecognized Oracle client version at " + oraHome);
        }
        this.displayName = OCIUtils.readWinRegistry(oraHome, OCIUtils.WIN_REG_ORA_HOME_NAME);
    }

    public Integer getOraVersion()
    {
        return oraVersion;
    }

    public String getProductName()
    {
        return "Oracle" + (oraVersion == null ? "" : " " + oraVersion);
    }

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

    public String getDisplayName()
    {
        if (displayName != null) {
            return displayName;
        }
        else {
            return getHomeId();
        }
    }

    public List<String> getOraServiceNames()
    {
        if (tnsNames == null) {
            tnsNames = OCIUtils.readTnsNames(getHomePath(), true);
        }
        return tnsNames;
    }

    private List<String> getRequiredJars()
    {
        List<String> list = new ArrayList<String>();
        String jdbcDriverJar;
        String jdbcPathPrefix = isInstantClient ? "/" : "/jdbc/lib/";
        String libPathPrefix = isInstantClient ? "/" : "/lib/";
        String oraHome = getHomeId();
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
                list.add(oraHome + libPathPrefix + "xml.jar");
                list.add(oraHome + libPathPrefix + "xmlcomp.jar");
                list.add(oraHome + libPathPrefix + "xmlcomp2.jar");
                list.add(oraHome + libPathPrefix + "xmlmesg.jar");
                list.add(oraHome + libPathPrefix + "xmlparserv2.jar");
                break;
            case 11:
                list.add(oraHome + jdbcPathPrefix + "ojdbc6.jar"); // Classes for use with JDK 1.6. It contains the JDBC driver classes except classes for NLS support in Oracle Object and Collection types.
                //addDriverJar2list(/list, oraHome, "ojdbc5.zip"); // Classes for use with JDK 1.5. It contains the JDBC driver classes, except classes for NLS support in Oracle Object and Collection types.
                list.add(oraHome + jdbcPathPrefix + "orai18n.jar"); //NLS classes for use with JDK 1.5, and 1.6. It contains classes for NLS support in Oracle Object and Collection types. This jar file replaces the old nls_charset jar/zip files.
                list.add(oraHome + libPathPrefix + "xml.jar");
                list.add(oraHome + libPathPrefix + "xmlcomp2.jar");
                list.add(oraHome + libPathPrefix + "xmlmesg.jar");
                list.add(oraHome + libPathPrefix + "xmlparserv2.jar");
                list.add(oraHome + libPathPrefix + "xschema.jar");
                list.add(oraHome + libPathPrefix + "xsu12.jar");
                break;
        }
        return list;
    }

    /**
     * Returns an array of urls of jar-libraries required for the driver.
     */
    public URL[] getLibraries()
    {
        List<String> libraries = getRequiredJars();
        List<File> files = new ArrayList<File>();
        for (String library : libraries) {
            File file  = new File(library);
            if (file.exists()) {
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
}
