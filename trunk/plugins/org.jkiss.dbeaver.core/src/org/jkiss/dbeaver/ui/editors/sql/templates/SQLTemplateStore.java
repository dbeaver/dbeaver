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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.osgi.framework.Bundle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
public class SQLTemplateStore extends TemplateStore {

    static final Log log = LogFactory.getLog(SQLTemplateStore.class);

    public SQLTemplateStore(ContextTypeRegistry registry, IPreferenceStore store, String key)
    {
        super(registry, store, key);
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
        Collection<TemplatePersistenceData> templates = new ArrayList<TemplatePersistenceData>();
        readIncludedTemplates(templates, "templates/default-templates.xml", "$nl$/templates/default-templates.properties");

        return templates;
    }

    private void readIncludedTemplates(Collection<TemplatePersistenceData> templates, String file, String translations) throws IOException
    {
        if (file != null) {
            Bundle plugin = DBeaverActivator.getInstance().getBundle();
            URL url = FileLocator.find(plugin, Path.fromOSString(file), null);
            if (url != null) {
                InputStream bundleStream = null;
                InputStream stream = null;
                try {
                    ResourceBundle bundle = null;
                    if (translations != null) {
                        URL bundleURL = FileLocator.find(plugin, Path.fromOSString(translations), null);
                        if (bundleURL != null) {
                            bundleStream = bundleURL.openStream();
                            bundle = new PropertyResourceBundle(bundleStream);
                        }
                    }
                    stream = new BufferedInputStream(url.openStream());
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
                    ContentUtils.close(bundleStream);
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
}

