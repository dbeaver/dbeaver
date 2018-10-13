/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.driver.LocalNativeClientLocation;

/**
 * PostgreServerHome
 */
public class PostgreServerHome extends LocalNativeClientLocation {

    private static final Log log = Log.getLog(PostgreServerHome.class);

    private String name;
    private String version;
    private String branding;
    private String dataDirectory;

    protected PostgreServerHome(String id, String path, String version, String branding, String dataDirectory) {
        super(id, path);
        this.name = branding == null ? id : branding;
        this.version = version;
        this.branding = branding;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public String getProductName() {
        return branding;
    }

    public String getProductVersion() {
        return version;
    }

    public String getBranding() {
        return branding;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

}
