
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * The simple statement that may or may not contain query plan, UDF plan or Stored Procedure plan 
 * 
 * <p>Java class for StmtSimpleType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StmtSimpleType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}BaseStmtInfoType">
 *       &lt;sequence>
 *         &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType" minOccurs="0"/>
 *         &lt;element name="UDF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="StoredProc" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StmtSimpleType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "queryPlan",
    "udf",
    "storedProc"
})
public class StmtSimpleType_sql2014
    extends BaseStmtInfoType_sql2014
{

    @XmlElement(name = "QueryPlan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected QueryPlanType_sql2014 queryPlan;
    @XmlElement(name = "UDF", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<FunctionType_sql2014> udf;
    @XmlElement(name = "StoredProc", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected FunctionType_sql2014 storedProc;

    /**
     * Gets the value of the queryPlan property.
     * 
     * @return
     *     possible object is
     *     {@link QueryPlanType_sql2014 }
     *     
     */
    public QueryPlanType_sql2014 getQueryPlan() {
        return queryPlan;
    }

    /**
     * Sets the value of the queryPlan property.
     * 
     * @param value
     *     allowed object is
     *     {@link QueryPlanType_sql2014 }
     *     
     */
    public void setQueryPlan(QueryPlanType_sql2014 value) {
        this.queryPlan = value;
    }

    /**
     * Gets the value of the udf property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the udf property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUDF().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FunctionType_sql2014 }
     * 
     * 
     */
    public List<FunctionType_sql2014> getUDF() {
        if (udf == null) {
            udf = new ArrayList<FunctionType_sql2014>();
        }
        return this.udf;
    }

    /**
     * Gets the value of the storedProc property.
     * 
     * @return
     *     possible object is
     *     {@link FunctionType_sql2014 }
     *     
     */
    public FunctionType_sql2014 getStoredProc() {
        return storedProc;
    }

    /**
     * Sets the value of the storedProc property.
     * 
     * @param value
     *     allowed object is
     *     {@link FunctionType_sql2014 }
     *     
     */
    public void setStoredProc(FunctionType_sql2014 value) {
        this.storedProc = value;
    }

}
