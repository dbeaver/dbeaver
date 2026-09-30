/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.List;

/**
 * A database or another kind of data source that can be accessed by one or more drivers.
 */
public interface DBPDataSourceType extends DBPNamedObject {
    String CUSTOM_ID = "custom";

    @NotNull
    String getId();

    @Nullable
    String getDescription();

    /** Brief information about the data source technology. */
    @NotNull
    default String getDataSourceInformation() {
        return getName();
    }

    @NotNull
    DBPImage getIcon();

    @NotNull
    DBPImage getIconBig();

    @Nullable
    DBPImage getLogoImage();

    @NotNull
    List<? extends DBPDriver> getDrivers();

    @NotNull
    List<? extends DBPDriver> getEnabledDrivers();

    int getPromotedScore();
}
