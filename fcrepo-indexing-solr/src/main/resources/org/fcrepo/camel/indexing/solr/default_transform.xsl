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
        </doc>
    </add>
    </xsl:template>

</xsl:stylesheet>
