/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.productman;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;

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
        try {
            VMProductDescriptor product = new VMProductDescriptor(XMLUtils.parseDocument(productDescriptor));

            VMVersionDescriptor version = product.getVersion(versionNumber);
            if (version == null) {
                throw new BuildException("Version '" + versionNumber + "' definition not found");
            }

            File dir = new File(targetDirectory);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new BuildException("Can't create target directory '" + dir.getAbsolutePath() + "'");
                }
            }
            File versionFile = new File(dir, "version.xml");

            FileOutputStream out = new FileOutputStream(versionFile);
            try {
                XMLBuilder versionXML = new XMLBuilder(out, "UTF-8");
                versionXML.setButify(true);
                generateVersionInfo(product, version, versionXML);
                versionXML.flush();
            } finally {
                IOUtils.close(out);
            }
        } catch (IOException e) {
            throw new BuildException("IO error", e);
        } catch (XMLException e) {
            throw new BuildException("XML error", e);
        }
    }

    private void generateVersionInfo(VMProductDescriptor product, VMVersionDescriptor version, XMLBuilder xml) throws IOException
    {
        xml.startElement("version");

        xml.startElement("name").addText(product.getProductName()).endElement();
        xml.startElement("number").addText(version.getNumber()).endElement();
        xml.startElement("date").addText(version.getUpdateTime()).endElement();
        xml.startElement("release-notes").addText(version.getReleaseNotes()).endElement();

        xml.endElement();
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
