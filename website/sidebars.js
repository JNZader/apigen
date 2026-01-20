// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Deployment',
      items: [
        'deployment/jitpack',
      ],
    },
  ],
};

export default sidebars;
