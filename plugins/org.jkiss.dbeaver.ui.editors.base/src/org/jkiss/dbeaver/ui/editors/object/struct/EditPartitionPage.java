package org.jkiss.dbeaver.ui.editors.object.struct;


import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EditPartitionPage extends AttributesSelectorPage {

    //, 号分割
    private String partitionNames;
    private String[] partitionColumns;

    //TMP
    private String selectedPartitionType;


    public String getPartitionNames() {
        return partitionNames;
    }

    public void setPartitionNames(String partitionNames) {
        this.partitionNames = partitionNames;
    }

    public String getSelectedPartitionType() {
        return selectedPartitionType;
    }

    public void setSelectedPartitionType(String selectedPartitionType) {
        this.selectedPartitionType = selectedPartitionType;
    }

    public EditPartitionPage(
            String title,
            DBSTablePartition partition)
    {
        super(title, partition.getTable());
    }


    @Override
    protected void createContentsBeforeColumns(Composite panel) {
        final Text nameText = entity != null ? UIUtils.createLabelText(panel, EditorsMessages.dialog_struct_edit_constrain_label_name, partitionNames) : null;
        if (nameText != null) {
            nameText.selectAll();
            nameText.setFocus();
            nameText.addModifyListener(e -> partitionNames = nameText.getText().trim());
        }
        UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_constrain_label_type);
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        typeCombo.add("HASH");  //DEFAULT
        typeCombo.add("RANGE");  //NOT SUPPORT

        typeCombo.select(0);

        selectedPartitionType = typeCombo.getItem(typeCombo.getSelectionIndex());

    }
}
