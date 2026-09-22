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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserAuthorization;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Add Authorization" dialog for a Mimer SQL user - Type (a real combo, though {@code OS_USER}
 * is the only value Mimer SQL currently supports; kept as a combo rather than a fixed label so a
 * future second authorization kind needs no dialog change here) and the OS user name itself.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateUserAuthorizationPage extends BaseObjectEditPage {

    private final MimerUserAuthorization authorization;

    private String authType = "OS_USER";
    private String osUser = "";

    public MimerCreateUserAuthorizationPage(@NotNull MimerUserAuthorization authorization) {
        super("Add authorization");
        this.authorization = authorization;
    }

    @Override
    public DBSObject getObject() {
        return authorization;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = UIUtils.createComposite(parent, 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "User", authorization.getUser().getName()).setEditable(false);

        Combo typeCombo = UIUtils.createLabelCombo(group, "Authorization type", SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.add("OS_USER");
        typeCombo.select(0);
        typeCombo.addModifyListener(e -> authType = typeCombo.getText());

        Text osUserText = UIUtils.createLabelText(group, "OS user", "");
        osUserText.addModifyListener(e -> {
            osUser = osUserText.getText();
            updatePageState();
        });
        osUserText.setFocus();

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(osUser);
    }

    /**
     * Applies the collected values to the authorization. Call after {@link #edit()} returns
     * {@code true}.
     */
    public void applyChanges() {
        authorization.setAuthorizationType(authType);
        authorization.setName(osUser.trim());
    }
}
