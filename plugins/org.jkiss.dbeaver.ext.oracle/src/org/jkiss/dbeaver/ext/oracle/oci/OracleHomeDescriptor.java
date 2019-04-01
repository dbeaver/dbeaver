/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.oci;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.model.connection.LocalNativeClientLocation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OracleHomeDescriptor extends LocalNativeClientLocation
{
    private static final Log log = Log.getLog(OracleHomeDescriptor.class);

    private Integer oraVersion; // short version (9, 10, 11...)
    private String displayName;
    private List<String> tnsNames;

    public OracleHomeDescriptor(String oraHome)
    {
        super(CommonUtils.removeTrailingSlash(oraHome), oraHome);
        this.oraVersion = OracleDataSourceProvider.getOracleVersion(this);
        if (oraVersion == null) {
            log.debug("Unrecognized Oracle client version at " + oraHome);
        }
        this.displayName = OCIUtils.readWinRegistry(oraHome, OCIUtils.WIN_REG_ORA_HOME_NAME);
    }

    @Override
    public String getDisplayName()
    {
        if (displayName != null) {
            return displayName;
        }
        else {
            return getName();
        }
    }

    public Collection<String> getOraServiceNames()
    {
        if (tnsNames == null) {
            tnsNames = new ArrayList<>(OCIUtils.readTnsNames(getPath(), true).keySet());
        }
        return tnsNames;
    }

}
