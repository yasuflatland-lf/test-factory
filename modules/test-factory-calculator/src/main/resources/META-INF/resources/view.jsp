<%@ include file="/init.jsp" %>

<portlet:resourceURL id="/test-factory/calculate" var="calculateURL" />

<react:component
	module="{Calculator} from test-factory-calculator"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"calculateURL", calculateURL
		).put(
			"namespace", renderResponse.getNamespace()
		).build()
	%>'
/>
