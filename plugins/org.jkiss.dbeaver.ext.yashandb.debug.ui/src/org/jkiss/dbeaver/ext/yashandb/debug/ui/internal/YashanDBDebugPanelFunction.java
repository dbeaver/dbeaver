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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanelContainer;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.debug.core.YashanDBDebugCore;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataType;
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
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class YashanDBDebugPanelFunction implements DBGConfigurationPanel {
    private DBGConfigurationPanelContainer container;
    private CSmartCombo<YashanDBProcedureStandalone> functionCombo;
    private YashanDBProcedureStandalone selectedFunction;
    private Table parametersTable;
    private final Map<String, YashanDBProcedureArgument> paramsCache = new ConcurrentHashMap<>();
    private final Log log = Log.getLog(YashanDBDebugPanelFunction.class);

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
        Group composite = UIUtils.createControlGroup(parent, "Function parameters", 1, GridData.FILL_BOTH, SWT.DEFAULT);

        parametersTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        parametersTable.setSize(1000, 800);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
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

                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                DBSProcedureParameterKind parameterKind = param.getParameterKind();
                if(!parameterKind.isInput() && index != 2){
                    return null;
                }

                String name = param.getName();
                Text editor = new Text(table, SWT.BORDER);
                YashanDBProcedureArgument argument = paramsCache.get(name);
                switch (index) {
                    // edit value
                    case 1:
                        editor.setText(CommonUtils.toString(argument.getValue(), ""));
                        break;
                    // edit type
                    case 2:
                        editor.setText(CommonUtils.toString(argument.getParamType(), ""));
                        break;

                }
                editor.addModifyListener(e -> {
                    // Save value immediately. This solves MacOS problems with focus events.
                    saveEditorValue(editor, index, item);
                });
                return editor;
            }

            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                YashanDBProcedureArgument param = (YashanDBProcedureArgument) item.getData();

                String newValue = ((Text) control).getText();
                item.setText(index, newValue);
                if (1 == index){
                    param.setValue(newValue);
                } else if (2 == index) {
                    param.setParamType(newValue);
                }
                paramsCache.put(param.getName(), param);
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

        Map<String, String> valueMap = (Map<String, String>) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS);
        Map<String, String> typeMap = (Map<String, String>) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS_TYPE);

        if(selectedFunction != null){
            saveTableItem(valueMap, typeMap);
        }

        if (selectedFunction != null) {
            functionCombo.addItem(selectedFunction);
            functionCombo.select(selectedFunction);
        }
    }

    private void saveTableItem(Map<String, String> valueMap, Map<String, String> typeMap){

        List<YashanDBProcedureArgument> params = selectedFunction.getParams();

        assert params != null;
        if(params.isEmpty()){
            return;
        }

        if(paramsCache.isEmpty()){
            // save origin data to cache.
            for (YashanDBProcedureArgument param : params) {
                if(param.getParameterKind().isInput() || param.getParameterKind().isOutput()){
                    String paramType = param.getFullTypeName();
                    String value = "";
                    if(valueMap != null){
                        value = valueMap.get(param.getName());
                    }
                    if(typeMap != null){
                        paramType = typeMap.get(param.getName());
                    }
                    param.setParamType(paramType);
                    param.setValue(value);
                    YashanDBProcedureArgument absent = paramsCache.putIfAbsent(param.getName(), param);
                    if(absent != null){
                        log.warn("Init param error, old param is present! The param is : " + absent.getName());
                    }
                }
            }
        }

        List<YashanDBProcedureArgument> arguments = paramsCache.values().stream().sorted(Comparator.comparingInt(YashanDBProcedureArgument::getPosition)).collect(Collectors.toList());
        for (YashanDBProcedureArgument argument : arguments) {
            if(argument.getParameterKind().isInput() || argument.getParameterKind().isOutput()){
                TableItem tableItem = new TableItem(parametersTable, SWT.NONE);
                tableItem.setData(argument);
                tableItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
                tableItem.setText(0, argument.getName());
                tableItem.setText(1, argument.getValue());
                tableItem.setText(2, argument.getParamType());
                tableItem.setText(3, argument.getParameterKind().getTitle());
            }
        }
    }

    @Override
    public void saveConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {

        if (selectedFunction != null) {
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_OID, selectedFunction.getObjectId());
            configuration.put(YashanDBDebugConstants.ATTR_SCHEMA_NAME, selectedFunction.getSchema().getName());

            HashMap<String, String> valueMap = new HashMap<>();
            HashMap<String, String> typeMap = new HashMap<>();
            for (YashanDBProcedureArgument argument : paramsCache.values()) {
                valueMap.put(argument.getName(), argument.getValue());
                typeMap.put(argument.getName(), argument.getParamType());
            }
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS, valueMap);
            configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS_TYPE, typeMap);
        } else {
            configuration.remove(YashanDBDebugConstants.ATTR_FUNCTION_OID);
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
