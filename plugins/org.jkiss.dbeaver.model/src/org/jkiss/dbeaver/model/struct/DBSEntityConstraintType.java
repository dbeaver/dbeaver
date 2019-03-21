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

import org.jkiss.dbeaver.model.messages.ModelMessages;

/**
 * DBSEntityConstraintType
 */
public class DBSEntityConstraintType
{
    public static final DBSEntityConstraintType FOREIGN_KEY = new DBSEntityConstraintType("fk", ModelMessages.model_struct_constraint_type_foreign_key, ModelMessages.model_struct_Foreign_Key, true, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType PRIMARY_KEY = new DBSEntityConstraintType("pk", ModelMessages.model_struct_constraint_type_primary_key, ModelMessages.model_struct_Primary_Key, false, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType UNIQUE_KEY = new DBSEntityConstraintType("unique", ModelMessages.model_struct_constraint_type_unique_key, ModelMessages.model_struct_Unique_Key, false, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType VIRTUAL_KEY = new DBSEntityConstraintType("virtual", ModelMessages.model_struct_constraint_type_virtual_key, ModelMessages.model_struct_Virtual_Key, false, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType PSEUDO_KEY = new DBSEntityConstraintType("pseudo", ModelMessages.model_struct_constraint_type_pseudo, ModelMessages.model_struct_Pseudo_Key, false, true, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType CHECK = new DBSEntityConstraintType("check", ModelMessages.model_struct_constraint_type_check, ModelMessages.model_struct_Check, false, false, true); //$NON-NLS-1$
    public static final DBSEntityConstraintType NOT_NULL = new DBSEntityConstraintType("notnull", ModelMessages.model_struct_constraint_type_not_null, ModelMessages.model_struct_Not_NULL, false, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType INDEX = new DBSEntityConstraintType("index", ModelMessages.model_struct_constraint_type_index, ModelMessages.model_struct_Index, false, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType ASSOCIATION = new DBSEntityConstraintType("association", ModelMessages.model_struct_constraint_type_association, ModelMessages.model_struct_Association, true, false, false); //$NON-NLS-1$
    public static final DBSEntityConstraintType INHERITANCE = new DBSEntityConstraintType("inheritance", ModelMessages.model_struct_constraint_type_inheritance, ModelMessages.model_struct_Inheritance, true, false, false); //$NON-NLS-1$

    private final String id;
    private final String name;
    private final String localizedName;
    private final boolean association;
    private final boolean unique;
    private final boolean custom;

    public DBSEntityConstraintType(String id, String name, String localizedName, boolean association, boolean unique, boolean custom)
    {
        this.id = id;
        this.name = name;
        this.localizedName = localizedName == null ? name : localizedName;
        this.association = association;
        this.unique = unique;
        this.custom = custom;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocalizedName()
    {
        return localizedName;
    }

    public boolean isAssociation()
    {
        return association;
    }

    public boolean isUnique()
    {
        return unique;
    }

    /**
     * Custom constraint (like CHECK) has some associated SQL expression
     */
    public boolean isCustom() {
        return custom;
    }

    public String toString()
    {
        return getName();
    }
}