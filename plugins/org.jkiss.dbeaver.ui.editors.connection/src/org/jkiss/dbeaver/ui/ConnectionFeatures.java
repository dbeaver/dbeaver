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
package org.jkiss.dbeaver.ui;

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

/**
 * DBeaver connection features
 */
public interface ConnectionFeatures {

    DBRFeature CATEGORY_CONNECTION = DBRFeature.createCategory("Connection", "Connection actions");
    DBRFeature CONNECTION_DELETE = DBRFeature.createFeature(CATEGORY_CONNECTION, "Delete connection");
    DBRFeature CONNECTION_TEST = DBRFeature.createFeature(CATEGORY_CONNECTION, "Test connection");
    DBRFeature CONNECTION_EDIT = DBRFeature.createFeature(CATEGORY_CONNECTION, "Edit connection");
    DBRFeature CONNECTION_CREATE = DBRFeature.createFeature(CATEGORY_CONNECTION, "Create connection");
    DBRFeature CONNECTION_CLOSE = DBRFeature.createFeature(CATEGORY_CONNECTION, "Close connection");
    DBRFeature CONNECTION_OPEN = DBRFeature.createFeature(CATEGORY_CONNECTION, "Open connection");
}