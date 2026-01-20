# APiGen Documentation Website

This directory contains the Docusaurus-powered documentation site for APiGen.

## Prerequisites

- Node.js 20+
- npm or yarn

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build

# Serve production build
npm run serve
```

## API Documentation

The site uses two complementary plugins for API documentation:

### 1. docusaurus-plugin-openapi-docs (Palo Alto Networks)

Generates MDX pages from OpenAPI specs that integrate with the documentation.

```bash
# Generate API docs from OpenAPI spec
npm run gen-api-docs

# Clean generated API docs
npm run clean-api-docs
```

### 2. Redocusaurus

Provides a beautiful three-panel API reference at `/api/reference`.

## Project Structure

```
website/
├── docs/                    # Documentation markdown files
│   ├── api/                 # Generated API docs (MDX)
│   └── intro.md            # Main intro page
├── openapi/                 # OpenAPI specifications
│   └── apigen-openapi.yaml # Main API spec
├── src/
│   ├── css/                # Custom styles
│   └── pages/              # Custom pages
├── static/
│   └── img/                # Static images
├── docusaurus.config.js    # Main configuration
├── sidebars.js             # Sidebar configuration
└── package.json            # Dependencies
```

## Updating the OpenAPI Spec

1. Export the spec from the running server:
   ```bash
   curl http://localhost:8080/v3/api-docs.yaml > openapi/apigen-openapi.yaml
   ```

2. Regenerate the docs:
   ```bash
   npm run gen-api-docs
   ```

## Deployment

### GitHub Pages (Automatic)

The documentation is automatically deployed to GitHub Pages when changes are pushed to the `main` branch.

**URL:** https://jnzader.github.io/apigen/

**Setup Required (one-time):**
1. Go to repository **Settings** > **Pages**
2. Under "Build and deployment", select **Source: GitHub Actions**
3. Push changes to `website/` folder on `main` branch

The workflow at `.github/workflows/docs.yml` handles:
- Building the Docusaurus site
- Generating OpenAPI documentation
- Deploying to GitHub Pages

### Manual Deployment

```bash
# Using Docusaurus deploy command
USE_SSH=true npm run deploy
```

### Local Build

```bash
npm run build
# Output in ./build directory
npm run serve  # Preview production build
```

## i18n

The site supports English and Spanish. To add a new locale:

1. Add the locale to `docusaurus.config.js`
2. Run `npm run write-translations -- --locale <locale>`
3. Translate the JSON files in `i18n/<locale>/`

## Links

- [Docusaurus Documentation](https://docusaurus.io/docs)
- [docusaurus-plugin-openapi-docs](https://github.com/PaloAltoNetworks/docusaurus-openapi-docs)
- [Redocusaurus](https://github.com/rohit-gohri/redocusaurus)
