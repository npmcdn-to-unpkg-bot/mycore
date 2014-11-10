<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="response" select="/response" />
  <xsl:template match="/">
    <xsl:variable name="normalResult" select="response/result/doc[1]" />
    <xsl:variable name="groupedResult" select="response/lst[@name='grouped']/lst[@name='returnId']/arr[@name='groups']/lst/result/doc[1]" />
    <xsl:apply-templates select="$normalResult|$groupedResult" />
  </xsl:template>
  <xsl:template match="doc">
    <xsl:variable name="objId" select="str[@name='id']" />
    <xsl:apply-templates select="document(concat('mcrobject:',$objId))" mode="attachResponse" />
  </xsl:template>
  <xsl:template match="/*" mode="attachResponse">
    <xsl:copy>
      <xsl:copy-of select="@*|node()" />
      <xsl:copy-of select="$response" />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>