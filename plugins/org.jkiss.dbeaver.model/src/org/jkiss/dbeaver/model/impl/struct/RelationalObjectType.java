/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.rdb.*;

/**
 * Relational database object type.
 * Used by structure assistants
 */
public class RelationalObjectType extends AbstractObjectType {

    public static final DBSObjectType TYPE_TABLE = new RelationalObjectType("Table", "Table", DBIcon.TREE_TABLE, DBSTable.class);
    public static final DBSObjectType TYPE_VIEW = new RelationalObjectType("View", "View", DBIcon.TREE_VIEW, DBSTable.class);
    public static final DBSObjectType TYPE_TABLE_COLUMN = new RelationalObjectType("Table column", "Table column", DBIcon.TREE_COLUMN, DBSTableColumn.class);
    public static final DBSObjectType TYPE_VIEW_COLUMN = new RelationalObjectType("View column", "View column", DBIcon.TREE_COLUMN, DBSTableColumn.class);
    public static final DBSObjectType TYPE_INDEX = new RelationalObjectType("Index", "Index", DBIcon.TREE_INDEX, DBSTableIndex.class);
    public static final DBSObjectType TYPE_CONSTRAINT = new RelationalObjectType("Constraint", "Table constraint", DBIcon.TREE_CONSTRAINT, DBSTableConstraint.class);
    public static final DBSObjectType TYPE_PROCEDURE = new RelationalObjectType("Procedure", "Procedure or function", DBIcon.TREE_PROCEDURE, DBSProcedure.class);
    public static final DBSObjectType TYPE_SEQUENCE = new RelationalObjectType("Sequence", "Sequence", DBIcon.TREE_SEQUENCE, DBSSequence.class);
    public static final DBSObjectType TYPE_TRIGGER = new RelationalObjectType("Trigger", "Trigger", DBIcon.TREE_TRIGGER, DBSTrigger.class);
    public static final DBSObjectType TYPE_DATA_TYPE = new RelationalObjectType("Data type", "Data type", DBIcon.TREE_DATA_TYPE, DBSDataType.class);
    public static final DBSObjectType TYPE_PACKAGE = new RelationalObjectType("Package", "Package", DBIcon.TREE_PACKAGE, DBSPackage.class);
    public static final DBSObjectType TYPE_SYNONYM = new RelationalObjectType("Synonym", "Synonym", DBIcon.TREE_SYNONYM, DBSAlias.class);

    public static final DBSObjectType TYPE_UNKNOWN = new RelationalObjectType("Unknown", "Unknown object type", DBIcon.TYPE_OBJECT, DBSObject.class);

    private RelationalObjectType(String typeName, String description, DBPImage image, Class<? extends DBSObject> objectClass) {
        super(typeName, description, image, objectClass);
    }
}
