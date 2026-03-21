<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="TableStructure">
        <xsl:if test="$showTables">
            <xsl:variable name="classNameWithPackage" select="@className"/>

            <xsl:variable name="className">
                <xsl:call-template name="packaging-selector">
                    <xsl:with-param name="clazz" select="@className" />
                </xsl:call-template>
            </xsl:variable>

            <xsl:variable name="classType">
                <xsl:value-of select="concat($bb, $tableStructureSpot, 'TableStructure', $ff)"/>
            </xsl:variable>

            <!-- Class definition -->
            <xsl:variable name="matchesFilter">
                <xsl:call-template name="matches-package-filter">
                    <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:if test="$matchesFilter = 'true'">
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:for-each select="Column">
                    <xsl:sort select="@name"/>
                    <xsl:variable name="datatype">
                        <xsl:call-template name="substring-after-last">
                            <xsl:with-param name="string" select="@datatype" />
                            <xsl:with-param name="delimiter" select="'.'" />
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:value-of select="concat('  ', @name, ': ', $datatype, '&#xa;')"/>
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