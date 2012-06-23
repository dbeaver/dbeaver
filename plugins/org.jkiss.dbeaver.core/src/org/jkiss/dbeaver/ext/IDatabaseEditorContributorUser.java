/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorActionBarContributor;

/**
 * ISection which provides action contributions
 */
public interface IDatabaseEditorContributorUser {

    /**
     * Returns null or contributor (new or obtained from manager)
     * then it should use it and return null.
     * @param manager contributor manager
     * @return null or contributor
     */
    IEditorActionBarContributor getContributor(IDatabaseEditorContributorManager manager);

}
