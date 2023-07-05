package org.jkiss.dbeaver.ext.altibase.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTypeset;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class CreateTypesetPage extends BaseObjectEditPage {

    private DBSProcedure procedure;
    private String name;

    public CreateTypesetPage(AltibaseTypeset procedure) {
        super("Create New Typeset");
        this.procedure = procedure;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text containerText = UIUtils.createLabelText(propsGroup, 
                "Container", 
                DBUtils.getObjectFullName(this.procedure.getParentObject(), DBPEvaluationContext.UI));
        containerText.setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, 
                "Name", null);
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            updatePageState();
        });
        
        propsGroup.setTabList(ArrayUtils.remove(Control.class, propsGroup.getTabList(), containerText));

        return propsGroup;
    }

    public DBSProcedureType getProcedureType() {
        return getDefaultProcedureType();
    }

    public DBSProcedureType getPredefinedProcedureType() {
        return getDefaultProcedureType();
    }

    public DBSProcedureType getDefaultProcedureType() {
        return DBSProcedureType.UNKNOWN;
    }

    public String getProcedureName() {
        return DBObjectNameCaseTransformer.transformName(procedure.getDataSource(), name);
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmpty(name);
    }
}
