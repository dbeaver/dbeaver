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

package org.jkiss.dbeaver.runtime.ide.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DBeaverIDECore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.runtime.ide.core"; //$NON-NLS-1$

    public static final String MARKER_ID_DATASOURCE = BUNDLE_SYMBOLIC_NAME + '.' + "datasourceMarker"; //$NON-NLS-1$

    public static final String MARKER_ATTRIBUTE_DATASOURCE_ID = BUNDLE_SYMBOLIC_NAME + '.' + "datasourceId"; //$NON-NLS-1$
    public static final String MARKER_ATTRIBUTE_NODE_PATH = BUNDLE_SYMBOLIC_NAME + '.' + "nodePath"; //$NON-NLS-1$

    public static IResource resolveWorkspaceResource(DBSObject dbsObject) {
        WorkspaceResourceResolver resolver = Adapters.adapt(dbsObject, WorkspaceResourceResolver.class, true);
        if (resolver != null) {
            return resolver.resolveResource(dbsObject);
        }
        return null;
    }

    public static IStatus createError(String message) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message);
    }

    public static IStatus createError(String message, Throwable t) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, t);
    }

    public static IStatus createCancel(String message) {
        return new Status(IStatus.CANCEL, BUNDLE_SYMBOLIC_NAME, message);
    }

}
