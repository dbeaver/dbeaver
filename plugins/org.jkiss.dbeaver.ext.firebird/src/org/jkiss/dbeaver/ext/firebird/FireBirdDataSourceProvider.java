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
package org.jkiss.dbeaver.ext.firebird;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

import java.io.File;

public class FireBirdDataSourceProvider extends GenericDataSourceProvider<FireBirdDataSource> {

    private static final String PROP_NATIVE_LIBRARY_PATH = "nativeLibraryPath";

    public FireBirdDataSourceProvider() {
        super(FireBirdDataSource.class);
    }

    @Override
    public long getFeatures() {
        return FEATURE_NONE;
    }

    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo) {
        // Forward nativeLibraryPath to jna.library.path for Jaybird 5.x embedded/native support.
        // Jaybird 6+ handles this natively via the JDBC connection property.
        String libPath = connectionInfo.getProviderProperty(PROP_NATIVE_LIBRARY_PATH);
        if (CommonUtils.isEmptyTrimmed(libPath)) {
            libPath = CommonUtils.toString(driver.getDriverParameter(PROP_NATIVE_LIBRARY_PATH));
        }
        if (!CommonUtils.isEmptyTrimmed(libPath)) {
            String trimmed = libPath.trim();
            String existing = System.getProperty("jna.library.path");
            if (existing == null || existing.isEmpty()) {
                System.setProperty("jna.library.path", trimmed);
            } else if (!existing.contains(trimmed)) {
                System.setProperty("jna.library.path", trimmed + File.pathSeparator + existing);
            }
        }
        return super.getConnectionURL(driver, connectionInfo);
    }

}
