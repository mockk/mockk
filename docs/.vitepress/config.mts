import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    base: '/mockk', // to be deleted, only for fork testing
    head: [['link', {rel: 'icon', href: '/mockk-icon.svg'}]],
    title: "MockK",
    description: "mocking library for Kotlin",
    themeConfig: {
        logo: '/mockk-icon.svg',

        // https://vitepress.dev/reference/default-theme-config
        nav: [
            {text: 'Docs', link: '/docs/'},
            {text: 'Help', link: 'https://github.com/mockk/mockk/discussions'},
        ],

        sidebar: {
            // This sidebar gets displayed when a user
            // is on `docs` directory.
            '/docs/': [
                {
                    items: [
                        // TODO: maybe create second sidebar group, couldn't think of good grouping yet
                        {text: 'Get Started', link: '/docs/'},
                        {text: 'Behavior-Driven Development', link: '/docs/bdd'},
                        {text: 'Android', link: '/docs/android'},
                        {text: 'Features', link: '/docs/features'},
                        {text: 'Matcher Extensibility', link: '/docs/matcher-extensibility'},
                        {text: 'Guides & Articles', link: '/docs/guides-articles'},
                        {text: 'Settings File', link: '/docs/settings-file'},
                        {text: 'Restricted Mocking', link: '/docs/restricted-mocking'},
                        {text: 'API Reference', link: '/docs/api-reference'},
                        {text: 'JDK 16 Access Exceptions', link: '/docs/jdk16-access-exceptions'},
                        {text: 'Known Issues', link: '/docs/known-issues'},
                    ]
                }
            ],
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/mockk/mockk'}
        ],
        lastUpdated: {
            text: 'Updated at',
            formatOptions: {
                dateStyle: 'full',
                timeStyle: 'medium'
            }
        },
        editLink: {
            pattern: 'https://github.com/mockk/mockk/edit/master/docs/:path'
        },
        search: {
            provider: 'local'
        }
    }
})
