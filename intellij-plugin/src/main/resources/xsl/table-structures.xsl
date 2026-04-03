<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <xsl:template match="TableStructure">
        <xsl:if test="$showTables">
            <xsl:variable name="classNameWithPackage" select="@className"/>
            <xsl:variable name="className" select="f:class-name(@className)"/>

            <xsl:variable name="classType">
                <xsl:value-of select="concat($bb, $tableStructureSpot, 'TableStructure', $ff)"/>
            </xsl:variable>

            <!-- Class definition -->
            <xsl:if test="f:matches-package-filter($classNameWithPackage)">
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:if test="$showMavenModule = 'true' and @mavenModule">
                    <xsl:value-of select="concat('  ', @mavenModule, '&#xa;')"/>
                    <xsl:text>  --&#xa;</xsl:text>
                </xsl:if>
                <xsl:for-each select="Column">
                    <xsl:sort select="@name"/>
                    <xsl:value-of select="concat('  ', @name, ': ', f:substring-after-last(@datatype, '.'), '&#xa;')"/>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>

                <!-- Datatype associations -->
                <xsl:for-each select="Column">
                    <xsl:call-template name="enum-association">
                        <xsl:with-param name="enumType" select="@datatype" />
                        <xsl:with-param name="className" select="$className" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
