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
        releaseNotes = CommonUtils.toString(XMLUtils.getChildElementBody(root, "release-notes"));
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
