/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorActionBarContributor;

/**
 * Contributor manager
 */
public interface IDatabaseEditorContributorManager {

    IEditorActionBarContributor getContributor(Class<? extends IEditorActionBarContributor> type);

}
