/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.erd.editor.ERDAttributeStyle;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

/**
 * Column entry in model Table
 * @author Serge Rider
 */
public class ERDEntityAttribute extends ERDObject<DBSEntityAttribute>
{
    private final EntityDiagram diagram;
    private boolean inPrimaryKey;
    private boolean inForeignKey;

    public ERDEntityAttribute(EntityDiagram diagram, DBSEntityAttribute attribute, boolean inPrimaryKey) {
        super(attribute);
        this.diagram = diagram;
        this.inPrimaryKey = inPrimaryKey;
    }

	public String getLabelText()
	{
        String text;
        if (diagram.hasAttributeStyle(ERDAttributeStyle.TYPES)) {
            text = object.getName() + ": " + object.getFullTypeName();
        } else {
            text = object.getName();
        }
        if (diagram.hasAttributeStyle(ERDAttributeStyle.COMMENTS)) {
            String comment = object.getDescription();
            if (!CommonUtils.isEmpty(comment)) {
                text += " - " + comment;
            }
        }
        return text;
	}

    public DBPImage getLabelImage()
    {
        if (!diagram.hasAttributeStyle(ERDAttributeStyle.ICONS)) {
            return null;
        }
        return DBValueFormatting.getObjectImage(object);
    }

    public boolean isInPrimaryKey() {
        return inPrimaryKey;
    }

    public boolean isInForeignKey() {
        return inForeignKey;
    }

    public void setInForeignKey(boolean inForeignKey) {
        this.inForeignKey = inForeignKey;
    }

    @NotNull
    @Override
    public String getName()
    {
        return getObject().getName();
    }
}