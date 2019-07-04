/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.utils.CommonUtils;

public class PostgreToolExtensionUninstall implements IUserInterfaceTool {

    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
            throws DBException {
        List<PostgreObject> extensionList = CommonUtils.filterCollection(objects, PostgreObject.class);
        if (!extensionList.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), extensionList);
            if (dialog.open() == IDialogConstants.OK_ID) {
                refreshObjectsState(extensionList);
            }
        }
        
    }
    private void refreshObjectsState(List<PostgreObject> extList) {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                for (PostgreObject ext : extList) {
                    try {
                        DBNDatabaseNode extNode = DBNUtils.getNodeByObject(ext);
                        if (extNode != null) {
                            extNode.refreshNode(monitor, PostgreToolExtensionUninstall.this);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Refresh extensions state", "Error refreshign extension state", e.getTargetException());
        }
    }

    class SQLDialog extends TableToolDialog {

        public SQLDialog(IWorkbenchPartSite partSite, List<PostgreObject> exts) {
            super(partSite,  "Uninstall extension", exts);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            lines.add("DROP EXTENSION " + ((PostgreObject) object).getName() + " CASCADE");
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }
}
