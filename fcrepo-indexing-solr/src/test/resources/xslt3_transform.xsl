<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:map="http://www.w3.org/2005/xpath-functions/map"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:f="http://fcrepo.org/test"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:fedora="http://fedora.info/definitions/v4/repository#"
    exclude-result-prefixes="xs map fn f rdf fedora">

    <!-- XSLT 3.0: declarative default mode with on-no-match -->
    <xsl:mode on-no-match="shallow-skip"/>

    <!-- XSLT 2.0/3.0: stylesheet function with typed signature -->
    <xsl:function name="f:localname" as="xs:string">
        <xsl:param name="uri" as="xs:string"/>
        <xsl:sequence select="tokenize($uri, '/')[last()]"/>
    </xsl:function>

    <xsl:template match="/">
        <add>
            <doc>
                <field name="id">
                    <xsl:value-of select="rdf:RDF/rdf:Description/@rdf:about"/>
                </field>

                <!-- XSLT 3.0: higher-order functions (for-each + inline function) -->
                <field name="rdftype_short">
                    <xsl:value-of separator=" "
                        select="fn:for-each(
                                  rdf:RDF/rdf:Description/rdf:type/@rdf:resource ! string(),
                                  function($u as xs:string) as xs:string { f:localname($u) })"/>
                </field>

                <!-- XSLT 2.0: grouping -->
                <xsl:for-each-group
                    select="rdf:RDF/rdf:Description/rdf:type/@rdf:resource"
                    group-by="tokenize(., '#|/')[last()]">
                    <field name="typegroup">
                        <xsl:value-of select="current-grouping-key()"/>
                    </field>
                </xsl:for-each-group>

                <!-- XSLT 3.0: maps and JSON serialization -->
                <xsl:variable name="m" as="map(xs:string, xs:string)"
                    select="map { 'src': 'fcrepo', 'ver': '3.0' }"/>
                <field name="meta_json">
                    <xsl:value-of select="fn:serialize($m, map { 'method': 'json' })"/>
                </field>

                <!-- XSLT 3.0: xsl:iterate -->
                <xsl:iterate select="1 to 3">
                    <xsl:param name="acc" as="xs:integer" select="0"/>
                    <xsl:on-completion>
                        <field name="counter"><xsl:value-of select="$acc"/></field>
                    </xsl:on-completion>
                    <xsl:next-iteration>
                        <xsl:with-param name="acc" select="$acc + ."/>
                    </xsl:next-iteration>
                </xsl:iterate>
            </doc>
        </add>
    </xsl:template>
</xsl:stylesheet>
