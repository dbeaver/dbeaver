/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.productman;

import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Product descriptor
 */
public class VMProductDescriptor {

    private List<VMVersionDescriptor> versions = new ArrayList<VMVersionDescriptor>();
    private String productName;

    public VMProductDescriptor(Document document)
    {
        Element root = document.getDocumentElement();

        productName = XMLUtils.getChildElementBody(root, "name");

        Element versionsElem = XMLUtils.getChildElement(root, "versions");
        if (versionsElem != null) {
            for (Element version : XMLUtils.getChildElementList(versionsElem, "version")) {
                VMVersionDescriptor versionDescriptor = new VMVersionDescriptor(version);
                versions.add(versionDescriptor);
            }

        }
    }

    public String getProductName()
    {
        return productName;
    }

    public List<VMVersionDescriptor> getVersions()
    {
        return versions;
    }

    public VMVersionDescriptor getVersion(String number)
    {
        for (VMVersionDescriptor versionDescriptor : versions) {
            if (versionDescriptor.getNumber().equals(number)) {
                return versionDescriptor;
            }
        }
        return null;
    }
}
