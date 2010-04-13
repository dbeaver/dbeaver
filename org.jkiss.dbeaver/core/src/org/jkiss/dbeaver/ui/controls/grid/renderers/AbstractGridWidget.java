/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.renderers;

import org.jkiss.dbeaver.ui.controls.grid.renderers.AbstractRenderer;
import org.jkiss.dbeaver.ui.controls.grid.renderers.IGridWidget;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA 
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p> 
 * Base implementation of IRenderer and IInternalWidget. Provides management of
 * a few values. 
 * 
 * @see org.jkiss.dbeaver.ui.controls.grid.renderers.AbstractRenderer
 * @author chris.gross@us.ibm.com
 */
public abstract class AbstractGridWidget extends AbstractRenderer implements IGridWidget
{

    String hoverDetail = "";

    /**
     * @return the hoverDetail
     */
    public String getHoverDetail()
    {
        return hoverDetail;
    }

    /**
     * @param hoverDetail the hoverDetail to set
     */
    public void setHoverDetail(String hoverDetail)
    {
        this.hoverDetail = hoverDetail;
    }

}
