<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>
    <xsl:template match="/">
        <xsl:text>svn.revision=</xsl:text>
        <xsl:value-of select="/info/entry/commit/@revision"/>
    </xsl:template>
</xsl:stylesheet>
