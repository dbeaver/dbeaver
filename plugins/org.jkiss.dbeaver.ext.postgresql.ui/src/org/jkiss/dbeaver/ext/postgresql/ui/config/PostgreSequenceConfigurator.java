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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateSequencePage;

/**
 * Postgre sequence configurator
 */
public class PostgreSequenceConfigurator implements DBEObjectConfigurator<PostgreSequence> {
    @Override
    public PostgreSequence configureObject(DBRProgressMonitor monitor, Object parent, PostgreSequence sequence) {
        return new UITask<PostgreSequence>() {
            @Override
            protected PostgreSequence runTask() {
                CreateSequencePage editPage = new CreateSequencePage(sequence);
                if (!editPage.edit()) {
                    return null;
                }
                sequence.setName(editPage.getSequenceName());
                return sequence;
            }
        }.execute();
    }

}
