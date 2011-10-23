/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.tools.ant.productman;

import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Element;

/**
 * Version descriptor
 */
public class VMVersionDescriptor {
    private String number;
    private String updateTime;
    private String releaseNotes;

    public VMVersionDescriptor(Element root)
    {
        number = XMLUtils.getChildElementBody(root, "number");
        updateTime = XMLUtils.getChildElementBody(root, "date");
        releaseNotes = CommonUtils.toString(XMLUtils.getChildElementBody(root, "release-notes")).trim();
    }

    public String getNumber()
    {
        return number;
    }

    public String getUpdateTime()
    {
        return updateTime;
    }

    public String getReleaseNotes()
    {
        return releaseNotes;
    }
}
