package com.jnzader.apigen.graphql.http;

import com.jnzader.apigen.graphql.GraphQLContext;
import com.jnzader.apigen.graphql.execution.GraphQLExecutor;
import graphql.ExecutionResult;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP controller for GraphQL endpoint.
 *
 * <p>Exposes the GraphQL API at {@code /graphql} with support for:
 *
 * <ul>
 *   <li>POST requests with JSON body
 *   <li>GET requests for simple queries (optional)
 *   <li>Accept-Language header for i18n
 *   <li>Authentication via Spring Security Principal
 * </ul>
 *
 * <p>This controller is auto-configured when {@code apigen.graphql.http.enabled=true}.
 */
@RestController
@RequestMapping("${apigen.graphql.path:/graphql}")
public class GraphQLController {

    private final GraphQLExecutor executor;

    public GraphQLController(GraphQLExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a GraphQL query via POST.
     *
     * @param request the GraphQL request
     * @param locale the locale from Accept-Language header
     * @param principal the authenticated user (if any)
     * @param httpRequest the HTTP request
     * @return the GraphQL response
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> executePost(
            @RequestBody GraphQLRequest request,
            Locale locale,
            Principal principal,
            HttpServletRequest httpRequest) {

        GraphQLContext context = buildContext(locale, principal, httpRequest);

        ExecutionResult result =
                executor.execute(
                        request.query(), request.operationName(), request.variables(), context);

        return ResponseEntity.ok(result.toSpecification());
    }

    /**
     * Executes a GraphQL query via GET (for simple queries).
     *
     * @param query the GraphQL query
     * @param operationName the operation name (optional)
     * @param variables the variables as JSON string (optional)
     * @param locale the locale from Accept-Language header
     * @param principal the authenticated user (if any)
     * @param httpRequest the HTTP request
     * @return the GraphQL response
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> executeGet(
            @RequestParam String query,
            @RequestParam(required = false) String operationName,
            @RequestParam(required = false) Map<String, Object> variables,
            Locale locale,
            Principal principal,
            HttpServletRequest httpRequest) {

        GraphQLContext context = buildContext(locale, principal, httpRequest);

        ExecutionResult result = executor.execute(query, operationName, variables, context);

        return ResponseEntity.ok(result.toSpecification());
    }

    private GraphQLContext buildContext(
            Locale locale, Principal principal, HttpServletRequest httpRequest) {
        GraphQLContext.Builder builder = GraphQLContext.builder().locale(locale);

        if (principal != null) {
            builder.userId(principal.getName());
        }

        // Add request info to context
        builder.attribute("remoteAddr", httpRequest.getRemoteAddr());
        builder.attribute("userAgent", httpRequest.getHeader("User-Agent"));

        return builder.build();
    }
}
