
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IndexKindType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="IndexKindType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Heap"/>
 *     &lt;enumeration value="Clustered"/>
 *     &lt;enumeration value="FTSChangeTracking"/>
 *     &lt;enumeration value="FTSMapping"/>
 *     &lt;enumeration value="NonClustered"/>
 *     &lt;enumeration value="PrimaryXML"/>
 *     &lt;enumeration value="SecondaryXML"/>
 *     &lt;enumeration value="Spatial"/>
 *     &lt;enumeration value="ViewClustered"/>
 *     &lt;enumeration value="ViewNonClustered"/>
 *     &lt;enumeration value="NonClusteredHash"/>
 *     &lt;enumeration value="SelectiveXML"/>
 *     &lt;enumeration value="SecondarySelectiveXML"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "IndexKindType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
@XmlEnum
public enum IndexKindType_sql2016sp1 {

    @XmlEnumValue("Heap")
    HEAP("Heap"),
    @XmlEnumValue("Clustered")
    CLUSTERED("Clustered"),
    @XmlEnumValue("FTSChangeTracking")
    FTS_CHANGE_TRACKING("FTSChangeTracking"),
    @XmlEnumValue("FTSMapping")
    FTS_MAPPING("FTSMapping"),
    @XmlEnumValue("NonClustered")
    NON_CLUSTERED("NonClustered"),
    @XmlEnumValue("PrimaryXML")
    PRIMARY_XML("PrimaryXML"),
    @XmlEnumValue("SecondaryXML")
    SECONDARY_XML("SecondaryXML"),
    @XmlEnumValue("Spatial")
    SPATIAL("Spatial"),
    @XmlEnumValue("ViewClustered")
    VIEW_CLUSTERED("ViewClustered"),
    @XmlEnumValue("ViewNonClustered")
    VIEW_NON_CLUSTERED("ViewNonClustered"),
    @XmlEnumValue("NonClusteredHash")
    NON_CLUSTERED_HASH("NonClusteredHash"),
    @XmlEnumValue("SelectiveXML")
    SELECTIVE_XML("SelectiveXML"),
    @XmlEnumValue("SecondarySelectiveXML")
    SECONDARY_SELECTIVE_XML("SecondarySelectiveXML");
    private final String value;

    IndexKindType_sql2016sp1(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static IndexKindType_sql2016sp1 fromValue(String v) {
        for (IndexKindType_sql2016sp1 c: IndexKindType_sql2016sp1 .values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
