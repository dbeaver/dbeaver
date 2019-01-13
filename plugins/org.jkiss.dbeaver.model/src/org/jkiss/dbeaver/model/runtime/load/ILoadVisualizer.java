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

package org.jkiss.dbeaver.model.runtime.load;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Lazy loading visualizer
 */
public interface ILoadVisualizer<RESULT> {

    /**
     * Allows visualizer to overwrite monitor by its own implementation.
     * By default returns passed one
     * @param monitor monitor
     * @return new or original monitor
     */
    DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor);

    boolean isCompleted();

    void visualizeLoading();

    void completeLoading(RESULT result);

}