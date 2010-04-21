/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

/**
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
