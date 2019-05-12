
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CursorPlanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CursorPlanType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Operation" maxOccurs="2" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType"/>
 *                   &lt;element name="UDF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="OperationType" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                       &lt;enumeration value="FetchQuery"/>
 *                       &lt;enumeration value="PopulateQuery"/>
 *                       &lt;enumeration value="RefreshQuery"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="CursorName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="CursorActualType" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CursorType" />
 *       &lt;attribute name="CursorRequestedType" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}CursorType" />
 *       &lt;attribute name="CursorConcurrency">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Read Only"/>
 *             &lt;enumeration value="Pessimistic"/>
 *             &lt;enumeration value="Optimistic"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ForwardOnly" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CursorPlanType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "operation"
})
public class CursorPlanType_sql2008 {

    @XmlElement(name = "Operation", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<CursorPlanType_sql2008 .Operation_sql2008> operation;
    @XmlAttribute(name = "CursorName")
    protected String cursorName;
    @XmlAttribute(name = "CursorActualType")
    protected CursorType_sql2008 cursorActualType;
    @XmlAttribute(name = "CursorRequestedType")
    protected CursorType_sql2008 cursorRequestedType;
    @XmlAttribute(name = "CursorConcurrency")
    protected String cursorConcurrency;
    @XmlAttribute(name = "ForwardOnly")
    protected Boolean forwardOnly;

    /**
     * Gets the value of the operation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the operation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOperation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CursorPlanType_sql2008 .Operation_sql2008 }
     * 
     * 
     */
    public List<CursorPlanType_sql2008 .Operation_sql2008> getOperation() {
        if (operation == null) {
            operation = new ArrayList<CursorPlanType_sql2008 .Operation_sql2008>();
        }
        return this.operation;
    }

    /**
     * Gets the value of the cursorName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCursorName() {
        return cursorName;
    }

    /**
     * Sets the value of the cursorName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCursorName(String value) {
        this.cursorName = value;
    }

    /**
     * Gets the value of the cursorActualType property.
     * 
     * @return
     *     possible object is
     *     {@link CursorType_sql2008 }
     *     
     */
    public CursorType_sql2008 getCursorActualType() {
        return cursorActualType;
    }

    /**
     * Sets the value of the cursorActualType property.
     * 
     * @param value
     *     allowed object is
     *     {@link CursorType_sql2008 }
     *     
     */
    public void setCursorActualType(CursorType_sql2008 value) {
        this.cursorActualType = value;
    }

    /**
     * Gets the value of the cursorRequestedType property.
     * 
     * @return
     *     possible object is
     *     {@link CursorType_sql2008 }
     *     
     */
    public CursorType_sql2008 getCursorRequestedType() {
        return cursorRequestedType;
    }

    /**
     * Sets the value of the cursorRequestedType property.
     * 
     * @param value
     *     allowed object is
     *     {@link CursorType_sql2008 }
     *     
     */
    public void setCursorRequestedType(CursorType_sql2008 value) {
        this.cursorRequestedType = value;
    }

    /**
     * Gets the value of the cursorConcurrency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCursorConcurrency() {
        return cursorConcurrency;
    }

    /**
     * Sets the value of the cursorConcurrency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCursorConcurrency(String value) {
        this.cursorConcurrency = value;
    }

    /**
     * Gets the value of the forwardOnly property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getForwardOnly() {
        return forwardOnly;
    }

    /**
     * Sets the value of the forwardOnly property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setForwardOnly(Boolean value) {
        this.forwardOnly = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType"/>
     *         &lt;element name="UDF" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}FunctionType" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *       &lt;attribute name="OperationType" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="FetchQuery"/>
     *             &lt;enumeration value="PopulateQuery"/>
     *             &lt;enumeration value="RefreshQuery"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "queryPlan",
        "udf"
    })
    public static class Operation_sql2008 {

        @XmlElement(name = "QueryPlan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected QueryPlanType_sql2008 queryPlan;
        @XmlElement(name = "UDF", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected List<FunctionType_sql2008> udf;
        @XmlAttribute(name = "OperationType", required = true)
        protected String operationType;

        /**
         * Gets the value of the queryPlan property.
         * 
         * @return
         *     possible object is
         *     {@link QueryPlanType_sql2008 }
         *     
         */
        public QueryPlanType_sql2008 getQueryPlan() {
            return queryPlan;
        }

        /**
         * Sets the value of the queryPlan property.
         * 
         * @param value
         *     allowed object is
         *     {@link QueryPlanType_sql2008 }
         *     
         */
        public void setQueryPlan(QueryPlanType_sql2008 value) {
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
         * {@link FunctionType_sql2008 }
         * 
         * 
         */
        public List<FunctionType_sql2008> getUDF() {
            if (udf == null) {
                udf = new ArrayList<FunctionType_sql2008>();
            }
            return this.udf;
        }

        /**
         * Gets the value of the operationType property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getOperationType() {
            return operationType;
        }

        /**
         * Sets the value of the operationType property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setOperationType(String value) {
            this.operationType = value;
        }

    }

}
