package com.jnzader.apigen.codegen.generator.java.jte;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates jte server-side templates with admin dashboard and CRUD views.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>jte layout templates
 *   <li>Admin dashboard with entity management
 *   <li>CRUD views for each entity (list, create, edit, view)
 *   <li>View controllers
 *   <li>Tailwind CSS and Alpine.js integration
 * </ul>
 */
public class JteGenerator {

    private static final String PKG_WEB = "web";

    private final String basePackage;

    public JteGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates all jte template files.
     *
     * @param tables list of entity tables
     * @param generateAdmin generate admin dashboard
     * @param generateCrudViews generate CRUD views for entities
     * @param includeTailwind include Tailwind CSS
     * @param includeAlpine include Alpine.js
     * @param adminPath admin path prefix
     * @return map of file path to content
     */
    public Map<String, String> generate(
            List<SqlTable> tables,
            boolean generateAdmin,
            boolean generateCrudViews,
            boolean includeTailwind,
            boolean includeAlpine,
            String adminPath) {

        Map<String, String> files = new LinkedHashMap<>();
        String basePath = "src/main/java/" + basePackage.replace('.', '/');

        // Generate layout template
        files.put(
                "src/main/jte/layout/page.jte",
                generateLayoutTemplate(includeTailwind, includeAlpine));

        // Generate components
        files.put("src/main/jte/components/nav.jte", generateNavComponent(tables, adminPath));
        files.put(
                "src/main/jte/components/sidebar.jte", generateSidebarComponent(tables, adminPath));
        files.put("src/main/jte/components/alert.jte", generateAlertComponent());
        files.put("src/main/jte/components/pagination.jte", generatePaginationComponent());

        // Generate home page
        files.put("src/main/jte/pages/index.jte", generateIndexPage());

        // Generate HomeController
        files.put(basePath + "/" + PKG_WEB + "/HomeController.java", generateHomeController());

        if (generateAdmin) {
            // Generate admin dashboard
            files.put("src/main/jte/admin/dashboard.jte", generateAdminDashboard(tables));

            // Generate AdminController
            files.put(
                    basePath + "/" + PKG_WEB + "/AdminController.java",
                    generateAdminController(adminPath));
        }

        if (generateCrudViews) {
            for (SqlTable table : tables) {
                String entityName = table.getEntityName();
                String moduleName = table.getModuleName();
                String entityLower = entityName.toLowerCase();

                // Generate CRUD templates
                files.put(
                        "src/main/jte/admin/" + entityLower + "/list.jte",
                        generateListTemplate(table));
                files.put(
                        "src/main/jte/admin/" + entityLower + "/form.jte",
                        generateFormTemplate(table));
                files.put(
                        "src/main/jte/admin/" + entityLower + "/view.jte",
                        generateViewTemplate(table));

                // Generate Entity View Controller
                files.put(
                        basePath + "/" + PKG_WEB + "/" + entityName + "ViewController.java",
                        generateEntityViewController(table, adminPath));
            }
        }

        // Generate jte configuration
        files.put("src/main/resources/application-jte.yml", generateJteConfig());

        return files;
    }

    private String generateLayoutTemplate(boolean tailwind, boolean alpine) {
        String tailwindCdn =
                tailwind ? "<script src=\"https://cdn.tailwindcss.com\"></script>" : "";
        String alpineCdn =
                alpine
                        ? "<script defer"
                              + " src=\"https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js\"></script>"
                        : "";

        return """
        @param String title = "Application"
        @param gg.jte.Content content
        @param gg.jte.Content scripts = null

        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${title}</title>
            %s
            %s
            <style>
                [x-cloak] { display: none !important; }
            </style>
        </head>
        <body class="min-h-screen bg-gray-100">
            @template.components.nav()

            <div class="flex">
                @template.components.sidebar()

                <main class="flex-1 p-6">
                    ${content}
                </main>
            </div>

            @if(scripts != null)
                ${scripts}
            @endif
        </body>
        </html>
        """
                .formatted(tailwindCdn, alpineCdn);
    }

    private String generateNavComponent(List<SqlTable> tables, String adminPath) {
        return """
        <nav class="bg-white shadow-sm border-b border-gray-200">
            <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div class="flex justify-between h-16">
                    <div class="flex items-center">
                        <a href="/" class="text-xl font-bold text-gray-800">
                            Application
                        </a>
                    </div>
                    <div class="flex items-center space-x-4">
                        <a href="%s" class="text-gray-600 hover:text-gray-900">
                            Admin
                        </a>
                    </div>
                </div>
            </div>
        </nav>
        """
                .formatted(adminPath);
    }

    private String generateSidebarComponent(List<SqlTable> tables, String adminPath) {
        StringBuilder links = new StringBuilder();
        for (SqlTable table : tables) {
            String entityName = table.getEntityName();
            String entityLower = entityName.toLowerCase();
            links.append(
                    """
                        <a href="%s/%s"
                           class="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
                            <span>%s</span>
                        </a>
                    """
                            .formatted(adminPath, entityLower, entityName + "s"));
        }

        return """
        <aside class="w-64 bg-white border-r border-gray-200 min-h-screen">
            <div class="p-4">
                <h2 class="text-lg font-semibold text-gray-800 mb-4">Menu</h2>
                <nav class="space-y-1">
                    <a href="/"
                       class="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
                        <span>Home</span>
                    </a>
        %s
                </nav>
            </div>
        </aside>
        """
                .formatted(links);
    }

    private String generateAlertComponent() {
        return """
        @param String type = "info"
        @param String message

        !{var bgColor = switch(type) {
            case "success" -> "bg-green-100 border-green-500 text-green-700";
            case "error" -> "bg-red-100 border-red-500 text-red-700";
            case "warning" -> "bg-yellow-100 border-yellow-500 text-yellow-700";
            default -> "bg-blue-100 border-blue-500 text-blue-700";
        };}

        <div class="border-l-4 p-4 ${bgColor}" role="alert" x-data="{ show: true }" x-show="show">
            <div class="flex justify-between">
                <p>${message}</p>
                <button @click="show = false" class="font-bold">&times;</button>
            </div>
        </div>
        """;
    }

    private String generatePaginationComponent() {
        return """
        @param int currentPage
        @param int totalPages
        @param String baseUrl

        @if(totalPages > 1)
        <nav class="flex justify-center mt-6">
            <ul class="flex space-x-2">
                @if(currentPage > 0)
                <li>
                    <a href="${baseUrl}?page=${currentPage - 1}"
                       class="px-3 py-2 bg-white border rounded hover:bg-gray-100">
                        Previous
                    </a>
                </li>
                @endif

                @for(int i = 0; i < totalPages; i++)
                    @if(i == currentPage)
                    <li>
                        <span class="px-3 py-2 bg-blue-500 text-white border rounded">
                            ${i + 1}
                        </span>
                    </li>
                    @else
                    <li>
                        <a href="${baseUrl}?page=${i}"
                           class="px-3 py-2 bg-white border rounded hover:bg-gray-100">
                            ${i + 1}
                        </a>
                    </li>
                    @endif
                @endfor

                @if(currentPage < totalPages - 1)
                <li>
                    <a href="${baseUrl}?page=${currentPage + 1}"
                       class="px-3 py-2 bg-white border rounded hover:bg-gray-100">
                        Next
                    </a>
                </li>
                @endif
            </ul>
        </nav>
        @endif
        """;
    }

    private String generateIndexPage() {
        return """
        @template.layout.page(title = "Home", content = @`
            <div class="max-w-4xl mx-auto">
                <h1 class="text-3xl font-bold text-gray-800 mb-6">Welcome</h1>
                <p class="text-gray-600 mb-4">
                    This is your application home page. Use the sidebar to navigate.
                </p>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-8">
                    <div class="bg-white rounded-lg shadow p-6">
                        <h3 class="font-semibold text-lg mb-2">Quick Start</h3>
                        <p class="text-gray-600">
                            Navigate to the admin section to manage your entities.
                        </p>
                    </div>
                    <div class="bg-white rounded-lg shadow p-6">
                        <h3 class="font-semibold text-lg mb-2">API Documentation</h3>
                        <p class="text-gray-600">
                            <a href="/swagger-ui.html" class="text-blue-500 hover:underline">
                                View API docs
                            </a>
                        </p>
                    </div>
                </div>
            </div>
        `)
        """;
    }

    private String generateHomeController() {
        return """
        package %s.web;

        import org.springframework.stereotype.Controller;
        import org.springframework.web.bind.annotation.GetMapping;

        /**
         * Controller for home page.
         */
        @Controller
        public class HomeController {

            @GetMapping("/")
            public String index() {
                return "pages/index";
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateAdminDashboard(List<SqlTable> tables) {
        StringBuilder stats = new StringBuilder();
        for (SqlTable table : tables) {
            String entityName = table.getEntityName();
            stats.append(
                    """
                            <div class="bg-white rounded-lg shadow p-6">
                                <h3 class="text-gray-500 text-sm font-medium">%s</h3>
                                <p class="text-3xl font-bold text-gray-800 mt-2">
                                    ${%sCount}
                                </p>
                                <a href="%s" class="text-blue-500 text-sm hover:underline">
                                    View all &rarr;
                                </a>
                            </div>
                    """
                            .formatted(
                                    entityName + "s",
                                    entityName.toLowerCase(),
                                    entityName.toLowerCase()));
        }

        return """
        @param long userCount = 0
        %s

        @template.layout.page(title = "Admin Dashboard", content = @`
            <div class="max-w-7xl mx-auto">
                <h1 class="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>

                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        %s
                </div>

                <div class="mt-8 bg-white rounded-lg shadow p-6">
                    <h2 class="text-lg font-semibold mb-4">Quick Actions</h2>
                    <div class="flex space-x-4">
                        <a href="/swagger-ui.html"
                           class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
                            API Docs
                        </a>
                        <a href="/actuator/health"
                           class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600">
                            Health Check
                        </a>
                    </div>
                </div>
            </div>
        `)
        """
                .formatted(
                        tables.stream()
                                .map(
                                        t ->
                                                "@param long "
                                                        + t.getEntityName().toLowerCase()
                                                        + "Count = 0")
                                .reduce("", (a, b) -> a + "\n" + b),
                        stats);
    }

    private String generateAdminController(String adminPath) {
        return """
        package %s.web;

        import org.springframework.stereotype.Controller;
        import org.springframework.ui.Model;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;

        /**
         * Controller for admin dashboard.
         */
        @Controller
        @RequestMapping("%s")
        public class AdminController {

            @GetMapping
            public String dashboard(Model model) {
                // TODO: Add counts from services
                return "admin/dashboard";
            }
        }
        """
                .formatted(basePackage, adminPath);
    }

    private String generateListTemplate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityLower = entityName.toLowerCase();
        String entityVar = table.getEntityVariableName();

        StringBuilder columns = new StringBuilder();
        StringBuilder rows = new StringBuilder();

        // ID column
        columns.append("<th class=\"px-4 py-3 text-left\">ID</th>\n");
        rows.append("<td class=\"px-4 py-3\">${").append(entityVar).append(".id()}</td>\n");

        // Business columns
        for (var col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            columns.append("<th class=\"px-4 py-3 text-left\">")
                    .append(fieldName)
                    .append("</th>\n");
            rows.append("<td class=\"px-4 py-3\">${")
                    .append(entityVar)
                    .append(".")
                    .append(fieldName)
                    .append("()}</td>\n");
        }

        return """
        @import java.util.List
        @param List<Object> items
        @param int currentPage = 0
        @param int totalPages = 1
        @param String message = null

        @template.layout.page(title = "%s List", content = @`
            <div class="max-w-7xl mx-auto">
                <div class="flex justify-between items-center mb-6">
                    <h1 class="text-2xl font-bold text-gray-800">%s</h1>
                    <a href="/%s/new"
                       class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
                        Add New
                    </a>
                </div>

                @if(message != null)
                    @template.components.alert(type = "success", message = message)
                @endif

                <div class="bg-white rounded-lg shadow overflow-hidden">
                    <table class="min-w-full">
                        <thead class="bg-gray-50">
                            <tr>
                                %s
                                <th class="px-4 py-3 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-200">
                            @for(var %s : items)
                            <tr>
                                %s
                                <td class="px-4 py-3 text-right">
                                    <a href="/%s/${%s.id()}"
                                       class="text-blue-500 hover:underline">View</a>
                                    <a href="/%s/${%s.id()}/edit"
                                       class="text-green-500 hover:underline ml-2">Edit</a>
                                    <button @click="confirm('Delete?') && $el.closest('form').submit()"
                                            class="text-red-500 hover:underline ml-2">
                                        Delete
                                    </button>
                                </td>
                            </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>

                @template.components.pagination(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    baseUrl = "/%s"
                )
            </div>
        `)
        """
                .formatted(
                        entityName,
                        entityName + "s",
                        entityLower,
                        columns,
                        entityVar,
                        rows,
                        entityLower,
                        entityVar,
                        entityLower,
                        entityVar,
                        entityLower);
    }

    private String generateFormTemplate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityLower = entityName.toLowerCase();

        StringBuilder fields = new StringBuilder();
        for (var col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            String inputType = getInputType(col.getJavaType());
            boolean required = !col.isNullable();

            fields.append(
                    """
                            <div>
                                <label for="%s" class="block text-sm font-medium text-gray-700 mb-1">
                                    %s%s
                                </label>
                                <input type="%s"
                                       id="%s"
                                       name="%s"
                                       value="${item != null ? item.%s() : ''}"
                                       class="w-full px-3 py-2 border rounded-lg focus:ring-blue-500 focus:border-blue-500"
                                       %s>
                            </div>
                    """
                            .formatted(
                                    fieldName,
                                    fieldName,
                                    required ? " *" : "",
                                    inputType,
                                    fieldName,
                                    fieldName,
                                    fieldName,
                                    required ? "required" : ""));
        }

        return """
        @param Object item = null
        @param String error = null

        @template.layout.page(title = "${item != null ? 'Edit' : 'New'} %s", content = @`
            <div class="max-w-2xl mx-auto">
                <h1 class="text-2xl font-bold text-gray-800 mb-6">
                    ${item != null ? "Edit" : "New"} %s
                </h1>

                @if(error != null)
                    @template.components.alert(type = "error", message = error)
                @endif

                <form action="/%s${item != null ? '/' + item.id() : ''}"
                      method="post"
                      class="bg-white rounded-lg shadow p-6 space-y-4">

                    @if(item != null)
                    <input type="hidden" name="_method" value="PUT">
                    @endif

                    %s

                    <div class="flex space-x-4 pt-4">
                        <button type="submit"
                                class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
                            ${item != null ? "Update" : "Create"}
                        </button>
                        <a href="/%s"
                           class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        `)
        """
                .formatted(entityName, entityName, entityLower, fields, entityLower);
    }

    private String generateViewTemplate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityLower = entityName.toLowerCase();

        StringBuilder fields = new StringBuilder();
        fields.append(
                """
                            <div class="grid grid-cols-2 gap-4 mb-4">
                                <div class="font-medium text-gray-700">ID</div>
                                <div>${item.id()}</div>
                            </div>
                """);

        for (var col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            fields.append(
                    """
                            <div class="grid grid-cols-2 gap-4 mb-4">
                                <div class="font-medium text-gray-700">%s</div>
                                <div>${item.%s()}</div>
                            </div>
                    """
                            .formatted(fieldName, fieldName));
        }

        return """
        @param Object item

        @template.layout.page(title = "%s Details", content = @`
            <div class="max-w-2xl mx-auto">
                <div class="flex justify-between items-center mb-6">
                    <h1 class="text-2xl font-bold text-gray-800">%s Details</h1>
                    <div class="space-x-2">
                        <a href="/%s/${item.id()}/edit"
                           class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600">
                            Edit
                        </a>
                        <a href="/%s"
                           class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">
                            Back
                        </a>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow p-6">
                    %s
                </div>
            </div>
        `)
        """
                .formatted(entityName, entityName, entityLower, entityLower, fields);
    }

    private String generateEntityViewController(SqlTable table, String adminPath) {
        String entityName = table.getEntityName();
        String entityLower = entityName.toLowerCase();

        return """
        package %s.web;

        import org.springframework.stereotype.Controller;
        import org.springframework.ui.Model;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestParam;

        /**
         * View controller for %s entity.
         */
        @Controller
        @RequestMapping("%s/%s")
        public class %sViewController {

            // TODO: Inject %sService

            @GetMapping
            public String list(
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(required = false) String message,
                    Model model) {
                // TODO: Fetch from service
                // model.addAttribute("items", service.findAll(page));
                // model.addAttribute("currentPage", page);
                // model.addAttribute("totalPages", totalPages);
                model.addAttribute("message", message);
                return "admin/%s/list";
            }

            @GetMapping("/{id}")
            public String view(@PathVariable Long id, Model model) {
                // TODO: Fetch from service
                // model.addAttribute("item", service.findById(id));
                return "admin/%s/view";
            }

            @GetMapping("/new")
            public String newForm(Model model) {
                return "admin/%s/form";
            }

            @GetMapping("/{id}/edit")
            public String editForm(@PathVariable Long id, Model model) {
                // TODO: Fetch from service
                // model.addAttribute("item", service.findById(id));
                return "admin/%s/form";
            }
        }
        """
                .formatted(
                        basePackage,
                        entityName,
                        adminPath,
                        entityLower,
                        entityName,
                        entityName,
                        entityLower,
                        entityLower,
                        entityLower,
                        entityLower);
    }

    private String generateJteConfig() {
        return """
        # jte Template Configuration
        gg:
          jte:
            development-mode: true
            template-suffix: .jte
            template-location: src/main/jte

        # In production, set development-mode to false and precompile templates
        """;
    }

    private String getInputType(String javaType) {
        return switch (javaType) {
            case "Integer", "Long", "Short", "int", "long" -> "number";
            case "Float", "Double", "BigDecimal" -> "number";
            case "Boolean", "boolean" -> "checkbox";
            case "LocalDate" -> "date";
            case "LocalDateTime" -> "datetime-local";
            case "LocalTime" -> "time";
            default -> "text";
        };
    }
}
