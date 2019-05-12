
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ConstantScanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConstantScanType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="Values" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Row" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConstantScanType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "values"
})
public class ConstantScanType_sql2014
    extends RelOpBaseType_sql2014
{

    @XmlElement(name = "Values", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ConstantScanType_sql2014 .Values_sql2014 values;

    /**
     * Gets the value of the values property.
     * 
     * @return
     *     possible object is
     *     {@link ConstantScanType_sql2014 .Values_sql2014 }
     *     
     */
    public ConstantScanType_sql2014 .Values_sql2014 getValues() {
        return values;
    }

    /**
     * Sets the value of the values property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConstantScanType_sql2014 .Values_sql2014 }
     *     
     */
    public void setValues(ConstantScanType_sql2014 .Values_sql2014 value) {
        this.values = value;
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
     *         &lt;element name="Row" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "row"
    })
    public static class Values_sql2014 {

        @XmlElement(name = "Row", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected List<ScalarExpressionListType_sql2014> row;

        /**
         * Gets the value of the row property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the row property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getRow().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ScalarExpressionListType_sql2014 }
         * 
         * 
         */
        public List<ScalarExpressionListType_sql2014> getRow() {
            if (row == null) {
                row = new ArrayList<ScalarExpressionListType_sql2014>();
            }
            return this.row;
        }

    }

}
