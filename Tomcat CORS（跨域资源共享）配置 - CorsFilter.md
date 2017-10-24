# CORS概念
CORS是一个W3C标准，全称是"跨域资源共享"（Cross-origin resource sharing）。
它允许浏览器向跨源服务器，发出XMLHttpRequest请求，从而克服了AJAX只能同源使用的限制。

# Tomcat对CORS的支持-CorsFilter
首先Tomcat默认禁止CORS。
如果需要开启CORS，可以通过配置过滤器CorsFilter来实现。注意：开启此过滤器后默认允许所有域访问，即cors.allowed.origins=*。
```xml
<filter>
	<filter-name>CorsFilter</filter-name>
	<filter-class>org.apache.catalina.filters.CorsFilter</filter-class>
	<!-- 允许跨域访问的域名 -->
	<init-param>
		<param-name>cors.allowed.origins</param-name>
		<param-value>*</param-value>
	</init-param>
	<!-- 请求healder中允许包含的参数 -->
	<init-param>
		<param-name>cors.allowed.headers</param-name>
		<param-value>Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers</param-value>
	</init-param>
	<!-- 允许访问的HTTP动词 -->
  	<init-param>
		<param-name>cors.allowed.methods</param-name>
		<param-value>GET,POST,HEAD,OPTIONS</param-value>
	</init-param>
</filter>
```
如果希望对tomcat下所有项目起作用，则配置到tomcat/conf/web.xml中
如果仅对单个项目起作用，则配置到project/webapp/WEB-INF/web.xml中

该过滤器由tomcat提供，如需调试，可以在pom.xml中增加依赖，虽然作用域是provided，目前没发现什么问题，最好调试完成后去掉此依赖。
```xml
<dependency>
	<groupId>javax.servlet</groupId>
	<artifactId>javax.servlet-api</artifactId>
	<version>3.1.0</version>
	<scope>provided</scope>
</dependency>
```
## CorsFilter 关键代码
### 通过request判断属于何种CORS校验方式
```java
/**
 * Determines the request type.
 *
 * @param request
 */
protected CORSRequestType checkRequestType(final HttpServletRequest request) {
	//默认为无效的CORS校验
    CORSRequestType requestType = CORSRequestType.INVALID_CORS;
    if (request == null) {
        throw new IllegalArgumentException(
                sm.getString("corsFilter.nullRequest"));
    }
    //获取header中的origin
    String originHeader = request.getHeader(REQUEST_HEADER_ORIGIN);
    // Section 6.1.1 and Section 6.2.1
    if (originHeader != null) {
    	//origin为空，不通过
        if (originHeader.isEmpty()) {
            requestType = CORSRequestType.INVALID_CORS;
        } else if (!isValidOrigin(originHeader)) {//origin有特殊字符，或者不是正确的URI格式，不通过
            requestType = CORSRequestType.INVALID_CORS;
        } else if (isLocalOrigin(request, originHeader)) {//如果是本域，origin和request的域名一致，则无需验证！
            return CORSRequestType.NOT_CORS;
        } else {
        	//获取HTTP动词
            String method = request.getMethod();
            if (method != null) {
                if ("OPTIONS".equals(method)) {
                	//获取header中的Access-Control-Request-Method
                    String accessControlRequestMethodHeader =
                            request.getHeader(
                                    REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                    if (accessControlRequestMethodHeader != null &&
                            !accessControlRequestMethodHeader.isEmpty()) { //如果Access-Control-Request-Method不为null和“”，走PRE_FLIGHT
                        requestType = CORSRequestType.PRE_FLIGHT;
                    } else if (accessControlRequestMethodHeader != null &&
                            accessControlRequestMethodHeader.isEmpty()) { //如果Access-Control-Request-Method不为null，但为“”，不通过
                        requestType = CORSRequestType.INVALID_CORS;
                    } else { //如果Access-Control-Request-Method为空，走ACTUAL
                        requestType = CORSRequestType.ACTUAL;
                    }
                } else if ("GET".equals(method) || "HEAD".equals(method)) {//如果是GET或HEAD，走简单校验
                    requestType = CORSRequestType.SIMPLE;
                } else if ("POST".equals(method)) {// 如果是POST，则判断mediaType
                    String mediaType = getMediaType(request.getContentType());
                    if (mediaType != null) {
                    	// 如果是表单提交，附件上传，普通文本，则走简单校验
                        if (SIMPLE_HTTP_REQUEST_CONTENT_TYPE_VALUES
                                .contains(mediaType)) {
                            requestType = CORSRequestType.SIMPLE;
                        } else { // 其他，走ACTUAL
                            requestType = CORSRequestType.ACTUAL;
                        }
                    }
                } else { //其他，走ACTUAL
                    requestType = CORSRequestType.ACTUAL;
                }
            }
        }
    } else { //origin为null，无需校验！
        requestType = CORSRequestType.NOT_CORS;
    }

    return requestType;
}
```
注意：1、如果是本域，origin和request的域名一致，则无需验证！所以本域是不用添加到cors.allowed.origins中的
2、origin为null，无需校验！会导致一些安全问题，具体还不太理解，建议在启用CorsFilter配置好cors.allowed.origins白名单。
### 校验类型枚举
```java
/**
 * Enumerates varies types of CORS requests. Also, provides utility methods
 * to determine the request type.
 */
protected static enum CORSRequestType {
    /**
     * A simple HTTP request, i.e. it shouldn't be pre-flighted.
     * 简单校验
     */
    SIMPLE,
    /**
     * A HTTP request that needs to be pre-flighted.
     * 也是走的简单校验
     */
    ACTUAL,
    /**
     * A pre-flight CORS request, to get meta information, before a
     * non-simple HTTP request is sent.
     */
    PRE_FLIGHT,
    /**
     * Not a CORS request, but a normal request.
     * 无需校验
     */
    NOT_CORS,
    /**
     * An invalid CORS request, i.e. it qualifies to be a CORS request, but
     * fails to be a valid one.
     * 无效的，校验不通过
     */
    INVALID_CORS
}
```
### 简单校验逻辑
```java
/**
 * Handles a CORS request of type {@link CORSRequestType}.SIMPLE.
 *
 * @param request
 *            The {@link HttpServletRequest} object.
 * @param response
 *            The {@link HttpServletResponse} object.
 * @param filterChain
 *            The {@link FilterChain} object.
 * @throws IOException
 * @throws ServletException
 * @see <a href="http://www.w3.org/TR/cors/#resource-requests">Simple
 *      Cross-Origin Request, Actual Request, and Redirects</a>
 */
protected void handleSimpleCORS(final HttpServletRequest request,
        final HttpServletResponse response, final FilterChain filterChain)
        throws IOException, ServletException {
    //再次判断校验类型
    CorsFilter.CORSRequestType requestType = checkRequestType(request);
    if (!(requestType == CorsFilter.CORSRequestType.SIMPLE ||
            requestType == CorsFilter.CORSRequestType.ACTUAL)) {
        throw new IllegalArgumentException(
                sm.getString("corsFilter.wrongType2",
                        CorsFilter.CORSRequestType.SIMPLE,
                        CorsFilter.CORSRequestType.ACTUAL));
    }

    final String origin = request
            .getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);
    final String method = request.getMethod();

    // Section 6.1.2
    //校验origin
    if (!isOriginAllowed(origin)) {
        handleInvalidCORS(request, response, filterChain);
        return;
    }

    //校验HTTP动词
    if (!allowedHttpMethods.contains(method)) {
        handleInvalidCORS(request, response, filterChain);
        return;
    }

    // Section 6.1.3
    // Add a single Access-Control-Allow-Origin header.
    if (anyOriginAllowed && !supportsCredentials) {
        // If resource doesn't support credentials and if any origin is
        // allowed
        // to make CORS request, return header with '*'.
        response.addHeader(
                CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                "*");
    } else {
        // If the resource supports credentials add a single
        // Access-Control-Allow-Origin header, with the value of the Origin
        // header as value.
        response.addHeader(
                CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                origin);
    }

    // Section 6.1.3
    // If the resource supports credentials, add a single
    // Access-Control-Allow-Credentials header with the case-sensitive
    // string "true" as value.
    if (supportsCredentials) {
        response.addHeader(
                CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS,
                "true");
    }

    // Section 6.1.4
    // If the list of exposed headers is not empty add one or more
    // Access-Control-Expose-Headers headers, with as values the header
    // field names given in the list of exposed headers.
    if ((exposedHeaders != null) && (exposedHeaders.size() > 0)) {
        String exposedHeadersString = join(exposedHeaders, ",");
        response.addHeader(
                CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS,
                exposedHeadersString);
    }

    // Forward the request down the filter chain.
    filterChain.doFilter(request, response);
}
```
### origin校验
```java
/**
 * Checks if the Origin is allowed to make a CORS request.
 *
 * @param origin
 *            The Origin.
 * @return <code>true</code> if origin is allowed; <code>false</code>
 *         otherwise.
 */
private boolean isOriginAllowed(final String origin) {
	//是否为*
    if (anyOriginAllowed) {
        return true;
    }

    // If 'Origin' header is a case-sensitive match of any of allowed
    // origins, then return true, else return false.
    // 是否包含在允许范围内
    return allowedOrigins.contains(origin);
}
```
# 遗留
- PRE_FLIGHT 是什么？
- 其他一些参数的作用？
