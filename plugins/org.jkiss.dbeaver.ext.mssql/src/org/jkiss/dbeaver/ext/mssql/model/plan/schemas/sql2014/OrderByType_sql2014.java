
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OrderByType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OrderByType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OrderByColumn" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="Ascending" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
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
@XmlType(name = "OrderByType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "orderByColumn"
})
public class OrderByType_sql2014 {

    @XmlElement(name = "OrderByColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<OrderByType_sql2014 .OrderByColumn_sql2014> orderByColumn;

    /**
     * Gets the value of the orderByColumn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the orderByColumn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrderByColumn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OrderByType_sql2014 .OrderByColumn_sql2014 }
     * 
     * 
     */
    public List<OrderByType_sql2014 .OrderByColumn_sql2014> getOrderByColumn() {
        if (orderByColumn == null) {
            orderByColumn = new ArrayList<OrderByType_sql2014 .OrderByColumn_sql2014>();
        }
        return this.orderByColumn;
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
     *         &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType"/>
     *       &lt;/sequence>
     *       &lt;attribute name="Ascending" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "columnReference"
    })
    public static class OrderByColumn_sql2014 {

        @XmlElement(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected ColumnReferenceType_sql2014 columnReference;
        @XmlAttribute(name = "Ascending", required = true)
        protected boolean ascending;

        /**
         * Gets the value of the columnReference property.
         * 
         * @return
         *     possible object is
         *     {@link ColumnReferenceType_sql2014 }
         *     
         */
        public ColumnReferenceType_sql2014 getColumnReference() {
            return columnReference;
        }

        /**
         * Sets the value of the columnReference property.
         * 
         * @param value
         *     allowed object is
         *     {@link ColumnReferenceType_sql2014 }
         *     
         */
        public void setColumnReference(ColumnReferenceType_sql2014 value) {
            this.columnReference = value;
        }

        /**
         * Gets the value of the ascending property.
         * 
         */
        public boolean isAscending() {
            return ascending;
        }

        /**
         * Sets the value of the ascending property.
         * 
         */
        public void setAscending(boolean value) {
            this.ascending = value;
        }

    }

}
