/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Wahlbrink <stephan.wahlbrink@walware.de> - [templates] improve logging when reading templates into SQLTemplateStore - https://bugs.eclipse.org/bugs/show_bug.cgi?id=212252
 *******************************************************************************/
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


/**
 * Manages templates. Handles reading default templates contributed via XML and
 * user-defined (or overridden) templates stored in the preferences.
 * <p>
 * Clients may instantiate but not subclass this class.
 * </p>
 */
public class SQLTemplateStore extends TemplateStore {

    private static final Log log = Log.getLog(SQLTemplateStore.class);
    public static final String PREF_STORE_KEY = "org.jkiss.dbeaver.core.sql_templates";

    public SQLTemplateStore(ContextTypeRegistry registry)
    {
        super(registry, new PreferenceStoreDelegate(new CustomTemplatesStore()), PREF_STORE_KEY); //$NON-NLS-1$
    }

    /**
     * Loads the templates contributed via the templates extension point.
     *
     * @throws java.io.IOException {@inheritDoc}
     */
    protected void loadContributedTemplates() throws IOException
    {
        Collection<TemplatePersistenceData> contributed = readContributedTemplates();
        for (TemplatePersistenceData data : contributed) {
            internalAdd(data);
        }
    }

    private Collection<TemplatePersistenceData> readContributedTemplates() throws IOException
    {
        Collection<TemplatePersistenceData> templates = new ArrayList<>();
        readIncludedTemplates(
            SQLEditorActivator.PLUGIN_ID,
            templates,
            "templates/default-templates.xml",
            "$nl$/templates/default-templates.properties");

        // Read templates for DS providers
        for (DBPDataSourceProviderDescriptor provider : DBWorkbench.getPlatform().getDataSourceProviderRegistry().getDataSourceProviders()) {
            readIncludedTemplates(
                provider.getPluginId(),
                templates,
                "templates/" + provider.getId() + "-templates.xml",
                "$nl$/templates/" + provider.getId() + "-templates.properties");
        }

        return templates;
    }

    private void readIncludedTemplates(
        String contributorId,
        Collection<TemplatePersistenceData> templates,
        String file,
        String translations) throws IOException
    {
        if (file != null) {
            Bundle plugin = Platform.getBundle(contributorId);
            URL url = FileLocator.find(plugin, Path.fromOSString(file), null);
            if (url != null) {
                ResourceBundle bundle = null;
                if (translations != null) {
                    URL bundleURL = FileLocator.find(plugin, Path.fromOSString(translations), null);
                    if (bundleURL != null) {
                        InputStream bundleStream = bundleURL.openStream();
                        try {
                            bundle = new PropertyResourceBundle(bundleStream);
                        } finally {
                            ContentUtils.close(bundleStream);
                        }
                    }
                }
                InputStream stream = new BufferedInputStream(url.openStream());
                try {
                    TemplateReaderWriter reader = new TemplateReaderWriter();
                    TemplatePersistenceData[] datas = reader.read(stream, bundle);
                    for (TemplatePersistenceData data : datas) {
                        if (data.isCustom()) {
                            if (data.getId() == null)
                                log.error("No template id specified");
                            else
                                log.error("Template " + data.getTemplate().getName() + " deleted");
                        } else if (validateTemplate(data.getTemplate())) {
                            templates.add(data);
                        }
                    }
                } finally {
                    ContentUtils.close(stream);
                }
            }
        }
    }

    /**
     * Validates a template against the context type registered in the context
     * type registry. Returns always <code>true</code> if no registry is
     * present.
     *
     * @param template the template to validate
     * @return <code>true</code> if validation is successful or no context
     *         type registry is specified, <code>false</code> if validation
     *         fails
     */
    private boolean validateTemplate(Template template)
    {
        String contextTypeId = template.getContextTypeId();
        if (!contextExists(contextTypeId))
            return false;

        if (getRegistry() != null) {
            try {
                getRegistry().getContextType(contextTypeId).validate(template.getPattern());
            } catch (TemplateException e) {
                log.error("Template '" + template.getName() + "' validation failed", e);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <code>true</code> if a context type id specifies a valid context type
     * or if no context type registry is present.
     *
     * @param contextTypeId the context type id to look for
     * @return <code>true</code> if the context type specified by the id
     *         is present in the context type registry, or if no registry is
     *         specified
     */
    private boolean contextExists(String contextTypeId)
    {
        return contextTypeId != null && (getRegistry() == null || getRegistry().getContextType(contextTypeId) != null);
    }

    protected void handleException(IOException x)
    {
        log.error(x);
    }

    private static class CustomTemplatesStore extends SimplePreferenceStore {
        private CustomTemplatesStore()
        {
            super(DBWorkbench.getPlatform().getPreferenceStore());
            try {
                File configurationFile = getConfigurationFile();
                if (configurationFile.exists()) {
                    setValue(PREF_STORE_KEY, ContentUtils.readFileToString(configurationFile));
                }
            } catch (IOException e) {
                log.error(e);
            }
        }

        private File getConfigurationFile()
        {
            return DBWorkbench.getPlatform().getConfigurationFile("templates.xml");
        }

        @Override
        public void save() throws IOException
        {
            // Save templates
            File configurationFile = getConfigurationFile();
            String templatesConfig = getString(PREF_STORE_KEY);
            if (!CommonUtils.isEmpty(templatesConfig)) {
                // Save it in templates file
                try (FileWriter writer = new FileWriter(configurationFile)) {
                    writer.write(templatesConfig);
                }
            } else {
                if (configurationFile.exists()) {
                    if (!configurationFile.delete()) {
                        log.warn("Can't delete empty template configuration");
                    }
                }
            }

        }
    }

}

