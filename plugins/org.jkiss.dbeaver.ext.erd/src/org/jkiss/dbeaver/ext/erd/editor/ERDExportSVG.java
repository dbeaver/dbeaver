/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;

/**
 * SVG exporter
 */
public class ERDExportSVG
{
    private static final Log log = Log.getLog(ERDExportSVG.class);

    private final EntityDiagram diagram;
    private final DiagramPart diagramPart;

    public ERDExportSVG(EntityDiagram diagram, DiagramPart diagramPart) {
        this.diagram = diagram;
        this.diagramPart = diagramPart;
    }


    public void exportToSVG(String filePath) {

    }
}
