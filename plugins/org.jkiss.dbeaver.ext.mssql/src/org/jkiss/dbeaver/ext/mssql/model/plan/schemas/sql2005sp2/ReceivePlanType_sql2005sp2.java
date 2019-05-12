
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ReceivePlanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReceivePlanType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Operation" maxOccurs="2" minOccurs="2">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="QueryPlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}QueryPlanType"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="OperationType">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                       &lt;enumeration value="ReceivePlanSelect"/>
 *                       &lt;enumeration value="ReceivePlanUpdate"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReceivePlanType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "operation"
})
public class ReceivePlanType_sql2005sp2 {

    @XmlElement(name = "Operation", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ReceivePlanType_sql2005sp2 .Operation_sql2005sp2> operation;

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
     * {@link ReceivePlanType_sql2005sp2 .Operation_sql2005sp2 }
     * 
     * 
     */
    public List<ReceivePlanType_sql2005sp2 .Operation_sql2005sp2> getOperation() {
        if (operation == null) {
            operation = new ArrayList<ReceivePlanType_sql2005sp2 .Operation_sql2005sp2>();
        }
        return this.operation;
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
     *       &lt;/sequence>
     *       &lt;attribute name="OperationType">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="ReceivePlanSelect"/>
     *             &lt;enumeration value="ReceivePlanUpdate"/>
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
        "queryPlan"
    })
    public static class Operation_sql2005sp2 {

        @XmlElement(name = "QueryPlan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected QueryPlanType_sql2005sp2 queryPlan;
        @XmlAttribute(name = "OperationType")
        protected String operationType;

        /**
         * Gets the value of the queryPlan property.
         * 
         * @return
         *     possible object is
         *     {@link QueryPlanType_sql2005sp2 }
         *     
         */
        public QueryPlanType_sql2005sp2 getQueryPlan() {
            return queryPlan;
        }

        /**
         * Sets the value of the queryPlan property.
         * 
         * @param value
         *     allowed object is
         *     {@link QueryPlanType_sql2005sp2 }
         *     
         */
        public void setQueryPlan(QueryPlanType_sql2005sp2 value) {
            this.queryPlan = value;
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
