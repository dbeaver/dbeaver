
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * The set options that affects query cost
 * 
 * <p>Java class for SetOptionsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SetOptionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="ANSI_NULLS" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ANSI_PADDING" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ANSI_WARNINGS" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="ARITHABORT" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="CONCAT_NULL_YIELDS_NULL" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="NUMERIC_ROUNDABORT" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="QUOTED_IDENTIFIER" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetOptionsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class SetOptionsType_sql2014sp2 {

    @XmlAttribute(name = "ANSI_NULLS")
    protected Boolean ansinulls;
    @XmlAttribute(name = "ANSI_PADDING")
    protected Boolean ansipadding;
    @XmlAttribute(name = "ANSI_WARNINGS")
    protected Boolean ansiwarnings;
    @XmlAttribute(name = "ARITHABORT")
    protected Boolean arithabort;
    @XmlAttribute(name = "CONCAT_NULL_YIELDS_NULL")
    protected Boolean concatnullyieldsnull;
    @XmlAttribute(name = "NUMERIC_ROUNDABORT")
    protected Boolean numericroundabort;
    @XmlAttribute(name = "QUOTED_IDENTIFIER")
    protected Boolean quotedidentifier;

    /**
     * Gets the value of the ansinulls property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getANSINULLS() {
        return ansinulls;
    }

    /**
     * Sets the value of the ansinulls property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setANSINULLS(Boolean value) {
        this.ansinulls = value;
    }

    /**
     * Gets the value of the ansipadding property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getANSIPADDING() {
        return ansipadding;
    }

    /**
     * Sets the value of the ansipadding property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setANSIPADDING(Boolean value) {
        this.ansipadding = value;
    }

    /**
     * Gets the value of the ansiwarnings property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getANSIWARNINGS() {
        return ansiwarnings;
    }

    /**
     * Sets the value of the ansiwarnings property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setANSIWARNINGS(Boolean value) {
        this.ansiwarnings = value;
    }

    /**
     * Gets the value of the arithabort property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getARITHABORT() {
        return arithabort;
    }

    /**
     * Sets the value of the arithabort property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setARITHABORT(Boolean value) {
        this.arithabort = value;
    }

    /**
     * Gets the value of the concatnullyieldsnull property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getCONCATNULLYIELDSNULL() {
        return concatnullyieldsnull;
    }

    /**
     * Sets the value of the concatnullyieldsnull property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCONCATNULLYIELDSNULL(Boolean value) {
        this.concatnullyieldsnull = value;
    }

    /**
     * Gets the value of the numericroundabort property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getNUMERICROUNDABORT() {
        return numericroundabort;
    }

    /**
     * Sets the value of the numericroundabort property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNUMERICROUNDABORT(Boolean value) {
        this.numericroundabort = value;
    }

    /**
     * Gets the value of the quotedidentifier property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getQUOTEDIDENTIFIER() {
        return quotedidentifier;
    }

    /**
     * Sets the value of the quotedidentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setQUOTEDIDENTIFIER(Boolean value) {
        this.quotedidentifier = value;
    }

}
