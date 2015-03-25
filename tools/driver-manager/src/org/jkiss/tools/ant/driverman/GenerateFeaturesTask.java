/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.tools.ant.driverman;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates Eclipse plugins and features from driver descriptors.
 *
 * 1. Loads drivers' plugin descriptors
 * 2. Generate driver's feature in target dir
 * 3. Copies drivers' plugin into target dir
 * 4. Generate update site in update site dir
 * 5. Create driver-pack archive in build dir
 */
public class GenerateFeaturesTask extends Task
{
    private static String[] RUNTIME_FEATURES = {
        "org.jkiss.dbeaver.runtime",
        "org.jkiss.dbeaver.ext.generic",
        "org.jkiss.dbeaver.ext.mysql",
        "org.jkiss.dbeaver.ext.oracle",
        "org.jkiss.dbeaver.ext.db2",
		"org.jkiss.dbeaver.ext.firebird",
        "org.jkiss.dbeaver.ext.postgresql",
		"org.jkiss.dbeaver.ext.mssql",
//        "org.jkiss.dbeaver.ext.wmi",
    };

    private String buildDirectory;
    private String targetDirectory;
    private String driversDirectory;
    private String featuresDirectory;
    private String updateSiteDirectory;

    private List<DriverInfo> drivers = new ArrayList<DriverInfo>();
    private File featuresPath;
    private File pluginsPath;
    private ZipOutputStream driversZip;
    private File zipFile;

    @Override
    public void execute() throws BuildException
    {
        File rootPath = new File(driversDirectory);
        System.out.println("Search drivers in " + rootPath.getAbsolutePath() + "...");
        searchDrivers(rootPath);
        System.out.println(drivers.size() + " driver(s) found");

        if (!drivers.isEmpty()) {
            try {
                makeDirectory(new File(buildDirectory));
                zipFile = new File(buildDirectory, "driver-pack.zip");
                System.out.println("Add drivers to archive [" + zipFile.getAbsolutePath() + "]");
                driversZip = new ZipOutputStream(
                    new FileOutputStream(zipFile));
                driversZip.setLevel(Deflater.BEST_COMPRESSION);
                try {
                    generateFeatures();
                    addDriversReadme();
                } finally {
                    driversZip.close();
                }
            } catch (IOException e) {
                throw new BuildException("Can't generate features", e);
            }
        }
    }

    private void addDriversReadme() throws IOException
    {
        driversZip.putNextEntry(new ZipEntry("drivers_readme.txt"));
        PrintWriter out = new PrintWriter(driversZip);
        out.println("JDBC Drivers Pack");
        out.println();
        out.println("This archive contains following JDBC drivers:");
        out.println();
        for (DriverInfo driver : drivers) {
            out.println("\t" + driver.getName() + " (version " + driver.getVersion() + ")");
        }
        out.println();
        out.println("DBeaver has configurations for all these drivers.");
        out.println("If you don't have internet access when using DBeaver then you may just extract this archive");
        out.println("in the same directory where DBeaver was installed.");
        out.println("");
        out.println("Default DBeaver installation folder in Windows is standard \"Program Files\" folder,");
        out.println("so typically it will be \"C:\\Program Files\\DBeaver\\\" for Windows XP 32-bit.");
        out.println("Extract this archive in this folder so drivers will be located in");
        out.println("\"C:\\Program Files\\DBeaver\\drivers\\\" folder.");
        out.println("");
        out.println("After that DBeaver will automatically locate driver's files whenever you create a new connection.");
        out.println("");
        out.println("Thank you for using DBeaver!");
        out.println("Visit us at http://dbeaver.jkiss.org/");
        out.flush();
        out.close();
    }

    private void generateFeatures() throws IOException
    {
        if (updateSiteDirectory == null) {
            updateSiteDirectory = targetDirectory + "/updateSite";
        }
        File targetPath = new File(targetDirectory);
        System.out.println("Generate Eclipse features into " + targetPath.getAbsolutePath() + "...");

        featuresPath = new File(targetPath, "features");
        pluginsPath = new File(targetPath, "plugins");
        makeDirectory(featuresPath);
        makeDirectory(pluginsPath);

        for (DriverInfo driver : drivers) {
            generateDriverFeature(driver);
        }

        System.out.println("Modify update site " + updateSiteDirectory);
        File updateSiteDir = new File(updateSiteDirectory);
        if (updateSiteDir.exists()) {
            File siteXML = new File(updateSiteDir, "site.xml");
            System.out.println("Create update site index " + siteXML.getAbsolutePath() + "...");
            createUpdateSiteMap(siteXML);
        }
    }

    private void generateDriverFeature(DriverInfo driver) throws IOException
    {
        System.out.println("\t-Generate feature " + driver.getFeatureID());
        File featurePath = new File(featuresPath, driver.getFeatureID());
        File pluginPath = new File(pluginsPath, driver.getPluginID());
        makeDirectory(featurePath);
        makeDirectory(pluginPath);

        // Driver pack
        {
            String filePrefix = "drivers/" + driver.getId() + "/";
            List<String> pluginFiles = new ArrayList<String>();
            pluginFiles.addAll(driver.getFiles());
            if (!CommonUtils.isEmpty(driver.getLicense())) {
                pluginFiles.add(driver.getLicense());
            }
            for (String driverFile : pluginFiles) {
                File sourceFile = new File(driver.getPath(), driverFile);
                if (!sourceFile.exists()) {
                    System.err.println("File '" + sourceFile.getAbsolutePath() + "' doesn't exist");
                    continue;
                }
                System.out.println("\t\tAdd " + driverFile + " [" + sourceFile.length() + "] to driver pack");
                ZipEntry zipEntry = new ZipEntry(driverFile);
                driversZip.putNextEntry(zipEntry);
                FileInputStream is = new FileInputStream(sourceFile);
                try {
                    IOUtils.copyStream(is, driversZip, 10000);
                } finally {
                    is.close();
                }
            }
        }

        // Feature
        {
            {
                // Generate build.properties
                File buildPropertiesFile = new File(featurePath, "build.properties");
                PrintWriter propsWriter = new PrintWriter(new FileWriter(buildPropertiesFile));
                propsWriter.println("bin.includes = feature.xml,feature.properties");
                propsWriter.close();
            }
            {
                // Generate feature.properties
                File featurePropertiesFile = new File(featurePath, "feature.properties");
                PrintWriter propsWriter = new PrintWriter(new FileWriter(featurePropertiesFile));
                propsWriter.println("featureName=" + driver.getName());
                propsWriter.println("providerName=" + driver.getVendor());
                propsWriter.println("description=" + driver.getDescription());
                propsWriter.println("copyright=");
                String license = driver.getLicense();
                if (!CommonUtils.isEmpty(license)) {
                    String licenseText = readFileToString(new File(driver.getPath(), license));
                    propsWriter.println("license=" + licenseText.replace("\r", "").replace("\n", "\\n\\\n"));
                }
                propsWriter.close();
            }
            {
                // Generate feature.xml
                FileWriter pluginWriter = new FileWriter(new File(featurePath, "feature.xml"));
                XMLBuilder featureXML = new XMLBuilder(pluginWriter, "UTF-8");
                featureXML.setButify(true);
                featureXML.startElement("feature");
                featureXML.addAttribute("id", driver.getFeatureID());
                featureXML.addAttribute("label", "%featureName");
                featureXML.addAttribute("version", driver.getVersion());
                featureXML.addAttribute("provider", "%providerName");
                featureXML.addAttribute("plugin", driver.getPluginID());

                featureXML.startElement("description");
                featureXML.addText("%description");
                featureXML.endElement();

                featureXML.startElement("copyright");
                featureXML.addText("%copyright");
                featureXML.endElement();

                featureXML.startElement("license");
                featureXML.addText("%license");
                featureXML.endElement();

                featureXML.startElement("requires");
                featureXML.startElement("import");
                featureXML.addAttribute("feature", "org.jkiss.dbeaver.ext.generic");
                featureXML.addAttribute("version", "1.0.0");
                featureXML.addAttribute("match", "greaterOrEqual");
                featureXML.endElement();
                featureXML.startElement("import");
                featureXML.addAttribute("feature", "org.jkiss.dbeaver.runtime");
                featureXML.addAttribute("version", "1.0.0");
                featureXML.addAttribute("match", "greaterOrEqual");
                featureXML.endElement();
                featureXML.endElement();

                featureXML.startElement("plugin");
                featureXML.addAttribute("id", driver.getPluginID());
                featureXML.addAttribute("download-size", 0);
                featureXML.addAttribute("install-size", 0);
                featureXML.addAttribute("version", driver.getVersion());
                featureXML.addAttribute("unpack", "true");
                featureXML.endElement();

                featureXML.endElement();
                featureXML.flush();
                pluginWriter.close();
            }
        }

        System.out.println("\t-Copy driver plugin " + driver.getFeatureID());
        copyDirs(driver.getPath(), pluginPath);
    }

    private void createUpdateSiteMap(File siteFile) throws IOException
    {
        System.out.println("Create update site map...");

        FileOutputStream os = new FileOutputStream(siteFile);
        XMLBuilder siteXML = new XMLBuilder(os, "utf-8");
        siteXML.setButify(true);

        siteXML.startElement("site");

        siteXML.startElement("description");
        siteXML.addAttribute("name", "DBeaver Update Site");
        siteXML.addAttribute("url", "http://dbeaver.jkiss.org/update/2.0");
        siteXML.addText("DBeaver Update Site");
        siteXML.endElement();

        siteXML.startElement("category-def");
        siteXML.addAttribute("name", "org.jkiss.dbeaver");
        siteXML.addAttribute("label", "DBeaver");
        siteXML.startElement("description");
        siteXML.addText("Universal Database Manager");
        siteXML.endElement();
        siteXML.endElement();

        siteXML.startElement("category-def");
        siteXML.addAttribute("name", "org.jkiss.dbeaver.drivers");
        siteXML.addAttribute("label", "External database drivers");
        siteXML.startElement("description");
        siteXML.addText("3rd party JDBC drivers for DBeaver");
        siteXML.endElement();
        siteXML.endElement();

        // Add runtime features
        for (String featureID : RUNTIME_FEATURES) {
            File featureFile = new File(featuresDirectory, featureID + "/feature.xml");
            if (!featureFile.exists()) {
                System.out.println("Feature [" + featureID + "] not found in [" + featuresDirectory + "]");
                continue;
            }
            String featureVersion;
            String featureOS;
            try {
                Document pluginDocument = XMLUtils.parseDocument(featureFile);
                Element featureElement = pluginDocument.getDocumentElement();
                featureVersion = featureElement.getAttribute("version");
                if (CommonUtils.isEmpty(featureVersion)) {
                    System.out.println("Feature [" + featureID + "] doesn't has version info");
                    continue;
                }
                featureOS = featureElement.getAttribute("os");
            } catch (XMLException e) {
                throw new IllegalArgumentException(e);
            }

            siteXML.startElement("feature");
            siteXML.addAttribute("id", featureID);
            siteXML.addAttribute("version", featureVersion);
            siteXML.addAttribute("url", "features/" + featureID + "_" + featureVersion + ".jar");
            if (!CommonUtils.isEmpty(featureOS)) {
                siteXML.addAttribute("os", featureOS);
            }
            siteXML.startElement("category");
            siteXML.addAttribute("name", "org.jkiss.dbeaver");
            siteXML.endElement();
            siteXML.endElement();

            System.out.println("\t-Runtime feature " + featureID + " added");
        }

        // Add drivers' features
        for (DriverInfo driver : drivers) {
            siteXML.startElement("feature");
            siteXML.addAttribute("id", driver.getFeatureID());
            siteXML.addAttribute("version", driver.getVersion());
            siteXML.addAttribute("url", "features/" + driver.getFeatureID() + "_" + driver.getVersion() + ".jar");
            siteXML.startElement("category");
            siteXML.addAttribute("name", "org.jkiss.dbeaver.drivers");
            siteXML.endElement();
            siteXML.endElement();

            System.out.println("\t-Driver feature " + driver.getFeatureID() + " added");
        }

        siteXML.endElement();

        siteXML.flush();
        os.close();
    }

    private void searchDrivers(File path)
    {
        File sourcePluginsDir = new File(path, "plugins");
        for (File pluginDir : ArrayUtils.safeArray(sourcePluginsDir.listFiles())) {
            if (pluginDir.isDirectory()) {
                try {
                    DriverInfo driver = new DriverInfo(pluginDir);
                    if (driver.getFiles().isEmpty()) {
                        continue;
                    }
                    drivers.add(driver);
                } catch (IllegalArgumentException e) {
                    // Bad driver
                    System.out.println("Plugin '" + pluginDir.getName() + "' has incorrect description [" + e.getMessage() + "]");
                    continue;
                }
            }
        }
    }

    public void setBuildDirectory(String buildDirectory)
    {
        this.buildDirectory = buildDirectory;
    }

    public void setTargetDirectory(String targetDirectory)
    {
        this.targetDirectory = targetDirectory;
    }

    public void setDriversDirectory(String driversDirectory)
    {
        this.driversDirectory = driversDirectory;
    }

    public void setFeaturesDirectory(String featuresDirectory)
    {
        this.featuresDirectory = featuresDirectory;
    }

    public void setUpdateSiteDirectory(String updateSiteDirectory)
    {
        this.updateSiteDirectory = updateSiteDirectory;
    }

    private void makeDirectory(File featurePath) throws IOException
    {
        if (!featurePath.exists() && !featurePath.mkdirs()) {
            throw new IOException("Can't create directory " + featurePath.getAbsolutePath());
        }
    }

    private void copyDirs(File src, File dest) throws IOException
    {
        for (File file : ArrayUtils.safeArray(src.listFiles())) {
            if (file.isDirectory()) {
                if (file.getName().equals(".") || file.getName().equals("..")) {
                    continue;
                }
                File destDir = new File(dest, file.getName());
                makeDirectory(destDir);
                copyDirs(file, destDir);
            } else {
                copyFiles(file, new File(dest, file.getName()));
            }
        }
    }

    private void copyFiles(File sourceFile, File targetFile) throws IOException
    {
        InputStream is = new FileInputStream(sourceFile);
        OutputStream os = new FileOutputStream(targetFile);
        IOUtils.copyStream(is, os, 10000);
        IOUtils.close(os);
        IOUtils.close(is);
    }

    private String readFileToString(File file) throws IOException
    {
        InputStream fileStream = new FileInputStream(file);
        try {
            InputStreamReader unicodeReader = new InputStreamReader(fileStream, "UTF-8");
            StringBuilder result = new StringBuilder((int) file.length());
            char[] buffer = new char[4000];
            for (;;) {
                int count = unicodeReader.read(buffer);
                if (count <= 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString();
        } finally {
            IOUtils.close(fileStream);
        }
    }

    public static void main(String[] args)
    {
        GenerateFeaturesTask task = new GenerateFeaturesTask();
        task.setBuildDirectory("c:\\temp\\build");
        task.setTargetDirectory("c:\\temp\\target");
        task.setUpdateSiteDirectory("c:\\temp\\update-site");
        task.setDriversDirectory("c:\\Devel\\My\\dbeaver\\contrib\\drivers");
        task.setFeaturesDirectory("c:\\Devel\\My\\dbeaver\\features");
        task.execute();
    }

}
