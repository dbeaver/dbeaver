package org.jkiss.dbeaver.ext.yashandb.debug.ui.internal;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanelContainer;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.debug.core.YashanDBDebugCore;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureArgument;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YashanDBDebugPanelFunction implements DBGConfigurationPanel {
    private DBGConfigurationPanelContainer container;
    private CSmartCombo<YashanDBProcedureStandalone> functionCombo;
    private YashanDBProcedureStandalone selectedFunction;
    private Map<DBSProcedureParameter, Object> parameterValues = new HashMap<>();
    private Map<DBSProcedureParameter, Object> parameterTypes = new HashMap<>();
    private Table parametersTable;

    @Override
    public void createPanel(Composite parent, DBGConfigurationPanelContainer container) {
        this.container = container;
        createFunctionGroup(parent);
        createParametersGroup(parent);
    }

    private void createFunctionGroup(Composite parent) {
        Group functionGroup = UIUtils.createControlGroup(parent, "Function", 2, GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
        UIUtils.createControlLabel(functionGroup, "Function");
        functionCombo = new CSmartSelector<YashanDBProcedureStandalone>(functionGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return DBeaverIcons.getImage(DBIcon.TREE_PROCEDURE);
            }

            @Override
            public String getText(Object element) {
                if (element == null) {
                    return "N/A" ;
                }
                return ((YashanDBProcedureStandalone) element).getFullQualifiedSignature();
            }
        }) {
            @Override
            protected void dropDown(boolean drop) {
                if (drop) {
                    DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                    DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(container.getDataSource());
                    if (dsNode != null) {
                        DBNNode curNode = selectedFunction == null ? null : navigatorModel.getNodeByObject(selectedFunction);
                        DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                                parent.getShell(),
                                "Select function to debug",
                                dsNode,
                                curNode,
                                new Class[]{DBSInstance.class, DBSObjectContainer.class, YashanDBProcedureStandalone.class},
                                new Class[]{YashanDBProcedureStandalone.class}, null);
                        if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof YashanDBProcedureStandalone) {
                            functionCombo.removeAll();
                            selectedFunction = (YashanDBProcedureStandalone) ((DBNDatabaseNode) node).getObject();
                            functionCombo.addItem(selectedFunction);
                            functionCombo.select(selectedFunction);
                            updateParametersTable();
                            container.updateDialogState();
                        }
                        parametersTable.setEnabled(selectedFunction != null);
                    }
                }

            }
        };
        functionCombo.addItem(null);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(functionCombo) * 40 + 10;
        functionCombo.setLayoutData(gd);
        functionCombo.setEnabled(false);
    }

    public static void main(String[] args) {
        //创建一个display对象
        Display display = new Display();
        //shell是程序的主窗体
        Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());
        //设置主窗体的标题
        shell.setText("ScrolledCompositeTest组件");


        Composite comp = new Composite(shell, SWT.NONE);

        comp.setLayout(new GridLayout(1, true));


        Group composite = UIUtils.createControlGroup(comp, "////////", 4, GridData.FILL_BOTH, SWT.DEFAULT);


        Table parametersTable = new Table(composite,  SWT.FULL_SELECTION | SWT.BORDER );
        final GridData gd = new GridData(GridData.FILL_BOTH);
        parametersTable.setLayoutData(gd);
        parametersTable.setHeaderVisible(true);
        parametersTable.setLinesVisible(true);

        final TableColumn nameColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Value");
        valueColumn.setWidth(200);
        final TableColumn typeColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Type");
        typeColumn.setWidth(150);
        final TableColumn kindColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Kind");
        kindColumn.setWidth(100);

        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            strings.add(String.valueOf(i));
        }
        for (String string : strings) {
            TableItem item = new TableItem(parametersTable, SWT.NONE);

            item.setText(0, string);
            item.setText(1, string);
            item.setText(2, string);
            item.setText(3, string);
        }


        //设置主窗体的标题
        shell.setText("ScrolledCompositeTest组件");
        ScrolledComposite c1 = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Button b1 = new Button(c1, SWT.PUSH);
        b1.setText("左侧");
        b1.setSize(800, 800);
        c1.setContent(b1);

        ScrolledComposite c2 = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Button b2 = new Button(c2, SWT.PUSH);
        b2.setText("右测");
        c2.setContent(b2);
        c2.setExpandHorizontal(true);
        c2.setExpandVertical(true);
        c2.setMinSize(400, 400);

        //设置窗体大小
        shell.setSize(600, 300);
        //打开主窗体
        shell.open();
        //如果主窗体没有关闭
        while (!shell.isDisposed()) {
            //如果display不忙
            if (!display.readAndDispatch()) {
                //休眠
                display.sleep();
            }
        }
        //销毁display
        display.dispose();
    }

    private void createParametersGroup(Composite parent) {
        Group composite = UIUtils.createControlGroup(parent, "Function parameters", 2, GridData.FILL_BOTH, SWT.DEFAULT);

        parametersTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        parametersTable.setLayoutData(gd);
        parametersTable.setHeaderVisible(true);
        parametersTable.setLinesVisible(true);

        final TableColumn nameColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Value");
        valueColumn.setWidth(200);
        final TableColumn typeColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Type");
        typeColumn.setWidth(150);
        final TableColumn kindColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Kind");
        kindColumn.setWidth(100);

        new CustomTableEditor(parametersTable) {
            {
                firstTraverseIndex = 1;
                lastTraverseIndex = 1;
                editOnEnter = false;
            }

            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                if (index != 1 && index != 2) {
                    return null;
                }
                if(index==1) {
                    DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                    Text editor = new Text(table, SWT.BORDER);
                    editor.setText(CommonUtils.toString(parameterValues.get(param), ""));
                    editor.selectAll();
                    return editor;
                }else {
                    DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                    Text editor = new Text(table, SWT.BORDER);
                    editor.setText(CommonUtils.toString(parameterTypes.get(param), ""));
                    editor.selectAll();
                    return editor;
                }
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                String newValue = ((Text) control).getText();
                item.setText(index, newValue);
                if(index==1) {
                    parameterValues.put(param, newValue);
                }else if(index==2){
                    parameterTypes.put(param,newValue);
                }
                container.updateDialogState();
            }
        };
    }

    @Override
    public void loadConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {
        long functionId = CommonUtils.toLong(configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_OID));
        if (functionId != 0 && dataSource != null) {
            try {
                container.getRunnableContext().run(true, true, monitor -> {
                    try {
                        selectedFunction = YashanDBDebugCore.resolveFunction(monitor, dataSource, configuration, null, null);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                container.setWarningMessage(null);
            } catch (InvocationTargetException e) {
                container.setWarningMessage(e.getTargetException().getMessage());
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (selectedFunction != null) {
            @SuppressWarnings("unchecked")
            List<String> paramValues = (List<String>) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS);
            if (paramValues != null) {
                List<YashanDBProcedureArgument> parameters = selectedFunction.getInputParams();
                if (parameters.size() == paramValues.size()) {
                    for (int i = 0; i < parameters.size(); i++) {
                        YashanDBProcedureArgument param = parameters.get(i);
                        parameterValues.put(param, paramValues.get(i));
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<String> paramTypes = (List<String>) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS_TYPE);
            if (paramTypes != null) {
                List<YashanDBProcedureArgument> parameters = selectedFunction.getInputParams();
                if (parameters.size() == paramTypes.size()) {
                    for (int i = 0; i < parameters.size(); i++) {
                        YashanDBProcedureArgument param = parameters.get(i);
                        parameterTypes.put(param, paramTypes.get(i));
                    }
                }
            }

            updateParametersTable();
        }
        if (selectedFunction != null) {
            functionCombo.addItem(selectedFunction);
            functionCombo.select(selectedFunction);
        }
    }

    private void updateParametersTable() {
        parametersTable.removeAll();
        for (DBSProcedureParameter param : selectedFunction.getInputParams()) {
            TableItem item = new TableItem(parametersTable, SWT.NONE);
            item.setData(param);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
            item.setText(0, param.getName());
            Object value = parameterValues.get(param);
            item.setText(1, CommonUtils.toString(value, ""));
            Object type=parameterTypes.get(param);
            if (type == null) {
                Field[] fields = param.getClass().getDeclaredFields();
                for(Field field: fields) {
                    field.setAccessible(true);
                    if(field.getName().equals("type")){
                        try {
                            type = field.get(param);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            item.setText(2, CommonUtils.toString(type, ""));
            item.setText(3, param.getParameterKind().getTitle());
        }

        parametersTable.select(0);
    }

    @Override
    public void saveConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {
//        configuration.put(PostgreDebugConstants.ATTR_ATTACH_KIND,
//            kindGlobal.getSelection() ? PostgreDebugConstants.ATTACH_KIND_GLOBAL : PostgreDebugConstants.ATTACH_KIND_LOCAL);
//        configuration.put(PostgreDebugConstants.ATTR_ATTACH_PROCESS, processIdText.getText());

        if (selectedFunction != null) {
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_OID, selectedFunction.getObjectId());
//            configuration.put(YashanDBDebugConstants.ATTR_DATABASE_NAME, selectedFunction.getDatabase().getName());
            configuration.put(YashanDBDebugConstants.ATTR_SCHEMA_NAME, selectedFunction.getSchema().getName());
            List<String> paramValues = new ArrayList<>();
            List<String> paramTypes=new ArrayList<>();
            for (YashanDBProcedureArgument param : selectedFunction.getInputParams()) {
                Object value = parameterValues.get(param);
                paramValues.add(value == null ? null : value.toString());
                Object type=parameterTypes.get(param);
                paramTypes.add(type==null?null:type.toString());
            }
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS, paramValues);
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS_TYPE,paramTypes);

            //YANGMENG,可以会返回参数名字
            //LinkedHashMap<String, String> params =
            //        parameterValues.entrySet().stream().collect(Collectors.toMap(t -> t.getKey().getName(), a -> a.getValue().toString(), (a, b) -> a,
            //                LinkedHashMap::new));
            //configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS, params);
        } else {
            configuration.remove(YashanDBDebugConstants.ATTR_FUNCTION_OID);
//            configuration.remove(YashanDBDebugConstants.ATTR_DATABASE_NAME);
            configuration.remove(YashanDBDebugConstants.ATTR_SCHEMA_NAME);
            configuration.remove(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS);
            configuration.remove(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS_TYPE);
        }
    }

    @Override
    public boolean isValid() {
        return selectedFunction != null;
    }
}
