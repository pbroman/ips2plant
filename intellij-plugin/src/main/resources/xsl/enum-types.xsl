<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <xsl:template match="EnumType">
        <xsl:if test="$showEnumTypes">
            <xsl:variable name="classNameWithPackage" select="@className"/>
            <xsl:variable name="className" select="f:class-name(@className)"/>

            <xsl:variable name="classType">
                <xsl:value-of select="$bb"/>
                <xsl:if test="not(@abstract = 'true')">
                    <xsl:text>(E,</xsl:text>
                    <xsl:choose>
                        <xsl:when test="@extensible">
                            <xsl:text>MediumSpringGreen) extensible </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>MediumSeaGreen)</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:value-of select="concat('EnumType', $ff)"/>
            </xsl:variable>

            <!-- Class definition -->
            <xsl:if test="f:matches-package-filter($classNameWithPackage)">
                <xsl:if test="@abstract='true'">
                    <xsl:text>abstract </xsl:text>
                </xsl:if>
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:if test="$showMavenModule = 'true' and @mavenModule">
                    <xsl:value-of select="concat('  ', @mavenModule, '&#xa;')"/>
                    <xsl:text>  --&#xa;</xsl:text>
                </xsl:if>
                <xsl:for-each select="EnumAttribute">
                    <xsl:variable name="datatype">
                        <xsl:choose>
                            <xsl:when test="@datatype = '' and @inherited = 'true'">inherited</xsl:when>
                            <xsl:otherwise><xsl:value-of select="@datatype"/></xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:value-of select="concat('  ', @name, ': ', $datatype, '&#xa;')"/>
                </xsl:for-each>

                <xsl:for-each select="EnumValue">
                    <xsl:value-of select="concat('  ',./EnumLiteralNameAttributeValue, ' (', ./EnumAttributeValue[1], ')&#xa;')"/>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>

                <!-- Description notes -->
                <xsl:call-template name="class-description-note">
                    <xsl:with-param name="className" select="$className"/>
                    <xsl:with-param name="description" select="(Description[@locale=$descriptionLocale], Description[@locale='de'])[1]"/>
                </xsl:call-template>
                <xsl:for-each select="EnumAttribute">
                    <xsl:call-template name="attribute-description-note">
                        <xsl:with-param name="className" select="$className"/>
                        <xsl:with-param name="attrName" select="@name"/>
                        <xsl:with-param name="description" select="(Description[@locale=$descriptionLocale], Description[@locale='de'])[1]"/>
                    </xsl:call-template>
                </xsl:for-each>

                <xsl:for-each select="EnumAttribute">
                    <xsl:call-template name="enum-association">
                        <xsl:with-param name="enumType" select="@datatype" />
                        <xsl:with-param name="className" select="$className" />
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- Inheritance -->
            <xsl:variable name="isSupertypePresent">
                <xsl:value-of select="../EnumType[@className=current()/@superEnumType]"/>
            </xsl:variable>
            <xsl:call-template name="inheritance">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
                <xsl:with-param name="supertypeAttr" select="@superEnumType"/>
                <xsl:with-param name="isSupertypePresent" select="$isSupertypePresent"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- Enum content (values for extensible enum types) -->
    <xsl:template match="EnumContent">
        <xsl:if test="$showEnumContent">
            <xsl:variable name="classNameWithPackage" select="@className"/>
            <xsl:variable name="className" select="f:class-name(@className)"/>
            <xsl:variable name="classType" select="concat($bb, '(E,GreenYellow) EnumContent', $ff)"/>

            <!-- Class definition -->
            <xsl:if test="f:matches-package-filter($classNameWithPackage)">
                <xsl:value-of select="concat('class ', $className, $classType, ' { &#xa;')"/>
                <xsl:if test="$showMavenModule = 'true' and @mavenModule">
                    <xsl:value-of select="concat('  ', @mavenModule, '&#xa;')"/>
                    <xsl:text>  --&#xa;</xsl:text>
                </xsl:if>
                <!-- Header: attribute names from the corresponding EnumType -->
                <xsl:variable name="linkedEnumType" select="../EnumType[@className=current()/@enumType]"/>
                <xsl:choose>
                    <xsl:when test="$linkedEnumType">
                        <xsl:text>  </xsl:text>
                        <xsl:for-each select="$linkedEnumType/EnumAttribute">
                            <xsl:if test="position() &gt; 1">
                                <xsl:text> | </xsl:text>
                            </xsl:if>
                            <xsl:value-of select="@name"/>
                        </xsl:for-each>
                        <xsl:text>&#xa;</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>  Enum Type not in Subset&#xa;</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text>  --&#xa;</xsl:text>
                <xsl:for-each select="EnumValue">
                    <xsl:text>  </xsl:text>
                    <xsl:for-each select="EnumAttributeValue">
                        <xsl:if test="position() &gt; 1">
                            <xsl:text> | </xsl:text>
                        </xsl:if>
                        <xsl:choose>
                            <xsl:when test="InternationalString">
                                <xsl:value-of select="(InternationalString/LocalizedString[@locale=$descriptionLocale], InternationalString/LocalizedString[@locale='de'])[1]/@text"/>
                            </xsl:when>
                            <xsl:when test="@isNull = 'true'">
                                <xsl:text>null</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                    <xsl:text>&#xa;</xsl:text>
                </xsl:for-each>
                <xsl:text>}&#xa;</xsl:text>
            </xsl:if>

            <!-- Inheritance to enum type -->
            <xsl:variable name="isEnumTypePresent">
                <xsl:value-of select="../EnumType[@className=current()/@enumType]"/>
            </xsl:variable>
            <xsl:call-template name="inheritance">
                <xsl:with-param name="classNameWithPackage" select="$classNameWithPackage"/>
                <xsl:with-param name="className" select="$className"/>
                <xsl:with-param name="supertypeAttr" select="@enumType"/>
                <xsl:with-param name="isSupertypePresent" select="$isEnumTypePresent"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
