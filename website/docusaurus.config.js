// @ts-check
import { themes as prismThemes } from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'APiGen',
  tagline: 'Spring Boot REST API Library - Production-ready CRUD in minutes',
  favicon: 'img/favicon.ico',

  url: 'https://jnzader.github.io',
  baseUrl: '/apigen/',

  organizationName: 'jnzader',
  projectName: 'apigen',

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'es'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/jnzader/apigen/tree/main/website/',
        },
        blog: false, // Disable blog
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
    // Redocusaurus for beautiful API reference at /api/reference
    [
      'redocusaurus',
      {
        specs: [
          {
            id: 'apigen-api',
            spec: 'openapi/apigen-openapi.yaml',
            route: '/api/reference',
          },
        ],
        theme: {
          primaryColor: '#1976d2',
          options: {
            scrollYOffset: 60,
            hideDownloadButton: false,
            expandResponses: '200,201',
          },
        },
      },
    ],
  ],

  plugins: [],

  themes: [],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/apigen-social-card.png',
      navbar: {
        title: 'APiGen',
        logo: {
          alt: 'APiGen Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Docs',
          },
          {
            to: '/api/reference',
            label: 'API Reference',
            position: 'left',
          },
          {
            href: 'https://github.com/jnzader/apigen',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Getting Started',
                to: '/docs/intro',
              },
              {
                label: 'Features',
                to: '/docs/features',
              },
              {
                label: 'API Reference',
                to: '/api/reference',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'GitHub Discussions',
                href: 'https://github.com/jnzader/apigen/discussions',
              },
              {
                label: 'Issues',
                href: 'https://github.com/jnzader/apigen/issues',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'GitHub',
                href: 'https://github.com/jnzader/apigen',
              },
              {
                label: 'Changelog',
                href: 'https://github.com/jnzader/apigen/blob/main/CHANGELOG.md',
              },
            ],
          },
        ],
        copyright: `Copyright ${new Date().getFullYear()} APiGen. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['java', 'groovy', 'kotlin', 'bash', 'yaml', 'json'],
      },
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
    }),
};

export default config;
