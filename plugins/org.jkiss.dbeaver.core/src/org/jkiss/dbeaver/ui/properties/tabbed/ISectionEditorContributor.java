/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.eclipse.ui.IEditorActionBarContributor;

import java.util.List;

/**
 * ISection which provides action contributions
 */
public interface ISectionEditorContributor {

    void addContributions(List<IEditorActionBarContributor> contributions);

}
