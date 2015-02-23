/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.jkiss.dbeaver.core.Log;
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
