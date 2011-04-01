/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

/**
 * IProgressControlProvider
 */
public interface IProgressControlProvider
{
    ProgressPageControl getProgressControl();
}