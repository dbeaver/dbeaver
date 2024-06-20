package org.jkiss.dbeaver.ext.gaussdb.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

public class EntityEditPage extends BaseObjectEditPage {

    private DBPDataSource dataSource;
    private String        name;

    public EntityEditPage(DBPDataSource dataSource, DBSEntityType entityType) {
        super("Create new " + entityType.getName());
        this.dataSource = dataSource;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text nameText = UIUtils.createLabelText(propsGroup, "Name", null); // $NON-NLS-2$
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText().trim();
                updatePageState();
            }
        });

        return propsGroup;
    }

    @Override
    public boolean isPageComplete() {
        return CommonUtils.isNotEmpty(name);
    }

    public String getEntityName() {
        return DBObjectNameCaseTransformer.transformName(dataSource, name);
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DBSObject getObject() {
        // TODO Auto-generated method stub
        return null;
    }
}