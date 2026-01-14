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
package org.jkiss.dbeaver.core;

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

/**
 * DBeaver core features
 */
public interface CoreFeatures {

    DBRFeature CATEGORY_APPLICATION = DBRFeature.createCategory("Application", "Application lifecycle actions");

    DBRFeature APP_OPEN = DBRFeature.createFeature(CATEGORY_APPLICATION, "Open application");
    DBRFeature APP_CLOSE = DBRFeature.createFeature(CATEGORY_APPLICATION, "Close application");

    DBRFeature CATEGORY_GENERAL = DBRFeature.createCategory("General", "General app actions");
    DBRFeature GENERAL_VIEW_OPEN = DBRFeature.createFeature(CATEGORY_GENERAL, "Open view");
    DBRFeature GENERAL_VIEW_CLOSE = DBRFeature.createFeature(CATEGORY_GENERAL, "Close view");
    DBRFeature GENERAL_SHOW_PERSPECTIVE = DBRFeature.createFeature(CATEGORY_GENERAL, "Show perspective");

}