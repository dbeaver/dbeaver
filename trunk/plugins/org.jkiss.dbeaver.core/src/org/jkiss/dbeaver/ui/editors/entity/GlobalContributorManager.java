/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorContributorManager;

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
        final List<IEditorPart> editors = new ArrayList<IEditorPart>();

        private ActionContributorInfo(IEditorActionBarContributor contributor)
        {
            this.contributor = contributor;
        }
    }

    private Map<Class<? extends IEditorActionBarContributor>, ActionContributorInfo> contributorMap = new HashMap<Class<? extends IEditorActionBarContributor>, ActionContributorInfo>();

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
