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

import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product descriptor
 */
public class VMProductDescriptor {

    private List<VMVersionDescriptor> versions = new ArrayList<VMVersionDescriptor>();
    private String productName;
    private Map<String,String> webSites = new HashMap<String, String>();

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

        for (Element web : XMLUtils.getChildElementList(root, "web")) {
            webSites.put(web.getAttribute("type"), XMLUtils.getElementBody(web));
        }
    }

    public String getProductName()
    {
        return productName;
    }

    public Map<String, String> getWebSites()
    {
        return webSites;
    }

    public String getWebSite(String type)
    {
        return webSites.get(type);
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
