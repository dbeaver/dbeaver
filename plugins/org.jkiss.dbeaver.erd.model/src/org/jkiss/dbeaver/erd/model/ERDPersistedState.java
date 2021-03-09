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
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity diagram loader/saver
 */
public class ERDPersistedState {

    public static final String TAG_DIAGRAM = "diagram";
    public static final String TAG_ENTITIES = "entities";
    public static final String TAG_DATA_SOURCE = "data-source";
    public static final String TAG_ENTITY = "entity";
    public static final String TAG_PATH = "path";
    public static final String TAG_RELATIONS = "relations";
    public static final String TAG_RELATION = "relation";
    public static final String TAG_BEND = "bend";

    public static final String ATTR_VERSION = "version";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TIME = "time";
    public static final String ATTR_ALIAS = "alias";
    public static final String ATTR_ID = "id";
    public static final String ATTR_ORDER = "order";
    public static final String ATTR_TRANSPARENT = "transparent";
    public static final String ATTR_COLOR_BG = "color-bg";
    public static final String ATTR_COLOR_FG = "color-fg";
    public static final String ATTR_FONT = "font";
    public static final String ATTR_BORDER_WIDTH = "border-width";
    public static final String ATTR_FQ_NAME = "fq-name";
    public static final String ATTR_REF_NAME = "ref-name";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_PK_REF = "pk-ref";
    public static final String ATTR_FK_REF = "fk-ref";
    public static final String ATTR_ATTRIBUTE_VISIBILITY = "showAttrs";
    public static final String TAG_COLUMN = "column";
    public static final String ATTR_X = "x";
    public static final String ATTR_Y = "y";
    public static final String ATTR_W = "w";
    public static final String ATTR_H = "h";

    public static final int ERD_VERSION_1 = 1;
    public static final String BEND_ABSOLUTE = "abs";
    public static final String BEND_RELATIVE = "rel";
    public static final String TAG_NOTES = "notes";
    public static final String TAG_NOTE = "note";

    public static List<DBPDataSourceContainer> extractContainers(IFile resource)
        throws IOException, XMLException, DBException
    {
        List<DBPDataSourceContainer> containers = new ArrayList<>();

        DBPProject projectMeta = DBWorkbench.getPlatform().getWorkspace().getProject(resource.getProject());
        if (projectMeta == null) {
            return containers;
        }
        try (InputStream is = resource.getContents()) {
            final Document document = XMLUtils.parseDocument(is);
            final Element diagramElem = document.getDocumentElement();

            final Element entitiesElem = XMLUtils.getChildElement(diagramElem, TAG_ENTITIES);
            if (entitiesElem != null) {
                // Parse data source
                for (Element dsElem : XMLUtils.getChildElementList(entitiesElem, TAG_DATA_SOURCE)) {
                    String dsId = dsElem.getAttribute(ATTR_ID);
                    if (!CommonUtils.isEmpty(dsId)) {
                        // Get connected datasource
                        final DBPDataSourceContainer dataSourceContainer = projectMeta.getDataSourceRegistry().getDataSource(dsId);
                        if (dataSourceContainer != null) {
                            containers.add(dataSourceContainer);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            throw new DBException("Error reading resource contents", e);
        }
        return containers;
    }


}
