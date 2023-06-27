/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.ui.config;

import org.jkiss.dbeaver.ext.altibase.model.AltibasePackage;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * AltibasePackageConfigurator
 */
public class AltibasePackageConfigurator implements DBEObjectConfigurator<AltibasePackage> {

    @Override
    public AltibasePackage configureObject(DBRProgressMonitor monitor, Object container, AltibasePackage altibasePackage, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(altibasePackage.getDataSource(), DBSEntityType.PACKAGE);
            if (!editPage.edit()) {
                return null;
            }
            String packName = editPage.getEntityName();
            /*
            altibasePackage.setName(packName);
            altibasePackage.setObjectDefinitionText(
                "CREATE OR REPLACE PACKAGE " + packName + "\n" +
                    "AS\n" +
                    "-- Package header\n" +
                    "END " + packName + ";");
            altibasePackage.setExtendedDefinitionText(
                "CREATE OR REPLACE PACKAGE BODY " + packName + "\n" +
                    "AS\n" +
                    "-- Package body\n" +
                    "END " + packName + ";");
                    */
            return altibasePackage;
        });
    }

}