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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthProfile;
import org.jkiss.dbeaver.model.app.DBPProject;

/**
 * Data source descriptors constants
 */
public class SecretKeyConstants {

    public static final String DATASOURCE_KEY_PREFIX = "/datasources/";
    public static final String PROFILE_KEY_PREFIX = "/profiles/";


    public static String getSecretKeyId(DBPDataSourceContainer dataSource) {
        return dataSource.getProject().getName() + DATASOURCE_KEY_PREFIX + dataSource.getId();
    }

    public static String getSecretKeyId(DBPProject project, DBAAuthProfile profile) {
        return project.getName() + PROFILE_KEY_PREFIX + profile.getProfileId();
    }

}
