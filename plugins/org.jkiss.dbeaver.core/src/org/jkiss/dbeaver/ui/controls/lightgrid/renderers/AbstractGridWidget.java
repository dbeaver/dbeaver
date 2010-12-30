/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 */
public abstract class AbstractGridWidget extends AbstractRenderer implements IGridWidget
{

    private String hoverDetail = "";

    protected AbstractGridWidget(LightGrid grid) {
        super(grid);
    }

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
