/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.erd.ui.dnd;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef3.EditPartViewer;
import org.eclipse.gef3.Request;
import org.eclipse.gef3.RequestConstants;
import org.eclipse.gef3.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef3.requests.CreateRequest;
import org.eclipse.gef3.requests.CreationFactory;
import org.jkiss.dbeaver.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.model.DiagramCollectSettingsDefault;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a listener for dropping nodes onto the editor drawing
 */
public class NodeDropTargetListener extends AbstractTransferDropTargetListener {

    public NodeDropTargetListener(EditPartViewer viewer)
    {
        super(viewer, DatabaseObjectTransfer.getInstance());
    }

    @Override
    protected void updateTargetRequest()
    {

    }

    @Override
    protected Request createTargetRequest()
    {
        CreateRequest request = new CreateRequest();
        request.setFactory(new CreationFactory() {
            @Override
            public Object getNewObject()
            {
                Collection<DBPNamedObject> objects = DatabaseObjectTransfer.getInstance().getObject();
                ERDEditorPart editor = ((DiagramPart) getViewer().getRootEditPart().getContents()).getEditor();
                if (objects == null) {
                    return null;
                }

                DBRRunnableWithResult<List<ERDEntity>> collector = new DBRRunnableWithResult<List<ERDEntity>>() {
                    @Override
                    public void run(DBRProgressMonitor monitor) {
                        result = DiagramObjectCollector.generateEntityList(
                            monitor,
                            editor.getDiagram(),
                            editor.getDiagramProject(),
                            objects,
                            new DiagramCollectSettingsDefault(),
                            true);
                    }
                };
                try {
                    UIUtils.runInProgressService(collector);
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError("Entity collect error", "Error during diagram entities collect", e);
                } catch (InterruptedException e) {
                    // ignore
                }
                final List<String> errorMessages = editor.getDiagram().getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    final List<Status> statuses = new ArrayList<>(errorMessages.size());
                    for (String error : errorMessages) {
                        statuses.add(new Status(Status.ERROR, ERDUIActivator.PLUGIN_ID, error));
                    }
                    DBWorkbench.getPlatformUI().showError(
                        "Diagram request error",
                        "Error(s) occurred during diagram request. If these errors are recoverable then fix errors and then repeat request",
                        new MultiStatus(ERDUIActivator.PLUGIN_ID, 0, statuses.toArray(new IStatus[0]), null, null)
                    );
                    editor.getDiagram().clearErrorMessages();
                }
                return collector.getResult();
            }

            @Override
            public Object getObjectType()
            {
                return RequestConstants.REQ_CREATE;
            }
        });
        request.setLocation(getDropLocation());
        return request;
    }

}

