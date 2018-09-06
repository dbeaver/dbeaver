/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.editor.ERDViewStyle;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
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
        nameLabel = new EditableLabel(
            entityName);
        if (tableImage != null) {
            nameLabel.setIcon(tableImage);
        }
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

        ToolbarLayout layout = new ToolbarLayout();
        layout.setHorizontal(false);
        layout.setStretchMinorAxis(true);
        setLayoutManager(layout);

        LineBorder border = new LineBorder(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_LINES_FOREGROUND), 2);

        setBorder(border);
        setOpaque(true);

        add(nameLabel);
        if (descLabel != null) {
            add(descLabel);
        }
        add(keyFigure);
        add(attributeFigure);

        // Tooltip doesn't make sense and just flicks around
/*
        Label toolTip = new Label(DBUtils.getObjectFullName(entity.getObject(), DBPEvaluationContext.UI));
        toolTip.setIcon(tableImage);
        setToolTip(toolTip);
*/
        refreshColors();
    }

    public void refreshColors() {
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();

        if (part.getEntity().isPrimary()) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_PRIMARY_BACKGROUND));
        } else if (part.getEntity().getObject().getEntityType() == DBSEntityType.ASSOCIATION) {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND));
        } else {
            setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND));
        }
        setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
        nameLabel.setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND));
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

    @Override
    public void add(IFigure figure, Object constraint, int index) {
        if (figure instanceof AttributeItemFigure) {
            ColorRegistry colorRegistry = UIUtils.getColorRegistry();
            figure.setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND));
            figure.setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_BACKGROUND));

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
}