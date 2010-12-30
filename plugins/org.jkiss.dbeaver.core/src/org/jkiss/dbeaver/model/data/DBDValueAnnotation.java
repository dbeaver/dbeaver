/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.graphics.Image;

/**
 * Value annotation
 */
public interface DBDValueAnnotation {

    Image getImage();

    String getToolTip();
}
