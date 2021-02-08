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
package org.jkiss.dbeaver.ext.hana.ui.internal;

public enum HANAEdition {
    GENERIC(HANAMessages.edition_generic),
    PLATFORM_SINGLE_DB(HANAMessages.edition_platform_single_db),
    PLATFORM_SYSTEM_DB(HANAMessages.edition_platform_system_db),
    PLATFORM_TENANT_DB(HANAMessages.edition_platform_tenant_db),
    EXPRESS(HANAMessages.edition_express),
    CLOUD(HANAMessages.edition_cloud);
        
    private final String title;

    HANAEdition(String title)
    {
        this.title = title;
    }

    public String getTitle() { return title; }
    
    public static HANAEdition fromName(String name) {
        for(HANAEdition edition : HANAEdition.values()) {
            if(edition.name().equals(name)) {
                return edition;
            }
        }
        return GENERIC;
    }
    
    public static HANAEdition fromTitle(String title) {
        for(HANAEdition edition : HANAEdition.values()) {
            if(edition.getTitle().equals(title)) {
                return edition;
            }
        }
        return GENERIC;
    }
    
}
