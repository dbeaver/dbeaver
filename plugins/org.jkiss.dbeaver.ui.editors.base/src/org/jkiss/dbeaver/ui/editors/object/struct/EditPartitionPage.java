package org.jkiss.dbeaver.ui.editors.object.struct;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class EditPartitionPage extends AttributesSelectorPage {

    private static final Log log = Log.getLog(EditPartitionPage.class);
    //, 号分割
    private String partitionNames;
    private String value;
    private String[] partitionColumns;
    private final DBRProgressMonitor monitor;
    private final  BiFunction<TableItem, DBNDatabaseNode, Boolean> check;
    //TMP
    private String selectedPartitionType;

    private String partitionType;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EditPartitionPage(
            String title,
            DBSTablePartition partition, DBRProgressMonitor monitor, BiFunction<TableItem, DBNDatabaseNode, Boolean> check, String partitionType)
    {
        super(title, partition.getTable());
        this.partitionType = partitionType;
        this.monitor = monitor;
        this.check = check;
    }


    @Override
    protected void createContentsBeforeColumns(Composite panel) {
        final Text nameText = entity != null ? UIUtils.createLabelText(panel, EditorsMessages.dialog_struct_edit_constrain_label_name, partitionNames) : null;
        if (nameText != null) {
            nameText.selectAll();
            nameText.setFocus();
            nameText.addModifyListener(e -> partitionNames = nameText.getText().trim());
        }

        IStructuredSelection currentSelection = getCurrentSelection();
        if(!isPresentChildren(currentSelection)){
            Label controlLabel = UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_constrain_label_type);
            final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            typeCombo.add("HASH");
            typeCombo.add("RANGE");
            typeCombo.add("LIST");
            typeCombo.select(0);
//        typeCombo.add("REFERENCE");

            typeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // 处理下拉框选择事件
                    int selectedIndex = typeCombo.getSelectionIndex();
                    selectedPartitionType = typeCombo.getItem(selectedIndex);
                }
            });

            selectedPartitionType = typeCombo.getItem(typeCombo.getSelectionIndex());
        }

        if(selectedPartitionType == null){
            selectedPartitionType = partitionType;
        }

        Text valueText = UIUtils.createLabelText(panel, "Partition Value", this.value);
        valueText.addModifyListener(v ->
                this.value = valueText.getText());

    }

    @Override
    protected void fillAttributes(final DBSEntity entity)
    {
        if (entity == null) {
            return;
        }
        final List<DBSEntityAttribute> attrList = new ArrayList<>();
        AbstractJob loadJob = new AbstractJob("Load entity attributes") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("Load attributes", 1);
                try {
                    for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                        if (isShowHiddenAttributes() || !DBUtils.isHiddenObject(attr) || DBUtils.isRowIdAttribute(attr)) {
                            attrList.add(attr);
                            // Preload node - required later to display its icon
                            DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, attr, true);
                        }
                    }
                } catch (DBException e) {
                    return GeneralUtils.makeErrorStatus("Error loading attributes", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                UIUtils.syncExec(() -> {
                    if (isPresentChildren(getCurrentSelection())){
                        attrList.clear();
                    }
                    for (DBSEntityAttribute attribute : attrList) {
                        TableItem columnItem = new TableItem(columnsTable, SWT.NONE);

                        AttributeInfo col = new AttributeInfo(attribute);
                        attributes.add(col);

                        DBNDatabaseNode attributeNode = DBWorkbench.getPlatform().getNavigatorModel().findNode(attribute);
                        if (attributeNode != null) {
                            columnItem.setImage(0, DBeaverIcons.getImage(attributeNode.getNodeIcon()));
                        }
                        fillAttributeColumns(attribute, col, columnItem);
                        columnItem.setData(col);
                    }
                    UIUtils.packColumns(columnsTable);
                    updateColumnSelection();
                    onAttributesLoad();
                    preselectAttributes();
                });
            }
        });
        loadJob.schedule();
    }

    @Override
    protected void preselectAttributes() {
        IStructuredSelection selection = getCurrentSelection();
        if (selection == null) {
            return;
        }

        if (isPresentChildren(selection)){
            this.attributes.clear();
            this.columnsTable.clearAll();
            this.updatePageState();
        }
    }

    private boolean isPresentChildren(IStructuredSelection selection){
        return getCurrentSelectionChildren(selection).length > 0;
    }

    private DBNDatabaseNode[] getCurrentSelectionChildren(IStructuredSelection selection){
        Object o = selection.toList().stream().filter(n -> n instanceof DBNDatabaseNode).findFirst().orElse(null);
        if(o == null){
            return new DBNDatabaseNode[0];
        }
        DBNDatabaseNode dbnDatabaseNode = (DBNDatabaseNode) o;
        DBNDatabaseNode[] children = null;
        try {
            if(dbnDatabaseNode.getParentNode() instanceof DBNDatabaseFolder){
                children = new DBNDatabaseNode[]{dbnDatabaseNode};
            }else {
                children = dbnDatabaseNode.getChildren(monitor);
            }
        } catch (DBException e) {
            log.error("Can't find partition folder's children");
        }
        return children;
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }
}
