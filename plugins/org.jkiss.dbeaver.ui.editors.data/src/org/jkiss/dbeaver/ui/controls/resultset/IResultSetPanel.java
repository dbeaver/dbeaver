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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.editors.IActionContributor;

/**
 * ResultSet panel.
 * RSV can embed multiple panels to provide additional visualization functionality
 */
public interface IResultSetPanel extends IActionContributor {

    Control createContents(IResultSetPresentation presentation, Composite parent);

    boolean isDirty();

    void activatePanel();

    void deactivatePanel();

    void refresh(boolean force);

}
