/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.swt.graphics.Image;

/**
 * Database persist action
 */
public interface IDatabasePersistAction {

    String getTitle();

    Image getIcon();

    String getScript();

    String getUndoScript();

}
