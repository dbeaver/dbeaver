/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.productman;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Generates version descriptor from product descriptor
 */
public class GenerateVersionTask extends Task
{

    private String targetDirectory;
    private String productDescriptor;
    private String versionNumber;
    private String configs;
    private String locales;

    public void execute() throws BuildException
    {
        File dir = new File(targetDirectory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new BuildException("Can't create target directory '" + dir.getAbsolutePath() + "'");
            }
        }
        File versionFile = new File(dir, "version.xml");
        try {
            FileOutputStream out = new FileOutputStream(versionFile);
            try {
                XMLBuilder versionXML = new XMLBuilder(out, "UTF-8");
                generateVersionInfo(versionXML);
            } finally {
                IOUtils.close(out);
            }
        } catch (IOException e) {
            throw new BuildException("IO error", e);
        }
    }

    private void generateVersionInfo(XMLBuilder xml)
    {

    }

    public void setProductDescriptor(String msg)
    {
        this.productDescriptor = msg;
    }

    public void setTargetDirectory(String targetDirectory)
    {
        this.targetDirectory = targetDirectory;
    }

    public void setVersionNumber(String versionNumber)
    {
        this.versionNumber = versionNumber;
    }

    public void setConfigs(String configs)
    {
        this.configs = configs;
    }

    public void setLocales(String locales)
    {
        this.locales = locales;
    }
}
