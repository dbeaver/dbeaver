/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.runtime.ide.ui.texteditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import static org.jkiss.dbeaver.runtime.DBMarkers.MARKER_ATTRIBUTE_DATASOURCE_ID;
import static org.jkiss.dbeaver.runtime.DBMarkers.MARKER_ATTRIBUTE_NODE_PATH;

public class DatabaseMarkerAnnotationModel extends ResourceMarkerAnnotationModel {

    private static final Log log = Log.getLog(DatabaseMarkerAnnotationModel.class);

    private final static String[] ATTRIBUTE_NAMES = new String[] { //
            MARKER_ATTRIBUTE_DATASOURCE_ID, //
            MARKER_ATTRIBUTE_NODE_PATH };

    private final DBSObject databaseObject;
    private final DBNNode node;

    private final String datasourceId;
    private final String nodeItemPath;

    public DatabaseMarkerAnnotationModel(DBSObject databaseObject, DBNDatabaseNode node, IResource resource) {
        super(resource);
        this.databaseObject = databaseObject;
        this.node = node;
        this.datasourceId = databaseObject.getDataSource().getContainer().getId();
        this.nodeItemPath = node.getNodeItemPath();
    }

    protected DBSObject getDatabaseObject() {
        return databaseObject;
    }
    
    protected DBNNode getNode() {
        return node;
    }

    @Override
    protected boolean isAcceptable(IMarker marker) {
        boolean acceptable = super.isAcceptable(marker);
        if (!acceptable) {
            return false;
        }
        try {
            Object[] attributes = marker.getAttributes(ATTRIBUTE_NAMES);
            return datasourceId.equals(attributes[0]) &&
                nodeItemPath.equals(attributes[1]);
        } catch (CoreException e) {
            log.log(e.getStatus());
            return false;
        }
    }

}
