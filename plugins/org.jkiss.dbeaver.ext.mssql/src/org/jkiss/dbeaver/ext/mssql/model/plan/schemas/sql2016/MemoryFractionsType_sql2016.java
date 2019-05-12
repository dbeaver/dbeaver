
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * For memory consuming relational operators, show fraction of memory grant iterator will use
 * 
 * <p>Java class for MemoryFractionsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MemoryFractionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attribute name="Input" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="Output" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MemoryFractionsType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class MemoryFractionsType_sql2016 {

    @XmlAttribute(name = "Input", required = true)
    protected double input;
    @XmlAttribute(name = "Output", required = true)
    protected double output;

    /**
     * Gets the value of the input property.
     * 
     */
    public double getInput() {
        return input;
    }

    /**
     * Sets the value of the input property.
     * 
     */
    public void setInput(double value) {
        this.input = value;
    }

    /**
     * Gets the value of the output property.
     * 
     */
    public double getOutput() {
        return output;
    }

    /**
     * Sets the value of the output property.
     * 
     */
    public void setOutput(double value) {
        this.output = value;
    }

}
