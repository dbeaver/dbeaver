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

package org.jkiss.dbeaver.ext.mysql.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.PairListControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PrivilegesPairList
 */
public class PrivilegesPairList extends PairListControl<String> {

    public PrivilegesPairList(Composite parent) {
        super(parent, SWT.NONE, "Available", "Granted");
    }

    public void setModel(Map<String, Boolean> privs)
    {
        List<String> availPrivs = new ArrayList<>();
        List<String> grantedPrivs = new ArrayList<>();
        for (Map.Entry<String,Boolean> priv : privs.entrySet()) {
            if (priv.getValue()) {
                grantedPrivs.add(priv.getKey());
            } else {
                availPrivs.add(priv.getKey());
            }
        }
        super.setModel(availPrivs, grantedPrivs);
    }
}
