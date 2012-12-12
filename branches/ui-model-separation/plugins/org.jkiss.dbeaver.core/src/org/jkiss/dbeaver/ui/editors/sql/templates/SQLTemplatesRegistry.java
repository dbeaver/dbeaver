package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import java.io.IOException;

/**
 * Global SQL template registry
 */
public class SQLTemplatesRegistry {

    static final Log log = LogFactory.getLog(SQLTemplatesRegistry.class);
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
