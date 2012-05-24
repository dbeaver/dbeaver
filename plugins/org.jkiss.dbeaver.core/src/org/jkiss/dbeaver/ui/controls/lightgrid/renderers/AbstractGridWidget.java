/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
    @Override
    public String getHoverDetail()
    {
        return hoverDetail;
    }

    /**
     * @param hoverDetail the hoverDetail to set
     */
    @Override
    public void setHoverDetail(String hoverDetail)
    {
        this.hoverDetail = hoverDetail;
    }

}
