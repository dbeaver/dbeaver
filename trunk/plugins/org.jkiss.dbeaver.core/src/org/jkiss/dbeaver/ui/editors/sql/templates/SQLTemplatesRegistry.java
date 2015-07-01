/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import java.io.IOException;

/**
 * Global SQL template registry
 */
public class SQLTemplatesRegistry {

    static final Log log = Log.getLog(SQLTemplatesRegistry.class);
    private static SQLTemplatesRegistry instance;

    private ContextTypeRegistry templateContextTypeRegistry;
    private TemplateStore templateStore;

    public synchronized static SQLTemplatesRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLTemplatesRegistry();
        }
        return instance;
    }

    public synchronized ContextTypeRegistry getTemplateContextRegistry() {
        if (templateContextTypeRegistry == null) {
            //SQLContextTypeRegistry registry = new SQLContextTypeRegistry();

            //TemplateContextType contextType= registry.getContextType("sql");

            templateContextTypeRegistry = new SQLContextTypeRegistry();
        }

        return templateContextTypeRegistry;
    }

    public TemplateStore getTemplateStore() {
        if (templateStore == null) {
            templateStore = new SQLTemplateStore(getTemplateContextRegistry());

            try {
                templateStore.load();
            } catch (IOException e) {
                log.error("Can't load template store", e);
            }
            templateStore.startListeningForPreferenceChanges();
        }

        return templateStore;
    }
}
