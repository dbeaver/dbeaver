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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.List;

/**
 * Data source provider
 */
public interface DBPDataSourceProviderRegistry {

    List<? extends DBPDataSourceProviderDescriptor> getDataSourceProviders();

    List<? extends DBPDataSourceProviderDescriptor> getEnabledDataSourceProviders();

    DBPDataSourceProviderDescriptor getDataSourceProvider(String id);
    DBPDataSourceProviderDescriptor makeFakeProvider(String providerID);

    DBPAuthModelDescriptor getAuthModel(String id);
    List<? extends DBPAuthModelDescriptor> getAllAuthModels();
    List<? extends DBPAuthModelDescriptor> getApplicableAuthModels(DBPDataSourceContainer dataSourceContainer);

    DBPConnectionType getConnectionType(String id, DBPConnectionType defaultType);
    void addConnectionType(DBPConnectionType connectionType);
    void removeConnectionType(DBPConnectionType connectionType);
    void saveConnectionTypes();

    DBPDriver findDriver(String driverIdOrName);

    DBPEditorContribution[] getContributedEditors(String category, DBPDataSourceContainer dataSource);

    // This pref store can be used to listen preference changes in ANY datasource.
    DBPPreferenceStore getGlobalDataSourcePreferenceStore();

}
