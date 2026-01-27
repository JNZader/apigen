package com.jnzader.apigen.codegen.generator.dx;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generator for GitHub issue and pull request templates.
 *
 * <p>Creates standardized templates in .github/ directory:
 *
 * <ul>
 *   <li>PULL_REQUEST_TEMPLATE.md - PR template with checklist
 *   <li>ISSUE_TEMPLATE/bug_report.md - Bug report template
 *   <li>ISSUE_TEMPLATE/feature_request.md - Feature request template
 * </ul>
 */
public class GitHubTemplatesGenerator {

    /**
     * Generates GitHub templates.
     *
     * @return map with file paths as keys and content as values
     */
    public Map<String, String> generate() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".github/PULL_REQUEST_TEMPLATE.md", generatePrTemplate());
        files.put(".github/ISSUE_TEMPLATE/bug_report.md", generateBugReportTemplate());
        files.put(".github/ISSUE_TEMPLATE/feature_request.md", generateFeatureRequestTemplate());
        files.put(".github/ISSUE_TEMPLATE/config.yml", generateIssueConfig());
        return files;
    }

    private String generatePrTemplate() {
        return """
        ## Summary

        <!-- Brief description of changes (1-3 sentences) -->

        ## Type of Change

        - [ ] Bug fix (non-breaking change fixing an issue)
        - [ ] New feature (non-breaking change adding functionality)
        - [ ] Breaking change (fix or feature causing existing functionality to break)
        - [ ] Refactoring (no functional changes)
        - [ ] Documentation update
        - [ ] CI/CD changes
        - [ ] Dependencies update

        ## Related Issues

        <!-- Link to related issues: Closes #123, Fixes #456 -->

        ## Changes Made

        <!-- Bullet points describing what changed -->

        -\s

        ## Checklist

        - [ ] My code follows the project's code style (`mise run lint` passes)
        - [ ] I have added tests covering my changes
        - [ ] All tests pass locally (`mise run test`)
        - [ ] I have updated documentation as needed
        - [ ] My commits follow [Conventional Commits](https://conventionalcommits.org/)
        - [ ] I have reviewed my own code

        ## Screenshots (if applicable)

        <!-- Add screenshots for UI changes -->

        ## Additional Notes

        <!-- Any additional context or notes for reviewers -->
        """;
    }

    private String generateBugReportTemplate() {
        return """
        ---
        name: Bug Report
        about: Report a bug or unexpected behavior
        title: '[BUG] '
        labels: bug, needs-triage
        assignees: ''
        ---

        ## Description

        <!-- Clear description of the bug -->

        ## Steps to Reproduce

        1.
        2.
        3.

        ## Expected Behavior

        <!-- What you expected to happen -->

        ## Actual Behavior

        <!-- What actually happened -->

        ## Environment

        - **OS:** [e.g., Windows 11, macOS 14, Ubuntu 24.04]
        - **Runtime Version:** [e.g., Java 25, Python 3.12, Node 20]
        - **Project Version:** [e.g., 1.0.0]

        ## Logs / Error Messages

        ```
        <!-- Paste relevant logs or error messages here -->
        ```

        ## Screenshots

        <!-- If applicable, add screenshots -->

        ## Additional Context

        <!-- Any other relevant information -->
        """;
    }

    private String generateFeatureRequestTemplate() {
        return """
        ---
        name: Feature Request
        about: Suggest a new feature or enhancement
        title: '[FEATURE] '
        labels: enhancement, needs-triage
        assignees: ''
        ---

        ## Problem Statement

        <!-- What problem does this feature solve? Is it related to a frustration? -->

        ## Proposed Solution

        <!-- How should this feature work? Be as specific as possible. -->

        ## Alternatives Considered

        <!-- What other approaches have you considered? Why is the proposed solution better? -->

        ## Use Cases

        <!-- Who would benefit from this feature and how? -->

        1.
        2.

        ## Acceptance Criteria

        <!-- How will we know this feature is complete? -->

        - [ ]\s
        - [ ]\s

        ## Additional Context

        <!-- Mockups, examples, links to similar implementations, or other relevant information -->
        """;
    }

    private String generateIssueConfig() {
        return """
        blank_issues_enabled: true
        contact_links:
          - name: Documentation
            url: https://github.com/YOUR_ORG/YOUR_REPO#readme
            about: Check the documentation before opening an issue
          - name: Discussions
            url: https://github.com/YOUR_ORG/YOUR_REPO/discussions
            about: Ask questions and discuss ideas in GitHub Discussions
        """;
    }
}
