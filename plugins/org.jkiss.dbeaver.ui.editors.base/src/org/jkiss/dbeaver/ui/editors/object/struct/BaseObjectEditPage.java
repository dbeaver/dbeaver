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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIdProvider;
import org.jkiss.dbeaver.ui.UIUtils;

public abstract class BaseObjectEditPage extends DialogPage {

    private EditObjectDialog container;

    public BaseObjectEditPage(String title)
    {
        super(title);
    }

    public BaseObjectEditPage(String title, DBIcon icon) {
        super(title, DBeaverIcons.getImageDescriptor(icon));
    }

    @Override
    public void performHelp() {
        if (this instanceof IHelpContextIdProvider) {
            IWorkbenchHelpSystem helpSystem = UIUtils.getActiveWorkbenchWindow().getWorkbench().getHelpSystem();
            if (helpSystem != null) {
                String helpContextId = ((IHelpContextIdProvider) this).getHelpContextId();
                IContext helpContext = new IContext() {
                    @Override
                    public IHelpResource[] getRelatedTopics() {
                        return new IHelpResource[] {
                            new IHelpResource() {
                                @Override
                                public String getHref() {
                                    return helpContextId;
                                }

                                @Override
                                public String getLabel() {
                                    return helpContextId;
                                }
                            }
                        };
                    }

                    @Override
                    public String getText() {
                        return helpContextId;
                    }
                };
                helpSystem.displayHelp(helpContext);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        Control pageContents = createPageContents(parent);
        setControl(pageContents);
        pageContents.addHelpListener(e -> performHelp());
    }


    public boolean isPageComplete() {
        return true;
    }

    protected void updatePageState() {
        if (container != null) {
            UIUtils.asyncExec(() -> container.updateButtons());
        }
    }

    void setContainer(EditObjectDialog container) {
        this.container = container;
    }

    public void performFinish() throws DBException {

    }

    protected abstract Control createPageContents(Composite parent);

    public boolean edit() {
        return EditObjectDialog.showDialog(this);
    }

    public boolean edit(Shell shell) {
        return EditObjectDialog.showDialog(shell, this);
    }
}
