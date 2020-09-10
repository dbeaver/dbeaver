/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.erd.model.ERDObject;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Column entry in model Table
 *
 * @author Serge Rider
 */
public class ERDEntityAttribute extends ERDObject<DBSEntityAttribute> {
    private boolean isChecked;
    private int order = -1;
    private boolean inPrimaryKey;
    private boolean inForeignKey;
    private String alias;

    public ERDEntityAttribute(DBSEntityAttribute attribute, boolean inPrimaryKey) {
        super(attribute);
        this.inPrimaryKey = inPrimaryKey;
    }

    public String getLabelText() {
        return object.getName();
    }

    public DBPImage getLabelImage() {
        return DBValueFormatting.getObjectImage(object);
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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
    public String getName() {
        return getObject().getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}