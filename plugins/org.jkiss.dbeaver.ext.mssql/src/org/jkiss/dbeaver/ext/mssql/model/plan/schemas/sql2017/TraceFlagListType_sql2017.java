
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Collection of trace flags used in SQL engine.
 * 			
 * 
 * <p>Java class for TraceFlagListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TraceFlagListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TraceFlag" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}TraceFlagType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="IsCompileTime" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TraceFlagListType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "traceFlag"
})
public class TraceFlagListType_sql2017 {

    @XmlElement(name = "TraceFlag", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<TraceFlagType_sql2017> traceFlag;
    @XmlAttribute(name = "IsCompileTime", required = true)
    protected boolean isCompileTime;

    /**
     * Gets the value of the traceFlag property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the traceFlag property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTraceFlag().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TraceFlagType_sql2017 }
     * 
     * 
     */
    public List<TraceFlagType_sql2017> getTraceFlag() {
        if (traceFlag == null) {
            traceFlag = new ArrayList<TraceFlagType_sql2017>();
        }
        return this.traceFlag;
    }

    /**
     * Gets the value of the isCompileTime property.
     * 
     */
    public boolean isIsCompileTime() {
        return isCompileTime;
    }

    /**
     * Sets the value of the isCompileTime property.
     * 
     */
    public void setIsCompileTime(boolean value) {
        this.isCompileTime = value;
    }

}
