/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;

import java.util.Collection;

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
                if (objects == null) {
                    return null;
                }

                return DiagramObjectCollector.generateEntityList(
                    ((DiagramPart) getViewer().getRootEditPart().getContents()).getDiagram(),
                    objects);
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

