/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.part;

import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;

import java.lang.reflect.InvocationTargetException;

public class RearrangeDiagramService extends AbstractLoadService<EntityDiagram> {

    private DiagramPart diagram;

    public RearrangeDiagramService(DiagramPart diagram) {
        super(ERDUIMessages.erd_rearrange_diagram_job_title);
        this.diagram = diagram;
    }

    @Override
    public EntityDiagram evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        diagram.getDiagram().setDiagramMonitor(monitor);
        monitor.subTask(ERDUIMessages.erd_job_rearrange_diagram);
        diagram.rearrangeDiagram(monitor);
        return diagram.getDiagram();
    }

    @Override
    public Object getFamily() {
        return diagram;
    }

}
