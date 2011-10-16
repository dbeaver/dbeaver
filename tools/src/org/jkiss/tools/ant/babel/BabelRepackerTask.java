/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.babel;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Copies properties files from Babel jajs into Eclipse plugins jars.
 * Babel location should be passed in babelLocation ('eclipse/plugins' folder should be here.
 * buildDirectory specifies a folder where Eclipse is located.
 */
public class BabelRepackerTask extends Task
{
    public static final FilenameFilter JARS_FILTER = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".jar");
        }
    };
    public static final FilenameFilter PROPERTIES_FILTER = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".properties");
        }
    };

    private String eclipseDirectory;
    private String babelLocation;
    private String locales;

    // The method executing the task
    public void execute() throws BuildException
    {
/*
        Project prj = this.getProject();
        File baseDir = prj.getBaseDir();
        String defaultTarget = prj.getDefaultTarget();
        String dscrptn = prj.getDescription();
        Hashtable inheritedProperties = prj.getInheritedProperties();
        Hashtable userProperties = prj.getUserProperties();
        Target owningTarget = this.getOwningTarget();
        Hashtable properties = prj.getProperties();
*/
        Map<File, List<File>> pluginJarMap = new HashMap<File, List<File>>(); // plugin jar -> list of Babel jars (for different locales)
        System.out.println("Collecting an information about jars for localization.");

        for (String locale : locales.split(",")) {
            String[] babelJars = null;
            final String nlSuffix = "nl_" + locale;
            File babelDir = new File(babelLocation + "/eclipse/plugins");
            if (babelDir.exists() && babelDir.isDirectory()) {
                babelJars = babelDir.list(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".jar") && name.contains(nlSuffix);
                    }
                });
            }
            else {
                throw new BuildException("Babel directory isn't found or it doesn't contain eclipse/plugins subfolder.");
            }

            if (babelJars != null && babelJars.length > 0) {

                File pluginsDir = new File(eclipseDirectory + "/plugins");
                if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                    String[] plugins = pluginsDir.list(JARS_FILTER);
                    for (String plugin : plugins) {
                        File pluginFile = new File(pluginsDir, plugin);
                        String pluginFileName = pluginFile.getName();
                        for (String babelJar : babelJars) {
                            String pluginName = babelJar.substring(0, babelJar.indexOf(nlSuffix) - 1);
                            if (pluginFileName.startsWith(pluginName) && Character.isDigit(pluginFileName.charAt(pluginName.length() + 1))) {
                                if (pluginJarMap.get(pluginFile) == null) {
                                    pluginJarMap.put(pluginFile, new ArrayList<File>());
                                }
                                pluginJarMap.get(pluginFile).add(new File(babelDir, babelJar));
                                break;
                            }
                        }
                    }
                }
                else {
                    throw new BuildException("Eclipse plugins directory isn't found.");
                }
            }
            else {
                throw new BuildException("No jars found in Babel.");
            }
        }

        Set<File> eclipsePlugins = pluginJarMap.keySet();
        int k = 1;
        int kplugins = eclipsePlugins.size();
        for (File pluginFile : eclipsePlugins) {
            System.out.println("[" + k++ + "/" + kplugins + "] plugin " + pluginFile.getName() + " is being localized.");
            localizePlugin(pluginJarMap.get(pluginFile), pluginFile, PROPERTIES_FILTER);
        }
    }

    public static void localizePlugin(List<File> babelFiles, File pluginFile, FilenameFilter filenameFilter)
    {
        if (!pluginFile.exists() || babelFiles == null || babelFiles.isEmpty()) {
            return;
        }
        // get a temp file
        File tempFile = null;
        try {
            tempFile = File.createTempFile("tmp", ".zip");
        } catch (IOException e) {
            System.out.println("Can't create a temp file.");
        }
        // delete it, otherwise you cannot rename your existing zip to it.
        tempFile.delete();

        boolean renameOk = pluginFile.renameTo(tempFile);
        if (!renameOk) {
            throw new BuildException("could not rename the file " + pluginFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }

        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(pluginFile));
        } catch (FileNotFoundException e) {
            System.out.println("Can't create an output stream for destination zip file " + pluginFile.getAbsolutePath());
            return;
        }

        // append dest file own entries
        ZipFile destZip = null;
        try {
            destZip = new ZipFile(tempFile);
        } catch (IOException e) {
            System.out.println("A problem with processing destination zip file " + pluginFile.getAbsolutePath());
            return;
        }
        Enumeration<? extends ZipEntry> destEntries = destZip.entries();
        while (destEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) destEntries.nextElement();
            //System.out.println(entry.getName());
            String entryName = entry.getName();
            ZipEntry newEntry = new ZipEntry(entryName);
            try {
                out.putNextEntry(newEntry);

                BufferedInputStream bis = new BufferedInputStream(destZip.getInputStream(entry));
                while (bis.available() > 0) {
                    out.write(bis.read());
                }
                out.closeEntry();
                bis.close();
            } catch (IOException e) {
                System.out.println("Can't copy " + entryName + " from temporary file to " + pluginFile.getAbsolutePath());
                break;
            }
        }

        for (File sourceZipFile : babelFiles) {
            // append source file entries
            ZipFile sourceZip = null;
            try {
                sourceZip = new ZipFile(sourceZipFile);
            } catch (IOException e) {
                System.out.println("A problem with processing source zip file " + sourceZipFile.getAbsolutePath());
            }
            Enumeration<? extends ZipEntry> srcEntries = sourceZip.entries();
            while (srcEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) srcEntries.nextElement();
                String entryName = entry.getName();
                if (filenameFilter.accept(null, entryName)) {
                    //System.out.println("entryName = " + entryName);
                    ZipEntry newEntry = new ZipEntry(entryName);
                    try {
                        out.putNextEntry(newEntry);
    
                        BufferedInputStream bis = new BufferedInputStream(sourceZip.getInputStream(entry));
                        while (bis.available() > 0) {
                            out.write(bis.read());
                        }
                        out.closeEntry();
                        bis.close();
                    } catch (IOException e) {
                        System.out.println("Can't copy " + entryName + " from " + sourceZipFile.getAbsolutePath() + " to " + pluginFile.getAbsolutePath());
                        break;
                    }
                }
            }
        }

        // Complete the ZIP file
        try {
            out.close();
        } catch (IOException e) {
            System.out.println("Can't close output stream for destination " + pluginFile.getAbsolutePath());
        }
        tempFile.delete();
    }

    public void setBabelLocation(String msg)
    {
        this.babelLocation = msg;
    }

    public void setEclipseDirectory(String eclipseDirectory)
    {
        this.eclipseDirectory = eclipseDirectory;
    }

    public void setLocales(String locales)
    {
        this.locales = locales;
    }
}
