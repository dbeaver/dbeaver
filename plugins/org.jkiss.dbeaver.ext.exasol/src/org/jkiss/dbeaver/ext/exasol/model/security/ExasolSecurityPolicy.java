package org.jkiss.dbeaver.ext.exasol.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

public class ExasolSecurityPolicy implements DBPRefreshableObject, DBPSaveableObject {
	
	private ExasolDataSource dataSource;
	private String name = ExasolMessages.exasol_security_policy_name;
	private String description = ExasolMessages.exasol_security_policy_description; 
	private Boolean persisted;
	private Integer minLength;
	private Integer maxLength;
	private Integer minLowerCase;
	private Integer minUpperCase;
	private Integer minNumericChars;
	private Integer minSpecialChars;
	private Integer reusableAfterChanges;
	private Integer reusableAfterDays;
	private Integer maxFailedLoginAttempts;
	private Boolean enabled;
	
	
	public static HashMap<String,Integer> parseInput(String input)
	{
		if (input.equals("OFF"))
		{
			return new HashMap<String,Integer>();
		}
		
		String[] parms = input.split(":");
		HashMap<String,Integer> ret = new HashMap<String,Integer>();
		
		for (int i = 0; i < parms.length; i++) {
			String parm = parms[i];
			
			String[] data = parm.split("=");
			ret.put(data[0], Integer.parseInt(data[1]));
		}
		return ret;
	}
	
	
	private void assignValues(HashMap<String,Integer> values)
	{
		if (values.isEmpty())
		{
			enabled=false;
		}
		
		for (String key : values.keySet()) {
			switch (key) {
			case "MIN_LENGTH":
				this.minLength = values.get(key);
				break;
			case "MAX_LENGTH":
				this.maxLength = values.get(key);
				break;
			case "MIN_LOWER_CASE":
				this.minLowerCase = values.get(key);
				break;
			case "MIN_UPPER_CASE":
				this.minUpperCase = values.get(key);
				break;
			case "MIN_NUMERIC_CHARS":
				this.minNumericChars = values.get(key);
				break;
			case "MIN_SPECIAL_CHARS":
				this.minSpecialChars = values.get(key);
				break;
			case "REUSABLE_AFTER_CHANGES":
				this.reusableAfterChanges = values.get(key);
				break;
			case "REUSABLE_AFTER_DAYS":
				this.reusableAfterDays = values.get(key);
				break;
			case "MAX_FAILED_LOGIN_ATTEMPTS":
				this.maxFailedLoginAttempts = values.get(key);
				break;
			default:
				break;
			}
		}
	}
	
	public ExasolSecurityPolicy(ExasolDataSource dataSource, ResultSet dbResult)
	{
		this.persisted = true;
		this.dataSource = dataSource;
		
		String value = JDBCUtils.safeGetString(dbResult, "SYSTEM_VALUE");
		
		if (value.isEmpty() | value.equals("OFF"))
		{
			this.enabled = false;
		} else {
			assignValues(ExasolSecurityPolicy.parseInput(value));
		}
	}
	

	@Override
    @Property(viewable = true,  order = 20, multiline= true)
	public String getDescription() {
		return description;
	}

	@Override
	public DBSObject getParentObject() {
		return dataSource.getContainer();
	}

	@Override
	public ExasolDataSource getDataSource() {
		return dataSource;
	}

	@Override
    @Property(viewable = true, order = 10)
	public String getName() {
		return name;
	}

	@Override
	public boolean isPersisted() {
		return persisted;
	}

	@Override
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 30)
	public Integer getMinLength() {
		return minLength;
	}

	public void setMinLength(Integer minLength) {
		this.minLength = minLength;
		this.enabled = true;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 40)
	public Integer getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(Integer maxLength) {
		this.enabled = true;
		this.maxLength = maxLength;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 50)
	public Integer getMinLowerCase() {
		return minLowerCase;
	}

	public void setMinLowerCase(Integer minLowerCase) {
		this.minLowerCase = minLowerCase;
		this.enabled = true;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 60)
	public Integer getMinUpperCase() {
		return minUpperCase;
	}

	public void setMinUpperCase(Integer minUpperCase) {
		this.minUpperCase = minUpperCase;
		this.enabled = true;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 70)
	public Integer getMinSpecialChars() {
		return minSpecialChars;
	}

	public void setMinSpecialChars(Integer specialChars) {
		this.enabled = true;
		this.minSpecialChars = specialChars;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 80)
	public Integer getReusableAfterChanges() {
		return reusableAfterChanges;
	}

	public void setReusableAfterChanges(Integer reusableAfterChanges) {
		this.enabled = true;
		this.reusableAfterChanges = reusableAfterChanges;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 90)
	public Integer getReusableAfterDays() {
		return reusableAfterDays;
	}

	public void setReusableAfterDays(Integer reusableAfterDays) {
		this.enabled = true;
		this.reusableAfterDays = reusableAfterDays;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 100)
	public Integer getMaxFailedLoginAttempts() {
		return maxFailedLoginAttempts;
	}

	public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
		this.enabled = true;
		this.maxFailedLoginAttempts = maxFailedLoginAttempts;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 110)
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
		if (! enabled) {
			this.maxFailedLoginAttempts = null;
			this.maxLength = null;
			this.minLength = null;
			this.minLowerCase = null;
			this.minUpperCase = null;
			this.minNumericChars = null;
			this.reusableAfterChanges = null;
			this.reusableAfterDays = null;
			this.minSpecialChars = null;
		}
	}

    @Property(viewable = true, editable = true, updatable = true, order = 75)
	public Integer getMinNumericChars() {
		return minNumericChars;
	}

	public void setMinNumericChars(Integer minNumericChars) {
		this.minNumericChars = minNumericChars;
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		return this;
	}
	
	public String getSecurityString()
	{
		if (! enabled)
		{
			return "OFF";
		}
		ArrayList<String> str = new ArrayList<>();
		
		if (maxFailedLoginAttempts != null)
		{
			str.add("MAX_FAILED_LOGIN_ATTEMPTS="+maxFailedLoginAttempts.toString());
		}
		if (minLength != null)
		{
			str.add("MIN_LENGTH="+minLength.toString());
		}
		if (maxLength != null)
		{
			str.add("MAX_LENGTH="+maxLength.toString());
		}
		if (minLowerCase != null)
		{
			str.add("MIN_LOWER_CASE="+minLowerCase.toString());
		}
		if (maxFailedLoginAttempts != null)
		{
			str.add("MIN_UPPER_CASE="+minUpperCase.toString());
		}
		if (minNumericChars != null)
		{
			str.add("MIN_NUMERIC_CHARS="+minNumericChars.toString());
		}
		if (minSpecialChars != null)
		{
			str.add("MIN_SPECIAL_CHARS="+minSpecialChars.toString());
		}
		if (reusableAfterChanges != null)
		{
			str.add("REUSABLE_AFTER_CHANGES="+reusableAfterChanges.toString());
		}
		if (reusableAfterDays != null)
		{
			str.add("REUSABLE_AFTER_DAYS="+reusableAfterDays.toString());
		}
		return CommonUtils.joinStrings(":", str);
	}

}
