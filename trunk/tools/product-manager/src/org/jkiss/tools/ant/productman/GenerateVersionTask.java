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
package org.jkiss.tools.ant.productman;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

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

    @Override
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
        File archivesFolder = new File(new File(targetDirectory), "dbeaver-" + versionNumber);

        xml.startElement("version");

        xml.startElement("name").addText(product.getProductName()).endElement();
        xml.startElement("number").addText(version.getNumber()).endElement();
        xml.startElement("date").addText(version.getUpdateTime()).endElement();
        xml.startElement("release-notes").addText(version.getReleaseNotes()).endElement();
        xml.startElement("base-url").addText(product.getWebSite("download")).endElement();

        if (!CommonUtils.isEmpty(configs)) {
            StringTokenizer st = new StringTokenizer(configs, "&");
            while (st.hasMoreTokens()) {
                String config = st.nextToken().trim();
                if (CommonUtils.isEmpty(config) || config.startsWith("$")) {
                    continue;
                }
                StringTokenizer cst = new StringTokenizer(config, ",");
                String os = cst.nextToken().trim();
                String ui = cst.nextToken().trim();
                String arch = cst.nextToken().trim();
                String fileName = "dbeaver-" + versionNumber + "-" + os + "." + ui + "." + arch + ".zip";
                File file = new File(archivesFolder, fileName);
                xml.startElement("distribution");
                xml.addAttribute("os", os);
                xml.addAttribute("arch", arch);
                xml.addAttribute("ui", ui);
                xml.addAttribute("type", "archive");
                xml.addAttribute("file", fileName);
                if (file.exists()) {
                    xml.addAttribute("size", file.length());
                }
                xml.endElement();
            }
        }

        if (!CommonUtils.isEmpty(locales)) {
            StringTokenizer st = new StringTokenizer(locales);
            while (st.hasMoreTokens()) {
                xml.startElement("locale");
                xml.addAttribute("name", st.nextToken());
                xml.endElement();
            }

        }

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
