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

package org.jkiss.dbeaver.tools.transfer.stream.model;

import org.jkiss.dbeaver.model.impl.AbstractDataSourceInfo;
import org.osgi.framework.Version;

class StreamDataSourceInfo extends AbstractDataSourceInfo {
    @Override
    public String getDatabaseProductName() {
        return "stream";
    }

    @Override
    public String getDatabaseProductVersion() {
        return "1.0";
    }

    @Override
    public Version getDatabaseVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public String getDriverName() {
        return "stream";
    }

    @Override
    public String getDriverVersion() {
        return "1.0";
    }

    @Override
    public String getSchemaTerm() {
        return null;
    }

    @Override
    public String getProcedureTerm() {
        return null;
    }

    @Override
    public String getCatalogTerm() {
        return null;
    }
}
