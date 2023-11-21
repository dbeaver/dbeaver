package org.jkiss.dbeaver.ext.cubrid.model.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class CubridMetaModelDescriptor extends AbstractDescriptor {
	
	private IConfigurationElement contributorConfig;
	private ObjectType implType;
	private CubridMetaModel instance;
	
	private String id;
	private final Map<String, GenericMetaObject> objects = new HashMap<>();
	private String[] driverClass;
	private final String dialectId;
	private List<String> modelReplacements;

	public CubridMetaModelDescriptor() {
		super("org.jkiss.dbeaver.ext.cubrid");
		implType = new ObjectType(CubridMetaModel.class.getName());
		instance = new CubridMetaModel();
		instance.descriptor = this;
		dialectId = CubridSQLDialect.CUBRID_DIALECT_ID;
	}

	public CubridMetaModelDescriptor(IConfigurationElement cfg) {
		super(cfg);
		this.contributorConfig = cfg;
		
		this.id = cfg.getAttribute("id");
		IConfigurationElement[] objectList = cfg.getChildren("object");
		if (!ArrayUtils.isEmpty(objectList)) {
			for (IConfigurationElement childConfig : objectList) {
				GenericMetaObject metaObject = new GenericMetaObject(childConfig);
				objects.put(metaObject.getType(), metaObject);
			}
		}
		String driverClassList = cfg.getAttribute("driverClass");
		if (CommonUtils.isEmpty(driverClassList)) {
			this.driverClass = new String[0];
		} else {
			this.driverClass = driverClassList.split(",");
		}

		implType = new ObjectType(cfg.getAttribute("class"));
		dialectId = CommonUtils.toString(cfg.getAttribute("dialect"), CubridSQLDialect.CUBRID_DIALECT_ID);

		IConfigurationElement[] replaceElements = cfg.getChildren("replace");
		for (IConfigurationElement replace : replaceElements) {
			String modelId = replace.getAttribute("model");
			if (modelReplacements == null) {
				modelReplacements = new ArrayList<>();
			}
			modelReplacements.add(modelId);
		}
	}

	public String getId() {
		return id;
	}

	@NotNull
	public String[] getDriverClass() {
		return driverClass;
	}
	
	public GenericMetaObject getObject(String id) {
		return objects.get(id);
	}
	
	public SQLDialectMetadata getDialect() {
		return SQLDialectRegistry.getInstance().getDialect(dialectId);
	}
	
	public List<String> getModelReplacements() {
		return CommonUtils.safeList(modelReplacements);
	}
	
	public void setModelReplacements(List<String> modelReplacements) {
		this.modelReplacements = modelReplacements;
	}

	public CubridMetaModel getInstance() throws DBException {
		if (instance != null) {
			return instance;
		}
		Class<? extends CubridMetaModel> implClass = implType.getObjectClass(CubridMetaModel.class);
		if (implClass == null) {
			throw new DBException("Can't create cubrid meta model instance '" + implType.getImplName() + "'");
		}
		try {
			instance = implClass.getDeclaredConstructor().newInstance();
		} catch (Throwable e) {
			throw new DBException("Can't instantiate meta model", e);
		}
		instance.descriptor = this;
		return instance;
	}
}
