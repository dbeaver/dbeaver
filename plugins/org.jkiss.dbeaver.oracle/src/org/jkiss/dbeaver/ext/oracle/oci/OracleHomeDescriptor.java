/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.oci;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OracleHomeDescriptor
{
    static final Log log = LogFactory.getLog(OracleHomeDescriptor.class);

    private String oraHome; // without slash at the end
    private Integer oraVersion; // short version (9, 10, 11...)
    private String fullOraVersion;
    private boolean isInstantClient;

    public OracleHomeDescriptor(String oraHome)
    {
        this.oraHome = CommonUtils.removeSplashFileName(oraHome);
        this.isInstantClient = OCIUtils.isInstatntClient(oraHome);
        this.oraVersion = OCIUtils.getOracleVersion(oraHome, isInstantClient);
        this.fullOraVersion = OCIUtils.getOracleClientVersion(oraHome, isInstantClient);
    }

    public String getOraHome()
    {
        return oraHome;
    }

    public Integer getOraVersion()
    {
        return oraVersion;
    }

    public String getFullOraVersion()
    {
        return fullOraVersion;
    }

    public boolean isInstantClient()
    {
        return isInstantClient;
    }

    /**
     * Reads TNS aliaces from TNSNAMES.ORA in specified Oracle home.
     * @param oraHome path of Oracle home location
     * @return TNS aliases list
     */
    public ArrayList<String> getOraServiceNames()
    {
        ArrayList<String> aliases = new ArrayList<String>();

        // parse TNSNAMES.ORA file
        if (oraHome != null) {
            String tnsnamesPath = oraHome + "/Network/Admin/TNSNAMES.ORA";
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
        }
        Collections.sort(aliases);
        return aliases;
    }

    private List<String> getRequiredJars()
    {
        List<String> list = new ArrayList<String>();
        String jdbcDriverJar;
        String jdbcPathPrefix = isInstantClient ? "/" : "/jdbc/lib/";
        String libPathPrefix = isInstantClient ? "/" : "/lib/";
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
