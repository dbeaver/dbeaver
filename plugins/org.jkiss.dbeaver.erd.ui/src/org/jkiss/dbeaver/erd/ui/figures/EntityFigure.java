/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.erd.ui.figures;

import org.eclipse.draw2dl.*;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.ui.ERDColors;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.editor.ERDViewStyle;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Figure used to represent a table in the schema
 *
 * @author Serge Rider
 */
public class EntityFigure extends Figure {

    private final EntityPart part;
    private AttributeListFigure keyFigure;
    private AttributeListFigure attributeFigure;
    private EditableLabel nameLabel;

    public EntityFigure(EntityPart part)
    {
        this.part = part;

        ERDEntity entity = part.getEntity();
        boolean useFQN = part.getDiagram().hasAttributeStyle(ERDViewStyle.ENTITY_FQN);
        boolean showComments = part.getDiagram().hasAttributeStyle(ERDViewStyle.COMMENTS);

        Image tableImage = DBeaverIcons.getImage(entity.getObject().getEntityType().getIcon());

        keyFigure = new AttributeListFigure(entity, true);
        attributeFigure = new AttributeListFigure(entity, false);

        String entityName = useFQN ?
            DBUtils.getObjectFullName(entity.getObject(), DBPEvaluationContext.DDL) :
            entity.getObject().getName();
        if (!CommonUtils.isEmpty(entity.getAlias())) {
            entityName += " " + entity.getAlias();
        }
        nameLabel = new EditableLabel(entityName) {
            @Override
            public IFigure getToolTip() {
                return null;//createToolTip();
            }
        };
        nameLabel.setIcon(tableImage);
        nameLabel.setBorder(new MarginBorder(3));

        Label descLabel = null;
        if (showComments && !CommonUtils.isEmpty(entity.getObject().getDescription())) {
            descLabel = new Label(entity.getObject().getDescription());
        }

/*
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
*/

        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        //layout.setHorizontal(false);
        //layout.setStretchMinorAxis(true);
        setLayoutManager(layout);

        LineBorder border = new LineBorder(getBorderColor(), ERDUIConstants.DEFAULT_ENTITY_BORDER_WIDTH);

        setBorder(border);
        setOpaque(true);

        add(nameLabel, new GridData(GridData.FILL_HORIZONTAL));
        if (descLabel != null) {
            add(descLabel, new GridData(GridData.FILL_HORIZONTAL));
        }
        add(keyFigure, new GridData(GridData.FILL_HORIZONTAL));
        add(attributeFigure, new GridData(GridData.FILL_BOTH));

        refreshColors();
    }

    @NotNull
    private IFigure createToolTip() {
        ERDEntity entity = part.getEntity();
        DBPDataSourceContainer dataSource = entity.getDataSource().getContainer();

        Figure toolTip = new Figure();
        toolTip.setOpaque(true);
        //toolTip.setPreferredSize(300, 200);
        toolTip.setBorder(getBorder());
        toolTip.setLayoutManager(new GridLayout(1, false));

        {
            Label dsLabel = new Label(dataSource.getName());
            dsLabel.setIcon(DBeaverIcons.getImage(dataSource.getDriver().getIcon()));
            dsLabel.setBorder(new MarginBorder(2));
            toolTip.add(dsLabel);
        }
        {
            Label entityLabel = new Label(DBUtils.getObjectFullName(entity.getObject(), DBPEvaluationContext.UI));
            entityLabel.setIcon(DBeaverIcons.getImage(entity.getObject().getEntityType().getIcon()));
            entityLabel.setBorder(new MarginBorder(2));
            toolTip.add(entityLabel);
        }

        return toolTip;
    }

    protected Color getBorderColor() {
        int dsIndex = getPart().getDiagram().getDataSourceIndex(part.getEntity().getDataSource().getContainer());
        boolean changeBorderColors = ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_CHANGE_BORDER_COLORS);
        if (dsIndex == 0 || !changeBorderColors) {
            return UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_LINES_FOREGROUND);
        }
        return ERDColors.getBorderColor(dsIndex - 1);
    }

    public EntityPart getPart() {
        return part;
    }

    public void refreshColors() {
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();

        setForegroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
        if (part.getEntity().isPrimary()) {
            setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_PRIMARY_BACKGROUND));
        } else if (part.getEntity().getObject().getEntityType() == DBSEntityType.ASSOCIATION) {
            setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND));
        } else {
            boolean changeHeaderColors = ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_CHANGE_HEADER_COLORS);
            if (changeHeaderColors) {
                changeHeaderColor(colorRegistry);
            } else {
                setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
            }
        }
    }

    private void changeHeaderColor(ColorRegistry colorRegistry) {
        DBSObjectContainer container = DBUtils.getParentOfType(DBSObjectContainer.class, part.getEntity().getObject());
        if (container != null) {
            DBPDataSourceContainer dataSourceContainer = container.getDataSource().getContainer();
            if (dataSourceContainer != null) {
                int containerIndex = part.getDiagram().getContainerIndex(dataSourceContainer, container);
                if (containerIndex == 0) {
                    setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
                } else {
                    setBackgroundColor(ERDColors.getHeaderColor(containerIndex - 1));
                }
            } else {
                setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
            }
        } else {
            setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
        }
    }


    public void updateTitleForegroundColor() {
        Color bgColor = getBackgroundColor();
        
        if(bgColor == null)
        	nameLabel.setForegroundColor(UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
        else
	        nameLabel.setForegroundColor(UIUtils.getContrastColor(bgColor));
    }

    @Override
    public void setBackgroundColor(Color bg) {
        super.setBackgroundColor(bg);
        updateTitleForegroundColor();
    }

    public void setSelected(boolean isSelected)
    {
        LineBorder lineBorder = (LineBorder) getBorder();
        if (isSelected) {
            lineBorder.setWidth(3);
        } else {
            lineBorder.setWidth(2);
        }
    }


    /**
     * @return returns the label used to edit the name
     */
    public EditableLabel getNameLabel()
    {
        return nameLabel;
    }

    public AttributeListFigure getKeyFigure() {
        return keyFigure;
    }

    /**
     * @return the figure containing the column labels
     */
    public AttributeListFigure getColumnsFigure()
    {
        return attributeFigure;
    }

    // Workaround: attribute figures aren't direct children of entity figure
    @Override
    public void add(IFigure figure, Object constraint, int index) {
        if (figure instanceof AttributeItemFigure) {
            ColorRegistry colorRegistry = UIUtils.getColorRegistry();
            figure.setForegroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ATTR_FOREGROUND));
            figure.setBackgroundColor(colorRegistry.get(ERDUIConstants.COLOR_ERD_ATTR_BACKGROUND));

            IFigure attrExtra = createRightPanel();

            AttributeItemFigure attributeItemFigure = (AttributeItemFigure) figure;
            attributeItemFigure.setRightPanel(attrExtra);
            if (attributeItemFigure.getAttribute().isInPrimaryKey()) {
                keyFigure.add(figure, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false));
                keyFigure.add(attrExtra, new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING));
            } else {
                attributeFigure.add(figure, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false));
                attributeFigure.add(attrExtra, new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING));
            }

        } else {
            super.add(figure, constraint, index);
        }
    }

    protected IFigure createRightPanel() {
        EditableLabel label = new EditableLabel("");
        //attrExtra.setBorder(new LineBorder(1));
        label.setTextAlignment(PositionConstants.RIGHT);
        return label;
    }

    public List<AttributeItemFigure> getAttributeFigures() {
        List<AttributeItemFigure> result = new ArrayList<>();
        result.addAll(keyFigure.getAttributes());
        result.addAll(attributeFigure.getAttributes());
        return result;
    }

    // Workaround: attribute figures aren't direct children of entity figure
    @Override
    public void remove(IFigure figure) {
        if (figure instanceof AttributeItemFigure) {
            AttributeItemFigure attrFigure = (AttributeItemFigure) figure;
            AttributeListFigure listFigure;
            if (keyFigure.getAttributes().contains(figure)) {
                listFigure = keyFigure;
            } else {
                listFigure = attributeFigure;
            }
            listFigure.remove(attrFigure);
            if (attrFigure.getRightPanel() != null) {
                listFigure.remove(attrFigure.getRightPanel());
            }
            this.revalidate();
        } else {
            super.remove(figure);
        }
    }
}