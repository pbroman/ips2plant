<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="urn:ips2plant:functions">

    <!-- Checks if a class name matches the package filter (comma-separated list) -->
    <xsl:function name="f:matches-package-filter" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="classNameWithPackage" as="xs:string"/>
        <xsl:param name="filters" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$filters = ''"><xsl:sequence select="true()"/></xsl:when>
            <xsl:when test="contains($filters, ',')">
                <xsl:sequence select="
                    if (starts-with($classNameWithPackage, normalize-space(substring-before($filters, ','))))
                    then true()
                    else f:matches-package-filter($classNameWithPackage, substring-after($filters, ','))
                "/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:sequence select="starts-with($classNameWithPackage, normalize-space($filters))"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Convenience overload using the global $packageFilter param -->
    <xsl:function name="f:matches-package-filter" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="classNameWithPackage" as="xs:string"/>
        <xsl:sequence select="f:matches-package-filter($classNameWithPackage, $packageFilter)"/>
    </xsl:function>

    <!-- Selects the class name with or without package prefix depending on $packages param -->
    <xsl:function name="f:class-name" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="clazz" as="xs:string"/>
        <xsl:sequence select="
            if ($packages) then $clazz
            else f:substring-after-last($clazz, '.')
        "/>
    </xsl:function>

    <!-- Returns the substring after the last occurrence of delimiter -->
    <xsl:function name="f:substring-after-last" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="string" as="xs:string"/>
        <xsl:param name="delimiter" as="xs:string"/>
        <xsl:sequence select="
            if (contains($string, $delimiter))
            then f:substring-after-last(substring-after($string, $delimiter), $delimiter)
            else $string
        "/>
    </xsl:function>

    <!--
        Determines whether a relationship (composition, association, aggregation) should be shown.
        A relationship is visible when:
        1. The target is present in the model OR addAssociations is enabled, AND
        2. Both source and target pass the package filter (or addAssociations allows cross-filter visibility)
    -->
    <xsl:function name="f:is-relationship-visible" as="xs:boolean"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="isTargetPresent" as="xs:string"/>
        <xsl:param name="sourceWithPackage" as="xs:string"/>
        <xsl:param name="targetWithPackage" as="xs:string"/>
        <xsl:variable name="sourceMatch" select="f:matches-package-filter($sourceWithPackage)"/>
        <xsl:variable name="targetMatch" select="f:matches-package-filter($targetWithPackage)"/>
        <xsl:sequence select="
            ($isTargetPresent != '' or $addAssociations = 'true')
            and (($sourceMatch or $targetMatch) and $addAssociations = 'true'
                 or ($sourceMatch and $targetMatch))
        "/>
    </xsl:function>

    <!-- Checks whether a target class exists in the model for the given component type -->
    <xsl:template name="is-target-present">
        <xsl:param name="componentType"/>
        <xsl:param name="targetClassName"/>
        <xsl:choose>
            <xsl:when test="$componentType = 'PolicyCmptType'"><xsl:value-of select="../../PolicyCmptType[@className=$targetClassName]"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="../../ProductCmptType2[@className=$targetClassName]"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Integer square root (floor), used to compute note wrap width -->
    <xsl:function name="f:isqrt" as="xs:integer"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="n" as="xs:integer"/>
        <xsl:sequence select="f:isqrt-step($n, 1)"/>
    </xsl:function>

    <xsl:function name="f:isqrt-step" as="xs:integer"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="n" as="xs:integer"/>
        <xsl:param name="x" as="xs:integer"/>
        <xsl:sequence select="if (($x + 1) * ($x + 1) > $n) then $x else f:isqrt-step($n, $x + 1)"/>
    </xsl:function>

    <!-- Word-wraps text at the given width, preserving paragraph breaks (blank lines) -->
    <xsl:function name="f:wrap-text" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="text" as="xs:string"/>
        <xsl:param name="width" as="xs:integer"/>
        <xsl:sequence select="string-join(
            for $para in tokenize($text, '\n\s*\n')
            return f:wrap-paragraph(normalize-space($para), $width),
            '&#xa;&#xa;'
        )"/>
    </xsl:function>

    <xsl:function name="f:wrap-paragraph" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="para" as="xs:string"/>
        <xsl:param name="width" as="xs:integer"/>
        <xsl:sequence select="f:wrap-words(tokenize($para, '\s+'), $width, '', '')"/>
    </xsl:function>

    <xsl:function name="f:wrap-words" as="xs:string"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xsl:param name="words" as="xs:string*"/>
        <xsl:param name="width" as="xs:integer"/>
        <xsl:param name="line" as="xs:string"/>
        <xsl:param name="result" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="empty($words)">
                <xsl:sequence select="
                    if ($line != '') then
                        if ($result != '') then concat($result, '&#xa;', $line) else $line
                    else $result
                "/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="word" select="$words[1]"/>
                <xsl:variable name="candidate" select="if ($line = '') then $word else concat($line, ' ', $word)"/>
                <xsl:choose>
                    <xsl:when test="string-length($candidate) &lt;= $width or $line = ''">
                        <xsl:sequence select="f:wrap-words(subsequence($words, 2), $width, $candidate, $result)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:variable name="newResult" select="if ($result != '') then concat($result, '&#xa;', $line) else $line"/>
                        <xsl:sequence select="f:wrap-words(subsequence($words, 2), $width, $word, $newResult)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- Renders a class description as a PlantUML note left of the class, word-wrapped to a roughly square shape -->
    <xsl:template name="class-description-note">
        <xsl:param name="className"/>
        <xsl:param name="description"/>
        <xsl:if test="$showDescriptions = 'true' and normalize-space($description) != ''">
            <xsl:variable name="width" select="max((35, f:isqrt(string-length($description) * 2)))"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema"/>
            <xsl:value-of select="concat('note left of ', $className, '&#xa;')"/>
            <xsl:value-of select="f:wrap-text($description, $width)"/>
            <xsl:text>&#xa;end note&#xa;</xsl:text>
        </xsl:if>
    </xsl:template>

    <!-- Renders an attribute description as a PlantUML note right of the attribute -->
    <xsl:template name="attribute-description-note">
        <xsl:param name="className"/>
        <xsl:param name="attrName"/>
        <xsl:param name="description"/>
        <xsl:if test="$showDescriptions = 'true' and normalize-space($description) != ''">
            <xsl:value-of select="concat('note right of ', $className, '::', $attrName, '&#xa;')"/>
            <xsl:value-of select="$description"/>
            <xsl:text>&#xa;end note&#xa;</xsl:text>
        </xsl:if>
    </xsl:template>

    <!-- Renders the target role label if enabled -->
    <xsl:template name="target-role-label">
        <xsl:if test="@targetRoleSingular and $printTargetRole = 'true'">
            <xsl:value-of select="concat(' : ', @targetRoleSingular)"/>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
