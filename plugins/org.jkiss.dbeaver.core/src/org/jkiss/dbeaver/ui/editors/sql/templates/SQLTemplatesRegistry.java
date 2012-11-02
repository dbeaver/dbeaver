package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.io.IOException;

/**
 * Global SQL template registry
 */
public class SQLTemplatesRegistry {

    static final Log log = LogFactory.getLog(SQLTemplatesRegistry.class);
    private static SQLTemplatesRegistry instance;

    private static final String TEMPLATES_KEY = "org.jkiss.dbeaver.core.sql_templates"; //$NON-NLS-1$

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
            SQLContextTypeRegistry registry = new SQLContextTypeRegistry();

            TemplateContextType all_contextType= registry.getContextType("sql");
            //((AbstractJavaContextType) all_contextType).initializeContextTypeResolvers();
/*
            registerJavaContext(registry, JavaContextType.ID_MEMBERS, all_contextType);
            registerJavaContext(registry, JavaContextType.ID_STATEMENTS, all_contextType);

            registerJavaContext(registry, SWTContextType.ID_ALL, all_contextType);
            all_contextType= registry.getContextType(SWTContextType.ID_ALL);

            registerJavaContext(registry, SWTContextType.ID_MEMBERS, all_contextType);
            registerJavaContext(registry, SWTContextType.ID_STATEMENTS, all_contextType);
*/

            templateContextTypeRegistry = registry;
        }

        return templateContextTypeRegistry;
    }

    public TemplateStore getTemplateStore() {
        if (templateStore == null) {
            final IPreferenceStore store= DBeaverCore.getInstance().getGlobalPreferenceStore();
            templateStore = new SQLTemplateStore(getTemplateContextRegistry(), store, TEMPLATES_KEY);

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
