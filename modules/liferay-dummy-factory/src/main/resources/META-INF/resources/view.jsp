<%@ include file="/init.jsp" %>

<portlet:resourceURL id="/ldf/org" var="orgResourceURL" />
<portlet:resourceURL id="/ldf/page" var="pageResourceURL" />
<portlet:resourceURL id="/ldf/role" var="roleResourceURL" />
<portlet:resourceURL id="/ldf/site" var="siteResourceURL" />
<portlet:resourceURL id="/ldf/user" var="userResourceURL" />
<portlet:resourceURL id="/ldf/wcm" var="wcmResourceURL" />
<portlet:resourceURL id="/ldf/doc" var="docResourceURL" />
<portlet:resourceURL id="/ldf/doc/upload" var="docUploadResourceURL" />
<portlet:resourceURL id="/ldf/vocabulary" var="vocabularyURL" />
<portlet:resourceURL id="/ldf/category" var="categoryURL" />
<portlet:resourceURL id="/ldf/data" var="dataResourceURL" />
<portlet:resourceURL id="/ldf/progress" var="progressResourceURL" />

<react:component
	module="{App} from liferay-dummy-factory"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"actionResourceURLs", HashMapBuilder.<String, Object>put(
				"/ldf/org", orgResourceURL
			).put(
				"/ldf/page", pageResourceURL
			).put(
				"/ldf/role", roleResourceURL
			).put(
				"/ldf/site", siteResourceURL
			).put(
				"/ldf/user", userResourceURL
			).put(
				"/ldf/wcm", wcmResourceURL
			).put(
				"/ldf/doc", docResourceURL
			).put(
				"/ldf/doc/upload", docUploadResourceURL
			).put(
				"/ldf/vocabulary", vocabularyURL
			).put(
				"/ldf/category", categoryURL
			).build()
		).put(
			"dataResourceURL", dataResourceURL
		).put(
			"namespace", renderResponse.getNamespace()
		).put(
			"progressResourceURL", progressResourceURL
		).build()
	%>'
/>
