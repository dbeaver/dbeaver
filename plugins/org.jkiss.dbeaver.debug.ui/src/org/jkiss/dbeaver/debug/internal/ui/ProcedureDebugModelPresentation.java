/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.internal.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.ui.DatabaseDebugModelPresentation;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

public class ProcedureDebugModelPresentation extends DatabaseDebugModelPresentation {

    @Override
    public IEditorInput getEditorInput(Object element)
    {
        if (element instanceof DBNDatabaseNode) {
            DBNDatabaseNode dbnNode = (DBNDatabaseNode) element;
            return new EntityEditorInput(dbnNode);
        }
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element)
    {
        //FIXME:AF: is there a constant anywhere? 
        return "org.jkiss.dbeaver.ui.editors.entity.EntityEditor";
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener)
    {
        try {
            String valueString = value.getValueString();
            listener.detailComputed(value, valueString);
        } catch (DebugException e) {
            String message = NLS.bind("Unable to compute valie for {0}", value);
            IStatus status = DebugCore.newErrorStatus(message, e);
            DebugCore.log(status);
            listener.detailComputed(value, e.getMessage());
        }
    }

}
