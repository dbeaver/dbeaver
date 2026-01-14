/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.settings;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.LocalizedPropertyDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductSettingDescriptor extends LocalizedPropertyDescriptor {
    private final List<String> scopes = new ArrayList<>();

    public ProductSettingDescriptor(String category, IConfigurationElement cfg) {
        super(category, cfg);
        String excludeAttr = cfg.getAttribute("scopes");
        if (CommonUtils.isNotEmpty(excludeAttr)) {
            scopes.addAll(Arrays.stream(excludeAttr.split(",")).toList());
        }
    }
    @NotNull
    public List<String> getScopes() {
        return scopes;
    }

}
