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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;

/**
 * DB editor utils
 */
public class DatabaseEditorUtils {

    public static void setPartBackground(IEditorPart editor, Composite composite)
    {
        if (composite == null || composite.isDisposed()) {
            return;
        }
        CTabFolder tabFolder = null;
        Composite rootComposite = composite;
        for (Composite c = composite; c != null; c = c.getParent()) {
            if (!c.isDisposed() && c.getParent() instanceof CTabFolder) {
                tabFolder = (CTabFolder) c.getParent();
                //rootComposite = c;
                break;
            }
        }
        if (tabFolder != null) {
            tabFolder.setBorderVisible(false);
        }

        DBPDataSourceContainer dsContainer = null;
        if (editor instanceof IDataSourceContainerProvider) {
            dsContainer = ((IDataSourceContainerProvider) editor).getDataSourceContainer();
        } else if (editor instanceof DBPContextProvider) {
            DBCExecutionContext context = ((DBPContextProvider) editor).getExecutionContext();
            if (context != null) {
                dsContainer = context.getDataSource().getContainer();
            }
        }

        if (dsContainer == null) {
            rootComposite.setBackground(null);
        } else {
            Color bgColor = UIUtils.getConnectionColor(dsContainer.getConnectionConfiguration());

            rootComposite.setData(DBStyles.DATABASE_EDITOR_COMPOSITE_DATASOURCE, dsContainer);
            rootComposite.setBackground(bgColor);
        }
    }

    public static void contributeStandardEditorActions(IWorkbenchSite workbenchSite, IContributionManager contributionManager)
    {
        contributionManager.add(ActionUtils.makeCommandContribution(
            workbenchSite,
            IWorkbenchCommandConstants.FILE_SAVE,
            null,
            UIIcon.SAVE,
            null,
            true));
        contributionManager.add(ActionUtils.makeCommandContribution(
            workbenchSite,
            IWorkbenchCommandConstants.FILE_REVERT,
            null,
            UIIcon.RESET,
            null,
            true));
    }

}
