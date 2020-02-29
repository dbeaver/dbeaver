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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorContributorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global contributor manager
 */
public class GlobalContributorManager implements IDatabaseEditorContributorManager{

    private static GlobalContributorManager instance = new GlobalContributorManager();

    private static class ActionContributorInfo {
        final IEditorActionBarContributor contributor;
        final List<IEditorPart> editors = new ArrayList<>();

        private ActionContributorInfo(IEditorActionBarContributor contributor)
        {
            this.contributor = contributor;
        }
    }

    private Map<Class<? extends IEditorActionBarContributor>, ActionContributorInfo> contributorMap = new HashMap<>();

    public static GlobalContributorManager getInstance()
    {
        return instance;
    }

    @Override
    public IEditorActionBarContributor getContributor(Class<? extends IEditorActionBarContributor> type)
    {
        ActionContributorInfo info = contributorMap.get(type);
        return info == null ? null : info.contributor;
    }

    public void addContributor(IEditorActionBarContributor contributor, IEditorPart editor)
    {
        ActionContributorInfo info = contributorMap.get(contributor.getClass());
        if (info == null) {
            contributor.init(editor.getEditorSite().getActionBars(), editor.getSite().getPage());
            info = new ActionContributorInfo(contributor);
            contributorMap.put(contributor.getClass(), info);
        }
        info.editors.add(editor);
    }

    public void removeContributor(IEditorActionBarContributor contributor, IEditorPart editor)
    {
        ActionContributorInfo info = contributorMap.get(contributor.getClass());
        if (info == null) {
            throw new IllegalStateException("Contributor is not registered");
        }
        if (!info.editors.remove(editor)) {
            throw new IllegalStateException("Contributor editor is not registered");
        }
        if (info.editors.isEmpty()) {
            contributorMap.remove(contributor.getClass());
            contributor.dispose();
        }
    }

}
