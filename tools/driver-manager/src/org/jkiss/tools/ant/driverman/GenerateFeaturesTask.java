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
package org.jkiss.tools.ant.driverman;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates Eclipse plugins and features from driver descriptors
 */
public class GenerateFeaturesTask extends Task
{

    private String buildDirectory;
    private String targetDirectory;
    private String driversDirectory;
    private String updateSiteDirectory;

    private List<DriverInfo> drivers = new ArrayList<DriverInfo>();
    private File featuresPath;
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
                zipFile = new File(buildDirectory, "drivers-pack.zip");
                System.out.println("Add drivers to archive [" + zipFile.getAbsolutePath() + "]");
                driversZip = new ZipOutputStream(
                    new FileOutputStream(zipFile));
                driversZip.setLevel(Deflater.BEST_COMPRESSION);
                try {
                    generateFeatures();
                } finally {
                    driversZip.close();
                }
            } catch (IOException e) {
                throw new BuildException("Can't generate features", e);
            }
        }
    }

    private void generateFeatures() throws IOException
    {
        if (updateSiteDirectory == null) {
            updateSiteDirectory = targetDirectory + "/updateSite";
        }
        File targetPath = new File(targetDirectory);
        System.out.println("Generate Eclipse features into " + targetPath.getAbsolutePath() + "...");

        featuresPath = new File(targetPath, "features");
        makeDirectory(featuresPath);

        for (DriverInfo driver : drivers) {
            generateDriverFeature(driver);
        }

        System.out.println("Modify update site " + updateSiteDirectory);
        File updateSiteDir = new File(updateSiteDirectory);
        if (updateSiteDir.exists()) {
            File siteXML = new File(updateSiteDir, "site.xml");
            System.out.println("Patch update site index " + siteXML.getAbsolutePath() + "...");
            if (siteXML.exists()) {
                patchUpdateSite(siteXML);
            }
        }
    }

    private void generateDriverFeature(DriverInfo driver) throws IOException
    {
        System.out.println("\t-Generate feature " + driver.getFeatureID());
        File featurePath = new File(featuresPath, driver.getFeatureID());
        makeDirectory(featurePath);

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
    }

    private void patchUpdateSite(File siteFile) throws IOException
    {
        System.out.println("Patch update site...");
        String siteContent = readFileToString(siteFile);
        StringBuilder extraFeatures = new StringBuilder();
        for (DriverInfo driver : drivers) {
            if (siteContent.contains(driver.getFeatureID())) {
                // Already patched
                continue;
            }
            extraFeatures.append("\n   <feature id=\"").append(driver.getFeatureID()).
                append("\" version=\"").append(driver.getVersion())
                .append("\" url=\"features/").append(driver.getFeatureID()).append("_").append(driver.getVersion()).append(".jar\"")
                .append(">\n")
                .append("      <category name=\"org.jkiss.dbeaver.drivers\"/>\n")
                .append("   </feature>\n");

            System.out.println("\t-Feature " + driver.getFeatureID() + " added");
        }
        int divPos = siteContent.indexOf("</site>");
        if (divPos != -1) {
            siteContent = siteContent.substring(0, divPos) + extraFeatures + siteContent.substring(divPos);
        }
        FileWriter out = new FileWriter(siteFile);
        out.write(siteContent);
        out.close();
    }

    private void searchDrivers(File path)
    {
        File sourcePluginsDir = new File(path, "plugins");
        for (File pluginDir : CommonUtils.safeArray(sourcePluginsDir.listFiles())) {
            if (pluginDir.isDirectory()) {
                try {
                    DriverInfo driver = new DriverInfo(pluginDir);
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
        task.setBuildDirectory("D:\\temp\\build");
        task.setTargetDirectory("D:\\temp\\target");
        task.setUpdateSiteDirectory("D:\\temp\\update-site");
        task.setDriversDirectory("D:\\Devel\\My\\dbeaver\\contrib\\drivers");
        task.execute();
    }

}
