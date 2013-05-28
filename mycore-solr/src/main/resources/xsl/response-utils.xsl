<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:encoder="xalan://java.net.URLEncoder" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  exclude-result-prefixes="mcrxsl encoder xalan i18n">
  <xsl:param name="RequestURL" />
  <xsl:key name="derivate" match="/response/response[@subresult='derivate']/result/doc" use="str[@name='returnId']" />
  <xsl:key name="file" match="/response/response[@subresult='unmerged']/result/doc" use="str[@name='returnId']" />
  <xsl:variable name="params" select="/response/lst[@name='responseHeader']/lst[@name='params']" />
  <xsl:variable name="result" select="/response/result[@name='response']" />
  <xsl:variable name="hits" select="number($result/@numFound)" />
  <xsl:variable name="start" select="number($result/@start)" />
  <xsl:variable name="rowTemp">
    <xsl:choose>
      <xsl:when test="xalan:nodeset($params)/str[@name='rows']">
        <xsl:value-of select="number(xalan:nodeset($params)/str[@name='rows'])" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="docCount" select="count($result/doc)" />
        <xsl:choose>
          <xsl:when test="$result/@numFound &gt; $docCount">
            <xsl:value-of select="$docCount" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="number($result/@numFound)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="rows" select="number($rowTemp)" />
  <xsl:variable name="currentPage" select="ceiling((($start + 1) - $rows) div $rows)+1" />
  <xsl:variable name="query" select="xalan:nodeset($params)/str[@name='q']" />
  <xsl:variable name="proxyBaseURL">
    <xsl:choose>
      <xsl:when test="string-length($HttpSession) &gt; 0 and contains($RequestURL, $HttpSession)">
        <xsl:value-of select="substring-before($RequestURL, $HttpSession)" />
      </xsl:when>
      <xsl:when test="contains($RequestURL, '?')">
        <xsl:value-of select="substring-before($RequestURL, '?')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$RequestURL" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="totalPages">
    <xsl:choose>
      <xsl:when test="$hits = 0">
        <xsl:value-of select="1" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="ceiling($hits div $rows )" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- retain the original query parameters, for attaching them to a url -->
  <xsl:variable name="solrParams">
    <xsl:value-of select="'?'" />
    <xsl:for-each select="$params/str[not(@name='start' or @name='rows')]">
      <!-- parameterName=parameterValue -->
      <xsl:value-of select="concat(@name,'=', encoder:encode(., 'UTF-8'))" />
      <xsl:if test="not (position() = last())">
        <xsl:value-of select="'&amp;'" />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>
  <xsl:template name="solr.Pagination">
    <xsl:param name="i" select="1" />
    <xsl:param name="href" select="concat($proxyBaseURL,$HttpSession,$solrParams)" />
    <xsl:param name="size" />
    <xsl:param name="currentpage" />
    <xsl:param name="totalpage" />
    <xsl:variable name="prev" select="'«'" />
    <xsl:variable name="next" select="'»'" />

    <div class="pagination pagination-centered">
      <ul>
        <li>
          <xsl:choose>
            <xsl:when test="$currentpage = 1">
              <xsl:attribute name="class">
                <xsl:value-of select="'disabled'" />
              </xsl:attribute>
              <a href="#">
                <xsl:value-of select="$prev" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <a href="{concat($href, '&amp;start=',(($currentpage -2) * $size), '&amp;rows=', $size)}">
                <xsl:value-of select="$prev" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </li>
        <xsl:call-template name="solr.PageGen">
          <xsl:with-param name="i" select="$i" />
          <xsl:with-param name="href" select="$href" />
          <xsl:with-param name="size" select="$size" />
          <xsl:with-param name="currentpage" select="$currentpage" />
          <xsl:with-param name="totalpage" select="$totalpage" />
        </xsl:call-template>
        <li>
          <xsl:choose>
            <xsl:when test="$currentpage = $totalpage">
              <xsl:attribute name="class">
                <xsl:value-of select="'disabled'" />
              </xsl:attribute>
              <a href="#">
                <xsl:value-of select="$next" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <a href="{concat($href, '&amp;start=',($currentpage * $size), '&amp;rows=', $size)}">
                <xsl:value-of select="$next" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </li>
      </ul>
    </div>
  </xsl:template>

  <xsl:template name="solr.PageGen">
    <xsl:param name="i" select="1" />
    <xsl:param name="href" select="concat($proxyBaseURL,$HttpSession,$solrParams)" />
    <xsl:param name="size" />
    <xsl:param name="currentpage" />
    <xsl:param name="totalpage" />
    <xsl:variable name="PageWindowSize">
      <xsl:choose>
        <xsl:when test="$currentpage &gt; 999">
          <xsl:value-of select="4" />
        </xsl:when>
        <xsl:when test="$currentpage &gt; 99">
          <xsl:value-of select="6" />
        </xsl:when>
        <xsl:when test="$currentpage &gt; 9">
          <xsl:value-of select="7" />
        </xsl:when>
        <xsl:when test="$totalpage &gt; 999">
          <xsl:value-of select="8" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="9" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- jumpSize is to determine the pages to be skipped -->
    <xsl:variable name="jumpSize">
      <xsl:choose>
        <!-- current printed page number is smaller than current displayed page -->
        <xsl:when test="$i &lt; $currentpage">
          <xsl:choose>
            <!-- This is to support a bigger PageWindow at the end of page listing and
                to skip a jump of 2
            -->
            <xsl:when
              test="(($totalpage - $PageWindowSize - 1) &lt;= $i) or
                                  (($currentpage - floor(($PageWindowSize -1) div 2) - 1) = 2)">
              <xsl:value-of select="1" />
            </xsl:when>
                    <!-- This is to support a bigger PageWindow at the begin of page listing
                    -->
            <xsl:when test="($totalpage - $currentpage) &lt; $PageWindowSize">
              <xsl:value-of select="($totalpage - $PageWindowSize - 1)" />
            </xsl:when>
            <xsl:when test="(($currentpage - $i) &lt;= floor(($PageWindowSize -1) div 2))">
              <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="($currentpage - floor(($PageWindowSize -1) div 2) - 1)" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$i &gt; $currentpage">
          <xsl:choose>
            <!-- jump only one if your near currentpage,
                or at last page 
                or to support bigger window at beginning
                or to skip a jump of 2
            -->
            <xsl:when
              test="( (($i - $currentpage) &lt; round(($PageWindowSize -1) div 2)) or ($i = $totalpage) or ($currentpage &lt;=$PageWindowSize and $i &lt;= $PageWindowSize) or ($totalpage - $i = 2))">
              <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="($totalpage - $i)" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="1" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="running">
      <xsl:if test="$i &lt;= $totalpage">
        <xsl:text>true</xsl:text>
      </xsl:if>
    </xsl:variable>
    <xsl:if test="$running='true'">
      <li>
        <xsl:if test="$i=$currentpage">
          <xsl:attribute name="class">
            <xsl:value-of select="'active'" />
          </xsl:attribute>
        </xsl:if>
        <!-- XSL.Style parameter in order to view search-results in a list -->
        <a href="{concat($href, '&amp;start=',(($i -1) * $size), '&amp;rows=', $size)}">
          <xsl:value-of select="$i" />
        </a>
      </li>
      <xsl:if test="$jumpSize &gt; 1">
        <li class="disabled">
          <a href="#">...</a>
        </li>
      </xsl:if>
      <xsl:call-template name="solr.PageGen">
        <xsl:with-param name="i" select="$i + $jumpSize" />
        <xsl:with-param name="href" select="$href" />
        <xsl:with-param name="size" select="$size" />
        <xsl:with-param name="currentpage" select="$currentpage" />
        <xsl:with-param name="totalpage" select="$totalpage" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template match="doc" mode="iview">
    <xsl:variable name="mcrid" select="@id" />
    <xsl:variable name="derivates" select="key('derivate', $mcrid)" />
    <xsl:for-each select="$derivates/str[@name='iviewFile']">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivate" select="../str[@name='id']" />
        <xsl:with-param name="fileName" select="." />
      </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="./arr[@name='derivateLink']/str">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivateLink" select="." />
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="doc" mode="hitInFiles">
    <xsl:param name="fileNodeServlet" select="concat($ServletsBaseURL,'MCRFileNodeServlet/')" />
    <xsl:variable name="mcrid" select="@id" />
    <xsl:variable name="files" select="key('file', $mcrid)" />
    <xsl:if test="$files">
      <div class="hitInFile">
        <span class="hitInFileLabel">
          <xsl:value-of select="concat(i18n:translate('results.file'),' ')" />
        </span>
        <ul>
          <xsl:for-each select="$files">
            <li>
            <!-- doc element of 'unmerged' response -->
              <xsl:variable name="derivateId" select="str[@name='derivateID']" />
              <xsl:variable name="filePath" select="str[@name='filePath']" />
              <xsl:variable name="fileName" select="str[@name='fileName']" />
              <xsl:choose>
                <xsl:when test="key('derivate',$mcrid)[str/@name='iviewFile' and str[@name='id']=$derivateId]">
                  <!-- iview support detected generate link to image viewer -->
                  <xsl:variable name="toolTipImg" select="concat($ServletsBaseURL,'MCRThumbnailServlet/',$derivateId,mcrxsl:encodeURIPath($filePath),$HttpSession)" />
                  <a onMouseOver="show('{$toolTipImg}')" onMouseOut="toolTip()"
                    href="{concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$filePath,'&amp;derivate=', $derivateId)}"
                    title="{i18n:translate('metaData.iView')}">
                    <xsl:value-of select="$fileName" />
                  </a>
                </xsl:when>
                <xsl:otherwise>
                  <a href="{concat($fileNodeServlet,$derivateId,mcrxsl:encodeURIPath($filePath),$HttpSession)}">
                    <xsl:value-of select="$fileName" />
                  </a>
                </xsl:otherwise>
              </xsl:choose>
            </li>
          </xsl:for-each>
        </ul>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivate" />
    <xsl:param name="mcrid" />
    <xsl:param name="fileName" />
    <xsl:param name="derivateLink" />

    <xsl:param name="derId">
      <xsl:choose>
        <xsl:when test="$derivate">
          <xsl:value-of select="$derivate" />
        </xsl:when>
        <xsl:when test="$derivateLink">
          <xsl:value-of select="substring-before($derivateLink , '/')" />
        </xsl:when>
      </xsl:choose>
    </xsl:param>
    <xsl:if test="string-length($derId) &gt; 0 and $mcrid">
      <xsl:variable name="pageToDisplay">
        <xsl:choose>
          <xsl:when test="$fileName">
            <xsl:value-of select="$fileName" />
          </xsl:when>
          <xsl:when test="$derivateLink">
            <xsl:value-of select="concat('/', substring-after($derivateLink, '/'))" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iview2.getSupport">
              <xsl:with-param select="$derivate" name="derivID" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:if test="$pageToDisplay != ''">
        <a
          href="{concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$pageToDisplay,'&amp;derivate=', $derId)}"
          title="{i18n:translate('metaData.iView')}">
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="$derId" name="derivate" />
            <xsl:with-param select="$pageToDisplay" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>