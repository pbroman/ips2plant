<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="flag" select="'true'"/>

    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="test/moo">
        <xsl:if test="$flag='true'">
            <xsl:value-of select="."/>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>