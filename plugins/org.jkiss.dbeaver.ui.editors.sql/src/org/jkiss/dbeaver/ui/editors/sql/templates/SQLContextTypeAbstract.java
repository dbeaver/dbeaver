/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.templates;


import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

public abstract class SQLContextTypeAbstract extends TemplateContextType {


    protected SQLContextTypeAbstract(@NotNull String id, @NotNull String name) {
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

    private void addDatabaseProposals() {
        addResolver(new SQLEntityResolver());
        addResolver(new SQLContainerResolver<>(
            SQLContainerResolver.VAR_NAME_SCHEMA, "Schema", DBSSchema.class));
        addResolver(new SQLContainerResolver<>(
            SQLContainerResolver.VAR_NAME_CATALOG, "Catalog", DBSCatalog.class));
        addResolver(new SQLAttributeResolver());
        addResolver(new SQLDataTypeResolver());
    }

    @Override
    public String toString() {
        return getId() + " [" + getName() + "]";
    }

    @Override
    public int hashCode() {
        return getId().hashCode() + getName().hashCode();
    }

}
