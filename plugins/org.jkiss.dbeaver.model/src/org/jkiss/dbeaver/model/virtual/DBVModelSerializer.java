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
package org.jkiss.dbeaver.model.virtual;

/**
 * DBVModelSerializer
 */
interface DBVModelSerializer {

    String TAG_CONTAINER = "container"; //$NON-NLS-1$
    String TAG_ENTITY = "entity"; //$NON-NLS-1$
    String TAG_CONSTRAINT = "constraint"; //$NON-NLS-1$
    String TAG_ASSOCIATION = "association"; //$NON-NLS-1$
    String TAG_ATTRIBUTE = "attribute"; //$NON-NLS-1$
    String ATTR_ID = "id"; //$NON-NLS-1$
    String ATTR_NAME = "name"; //$NON-NLS-1$
    String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
    String ATTR_CUSTOM = "custom"; //$NON-NLS-1$
    String ATTR_ENTITY = "entity"; //$NON-NLS-1$
    String ATTR_CONSTRAINT = "constraint"; //$NON-NLS-1$
    String TAG_PROPERTY = "property"; //$NON-NLS-1$
    String ATTR_VALUE = "value"; //$NON-NLS-1$
    String ATTR_TYPE = "type"; //$NON-NLS-1$
    String TAG_COLORS = "colors";
    String TAG_COLOR = "color";
    String ATTR_OPERATOR = "operator";
    String ATTR_RANGE = "range";
    String ATTR_SINGLE_COLUMN = "singleColumn";
    String ATTR_FOREGROUND = "foreground";
    String ATTR_BACKGROUND = "background";
    String ATTR_FOREGROUND2 = "foreground2";
    String ATTR_BACKGROUND2 = "background2";

    String TAG_VALUE = "value";
    String TAG_TRANSFORM = "transform";
    String TAG_INCLUDE = "include";
    String TAG_EXCLUDE = "exclude";


}
