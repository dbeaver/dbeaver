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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateStore;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;

/**
 * Templates preference page
 */
public class PrefPageSQLTemplates extends TemplatePreferencePage implements IWorkbenchPropertyPage {
    public PrefPageSQLTemplates()
    {
        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
        setTemplateStore(SQLTemplatesRegistry.getInstance().getTemplateStore());
        setContextTypeRegistry(SQLTemplatesRegistry.getInstance().getTemplateContextRegistry());
    }

    protected String getFormatterPreferenceKey() {
        return SQLTemplateStore.PREF_STORE_KEY;
    }

    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }
}
