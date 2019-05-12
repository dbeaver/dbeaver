
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DefinedValuesListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DefinedValuesListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DefinedValue" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;choice>
 *                     &lt;element name="ValueVector">
 *                       &lt;complexType>
 *                         &lt;complexContent>
 *                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                             &lt;sequence>
 *                               &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded" minOccurs="2"/>
 *                             &lt;/sequence>
 *                           &lt;/restriction>
 *                         &lt;/complexContent>
 *                       &lt;/complexType>
 *                     &lt;/element>
 *                     &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType"/>
 *                   &lt;/choice>
 *                   &lt;choice minOccurs="0">
 *                     &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded"/>
 *                     &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType"/>
 *                   &lt;/choice>
 *                 &lt;/sequence>
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
@XmlType(name = "DefinedValuesListType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "definedValue"
})
public class DefinedValuesListType_sql2016sp1 {

    @XmlElement(name = "DefinedValue", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1> definedValue;

    /**
     * Gets the value of the definedValue property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the definedValue property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDefinedValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 }
     * 
     * 
     */
    public List<DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1> getDefinedValue() {
        if (definedValue == null) {
            definedValue = new ArrayList<DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1>();
        }
        return this.definedValue;
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
     *         &lt;choice>
     *           &lt;element name="ValueVector">
     *             &lt;complexType>
     *               &lt;complexContent>
     *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                   &lt;sequence>
     *                     &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded" minOccurs="2"/>
     *                   &lt;/sequence>
     *                 &lt;/restriction>
     *               &lt;/complexContent>
     *             &lt;/complexType>
     *           &lt;/element>
     *           &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType"/>
     *         &lt;/choice>
     *         &lt;choice minOccurs="0">
     *           &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded"/>
     *           &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType"/>
     *         &lt;/choice>
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
        "content"
    })
    public static class DefinedValue_sql2016sp1 {

        @XmlElementRefs({
            @XmlElementRef(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = JAXBElement.class, required = false),
            @XmlElementRef(name = "ValueVector", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = JAXBElement.class, required = false),
            @XmlElementRef(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", type = JAXBElement.class, required = false)
        })
        protected List<JAXBElement<?>> content;

        /**
         * Gets the rest of the content model. 
         * 
         * <p>
         * You are getting this "catch-all" property because of the following reason: 
         * The field name "ColumnReference" is used by two different parts of a schema. See: 
         * line 404 of file:/C:/dbeaver/mssql_plans/schemas/sql2016sp1/showplanxml.xsd
         * line 401 of file:/C:/dbeaver/mssql_plans/schemas/sql2016sp1/showplanxml.xsd
         * <p>
         * To get rid of this property, apply a property customization to one 
         * of both of the following declarations to change their names: 
         * Gets the value of the content property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the content property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getContent().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link JAXBElement }{@code <}{@link ColumnReferenceType_sql2016sp1 }{@code >}
         * {@link JAXBElement }{@code <}{@link DefinedValuesListType_sql2016sp1 .DefinedValue_sql2016sp1 .ValueVector_sql2016sp1 }{@code >}
         * {@link JAXBElement }{@code <}{@link ScalarType_sql2016sp1 }{@code >}
         * 
         * 
         */
        public List<JAXBElement<?>> getContent() {
            if (content == null) {
                content = new ArrayList<JAXBElement<?>>();
            }
            return this.content;
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
         *         &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded" minOccurs="2"/>
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
            "columnReference"
        })
        public static class ValueVector_sql2016sp1 {

            @XmlElement(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
            protected List<ColumnReferenceType_sql2016sp1> columnReference;

            /**
             * Gets the value of the columnReference property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the columnReference property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getColumnReference().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link ColumnReferenceType_sql2016sp1 }
             * 
             * 
             */
            public List<ColumnReferenceType_sql2016sp1> getColumnReference() {
                if (columnReference == null) {
                    columnReference = new ArrayList<ColumnReferenceType_sql2016sp1>();
                }
                return this.columnReference;
            }

        }

    }

}
