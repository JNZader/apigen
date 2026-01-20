// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/quick-start',
        'getting-started/installation',
        'getting-started/configuration',
      ],
    },
    {
      type: 'category',
      label: 'Core Concepts',
      items: [
        'core/entities',
        'core/repositories',
        'core/services',
        'core/controllers',
        'core/dtos-mappers',
      ],
    },
    {
      type: 'category',
      label: 'Features',
      items: [
        'features/crud-operations',
        'features/filtering',
        'features/pagination',
        'features/hateoas',
        'features/soft-delete',
        'features/auditing',
        'features/etag-caching',
        'features/domain-events',
      ],
    },
    {
      type: 'category',
      label: 'Security',
      items: [
        'security/jwt-authentication',
        'security/rate-limiting',
        'security/pkce-oauth2',
        'security/headers',
      ],
    },
    {
      type: 'category',
      label: 'Advanced',
      items: [
        'advanced/multi-tenancy',
        'advanced/event-sourcing',
        'advanced/api-versioning',
        'advanced/webhooks',
        'advanced/bulk-operations',
        'advanced/i18n',
      ],
    },
    {
      type: 'category',
      label: 'Modules',
      items: [
        'modules/core',
        'modules/security',
        'modules/codegen',
        'modules/graphql',
        'modules/grpc',
        'modules/gateway',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      link: {
        type: 'generated-index',
        title: 'API Reference',
        description: 'APiGen REST API Documentation',
        slug: '/category/api-reference',
      },
      items: require('./docs/api/sidebar.js'),
    },
    {
      type: 'category',
      label: 'Deployment',
      items: [
        'deployment/docker',
        'deployment/native-image',
        'deployment/jitpack',
      ],
    },
    'roadmap',
    'contributing',
  ],
};

export default sidebars;
