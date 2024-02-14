<?xml version="1.0" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:fedora="http://fedora.info/definitions/v4/repository#"
    xmlns:ldp="http://www.w3.org/ns/ldp#">

    <xsl:template match="/">
    <add>
        <doc>
            <field name="id"><xsl:value-of select="rdf:RDF/rdf:Description/@rdf:about" /></field>
            <xsl:for-each select="rdf:RDF/rdf:Description/rdf:type">
                <field name="rdftype"><xsl:value-of select="@rdf:resource" /></field>
            </xsl:for-each>
            <field name="contains"><xsl:value-of select="rdf:RDF/rdf:Description/ldp:contains/@rdf:resource" /></field>
            <field name="lastmodified"><xsl:value-of select="rdf:RDF/rdf:Description/fedora:lastModified" /></field>
            <field name="created"><xsl:value-of select="rdf:RDF/rdf:Description/fedora:created" /></field>
        </doc>
    </add>
    </xsl:template>

</xsl:stylesheet>
