/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;

/**
 * Data source provider descriptor
 */
public interface DBPAuthModelDescriptor extends DBPNamedObject {

    @NotNull
    String getId();

    String getDescription();

    DBPImage getIcon();

    @NotNull
    String getImplClassName();

    boolean isDefaultModel();

    // Model works in desktop application only
    boolean isDesktopModel();

    // Model needs a configuration on a local FS
    boolean requiresLocalConfiguration();

    boolean isApplicableTo(DBPDriver driver);

    // Auth model which replaced this one. Usually null
    @Nullable
    DBPAuthModelDescriptor getReplacedBy(@NotNull DBPDriver driver);

    @NotNull
    <T extends DBAAuthCredentials> DBAAuthModel<T> getInstance();

    @NotNull
    DBPPropertySource createCredentialsSource(@Nullable DBPDataSourceContainer dataSource, @Nullable DBPConnectionConfiguration configuration);

    /**
     * Returns id of required external authorization.
     * Null - if additional authorization is not required
     */
    @Nullable
    String getRequiredAuthProviderId();
}
