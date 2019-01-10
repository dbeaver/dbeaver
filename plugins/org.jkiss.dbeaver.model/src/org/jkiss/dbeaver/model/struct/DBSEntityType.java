/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;

/**
 * Entity type
 */
public class DBSEntityType
{
    public static final DBSEntityType TABLE = new DBSEntityType("table", "Table", DBIcon.TREE_TABLE, true); //$NON-NLS-1$
    public static final DBSEntityType VIEW = new DBSEntityType("view", "View", DBIcon.TREE_VIEW, true); //$NON-NLS-1$
    public static final DBSEntityType TYPE = new DBSEntityType("type", "Type", DBIcon.TREE_DATA_TYPE, true); //$NON-NLS-1$
    public static final DBSEntityType CLASS = new DBSEntityType("class", "Class", DBIcon.TREE_CLASS, false); //$NON-NLS-1$
    public static final DBSEntityType ASSOCIATION = new DBSEntityType("association", "Association", DBIcon.TREE_ASSOCIATION, false); //$NON-NLS-1$
    public static final DBSEntityType TRIGGER = new DBSEntityType("trigger", "Trigger", DBIcon.TREE_TRIGGER, true); //$NON-NLS-1$

    public static final DBSEntityType VIRTUAL_ENTITY = new DBSEntityType("virtual_entity", "Virtual Entity", DBIcon.TREE_TABLE, true); //$NON-NLS-1$
    public static final DBSEntityType VIRTUAL_ASSOCIATION = new DBSEntityType("virtual_association", "Virtual Association", DBIcon.TREE_ASSOCIATION, false); //$NON-NLS-1$
    public static final DBSEntityType SEQUENCE = new DBSEntityType("sequence", "Sequence", DBIcon.TREE_SEQUENCE, true);
    public static final DBSEntityType PACKAGE = new DBSEntityType("package", "Package", DBIcon.TREE_PACKAGE, true);

    private final String id;
    private final String name;
    private final DBPImage icon;
    private final boolean physical;

    public DBSEntityType(String id, String name, DBPImage icon, boolean physical)
    {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.physical = physical;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DBPImage getIcon()
    {
        return icon;
    }

    public boolean isPhysical()
    {
        return physical;
    }

    public String toString()
    {
        return getName();
    }
}