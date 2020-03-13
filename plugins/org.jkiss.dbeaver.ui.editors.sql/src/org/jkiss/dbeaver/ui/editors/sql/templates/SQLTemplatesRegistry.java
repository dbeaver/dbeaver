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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.jkiss.dbeaver.Log;

import java.io.IOException;

/**
 * Global SQL template registry
 */
public class SQLTemplatesRegistry {

    private static final Log log = Log.getLog(SQLTemplatesRegistry.class);
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
