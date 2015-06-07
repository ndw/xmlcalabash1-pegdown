<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:h="http://www.w3.org/1999/xhtml"
                exclude-inline-prefixes="c cx h">
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/pegdown.xpl"/>

<cx:pegdown>
  <p:input port="source">
    <p:data href="../../../README.md"/>
  </p:input>
</cx:pegdown>

<p:choose>
  <p:when test="/h:html">
    <p:identity>
      <p:input port="source">
        <p:inline><c:result>PASS</c:result></p:inline>
      </p:input>
    </p:identity>
  </p:when>
  <p:otherwise>
    <p:error code="FAIL">
      <p:input port="source">
        <p:inline><message>Did not find expected text.</message></p:inline>
      </p:input>
    </p:error>
  </p:otherwise>
</p:choose>

</p:declare-step>
