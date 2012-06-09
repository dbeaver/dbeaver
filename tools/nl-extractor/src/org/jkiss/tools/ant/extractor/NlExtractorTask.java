/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.extractor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Extract resources properties files from the project for localization.
 */
public class NlExtractorTask extends Task
{
    public static final int PROPERTIES_EXT_LENGTH = 11;

    public static final FilenameFilter PROPERTIES_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".properties");
        }
    };

    private String baseLocation;
    private String targetLocation;
    private String locales;
    private File baseDir;
    private File targetDir;

    // The method executing the task
    @Override
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
        System.out.println("baseLocation = " + baseLocation);
        System.out.println("targetLocation = " + targetLocation);

        baseDir = new File(baseLocation);
        targetDir = new File(targetLocation);
        try {
            processDirectory (baseDir);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new BuildException(e.getMessage(), e);
        }
    }

    private void processDirectory(File dir) throws IOException
    {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            }
            else {
                String filename = file.getName();
                if (PROPERTIES_FILTER.accept(null, filename)) {
                    String name = (file.getName()).substring(0, filename.length() - PROPERTIES_EXT_LENGTH);
                    if (new File(dir, name + "_ru.properties").exists()) {
                        String relativePath = baseDir.toURI().relativize(file.toURI()).toString();
                        //System.out.println(relativePath);
                        String relativeDir = relativePath.substring(0, relativePath.lastIndexOf("/"));
                        //System.out.println(relativeDir);
                        File target = new File(targetDir, relativeDir);
                        target.mkdirs();
                        for (File properties : dir.listFiles(PROPERTIES_FILTER)) {
                            if (properties.getName().startsWith(name)) {
                                //System.out.println(name);
                                copyFile(properties, target);
                            }
                        }
                    }
                }
            }
        }

    }

    private static void copyFile(File source , File target) throws IOException
    {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(target+"/"+source.getName());

            // Copy the bits from input stream to output stream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
      }

    public void setTargetLocation(String msg)
    {
        this.targetLocation = msg;
    }

    public void setBaseLocation(String baseLocation)
    {
        this.baseLocation = baseLocation;
    }

    public void setLocales(String locales)
    {
        this.locales = locales;
    }
}
