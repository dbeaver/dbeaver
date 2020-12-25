/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;


import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

public abstract class SQLContextTypeAbstract extends TemplateContextType {


    protected SQLContextTypeAbstract(String id, String name)
    {
        super(id, name);
        addGlobalResolvers();
        addDatabaseProposals();
    }

    private void addGlobalResolvers() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }

    private void addDatabaseProposals()
    {
        addResolver(new SQLEntityResolver());
        addResolver(new SQLContainerResolver<>(
            SQLContainerResolver.VAR_NAME_SCHEMA, "Schema", DBSSchema.class));
        addResolver(new SQLContainerResolver<>(
            SQLContainerResolver.VAR_NAME_CATALOG, "Catalog", DBSCatalog.class));
        addResolver(new SQLAttributeResolver());
        addResolver(new SQLDataTypeResolver());
    }

    @Override
    public String toString()
    {
        return getId() + " [" + getName() + "]";
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode() + getName().hashCode();
    }

}
