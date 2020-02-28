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
package org.jkiss.dbeaver.debug;

import org.jkiss.dbeaver.runtime.DBMarkers;

public class DBGConstants {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.core"; //$NON-NLS-1$

    public static final String ATTR_DATASOURCE_ID = BUNDLE_SYMBOLIC_NAME +  ".ATTR_DATASOURCE_ID"; //$NON-NLS-1$
    public static final String ATTR_DEBUG_TYPE = BUNDLE_SYMBOLIC_NAME + ".ATTR_DEBUG_TYPE"; //$NON-NLS-1$

    public static final String SOURCE_CONTAINER_TYPE_DATASOURCE = BUNDLE_SYMBOLIC_NAME + ".datasourceSourceContainerType"; //$NON-NLS-1$
    public static final String BREAKPOINT_ID_DATABASE_LINE = BUNDLE_SYMBOLIC_NAME + ".databaseLineBreakpointMarker"; //$NON-NLS-1$
    public static final String MODEL_IDENTIFIER_DATABASE = BUNDLE_SYMBOLIC_NAME + ".database"; //$NON-NLS-1$
    public static final String BREAKPOINT_ATTRIBUTE_DATASOURCE_ID = DBMarkers.MARKER_ATTRIBUTE_DATASOURCE_ID;
    public static final String BREAKPOINT_ATTRIBUTE_NODE_PATH = DBMarkers.MARKER_ATTRIBUTE_NODE_PATH;
    public static final String BREAKPOINT_ATTRIBUTE_OBJECT_NAME = BUNDLE_SYMBOLIC_NAME + ".objectName"; //$NON-NLS-1$

}
