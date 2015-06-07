<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="1.0">

<p:declare-step type="cx:pegdown">
   <p:input port="source"/>
   <p:output port="result"/>
   <p:option name="max-parsing-time"/>
   <p:option name="smarts" select="'false'"/>
   <p:option name="quotes" select="'false'"/>
   <p:option name="abbreviations" select="'false'"/>
   <p:option name="hardwraps" select="'false'"/>
   <p:option name="autolinks" select="'false'"/>
   <p:option name="tables" select="'false'"/>
   <p:option name="definitions" select="'false'"/>
   <p:option name="fenced-code-blocks" select="'false'"/>
   <p:option name="wikilinks" select="'false'"/>
   <p:option name="strikethrough" select="'false'"/>
   <p:option name="anchorlinks" select="'false'"/>
   <p:option name="suppress-html-blocks" select="'false'"/>
   <p:option name="suppress-inline-html" select="'false'"/>
   <p:option name="suppress-all-html" select="'false'"/>
   <p:option name="plugins"/>
   <p:option name="link-renderer"/>
   <p:option name="verbatim-serializer"/>
   <p:option name="to-html-serializer-plugins"/>
</p:declare-step>
</p:library>
