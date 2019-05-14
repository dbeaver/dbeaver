
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ConvertType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConvertType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Style" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="ScalarOperator" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="DataType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Length" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Precision" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Scale" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Style" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="Implicit" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConvertType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "convertStyle",
    "scalarOperator"
})
public class ConvertType_sql2017 {

    @XmlElement(name = "Style", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 convertStyle;
    @XmlElement(name = "ScalarOperator", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ScalarType_sql2017 scalarOperator;
    @XmlAttribute(name = "DataType", required = true)
    protected String dataType;
    @XmlAttribute(name = "Length")
    protected Integer length;
    @XmlAttribute(name = "Precision")
    protected Integer precision;
    @XmlAttribute(name = "Scale")
    protected Integer scale;
    @XmlAttribute(name = "Style", required = true)
    protected int style;
    @XmlAttribute(name = "Implicit", required = true)
    protected boolean implicit;

    /**
     * Gets the value of the convertStyle property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getConvertStyle() {
        return convertStyle;
    }

    /**
     * Sets the value of the convertStyle property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setConvertStyle(ScalarExpressionType_sql2017 value) {
        this.convertStyle = value;
    }

    /**
     * Gets the value of the scalarOperator property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarType_sql2017 }
     *     
     */
    public ScalarType_sql2017 getScalarOperator() {
        return scalarOperator;
    }

    /**
     * Sets the value of the scalarOperator property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarType_sql2017 }
     *     
     */
    public void setScalarOperator(ScalarType_sql2017 value) {
        this.scalarOperator = value;
    }

    /**
     * Gets the value of the dataType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Sets the value of the dataType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataType(String value) {
        this.dataType = value;
    }

    /**
     * Gets the value of the length property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getLength() {
        return length;
    }

    /**
     * Sets the value of the length property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setLength(Integer value) {
        this.length = value;
    }

    /**
     * Gets the value of the precision property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * Sets the value of the precision property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPrecision(Integer value) {
        this.precision = value;
    }

    /**
     * Gets the value of the scale property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getScale() {
        return scale;
    }

    /**
     * Sets the value of the scale property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setScale(Integer value) {
        this.scale = value;
    }

    /**
     * Gets the value of the style property.
     * 
     */
    public int getStyle() {
        return style;
    }

    /**
     * Sets the value of the style property.
     * 
     */
    public void setStyle(int value) {
        this.style = value;
    }

    /**
     * Gets the value of the implicit property.
     * 
     */
    public boolean isImplicit() {
        return implicit;
    }

    /**
     * Sets the value of the implicit property.
     * 
     */
    public void setImplicit(boolean value) {
        this.implicit = value;
    }

}
