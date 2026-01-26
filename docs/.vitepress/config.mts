import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    base: '/',
    title: "MockK",
    head: [['link', {rel: 'icon', href: '/mockk-icon.svg'}]],
    description: "mocking library for Kotlin",
    themeConfig: {
        logo: '/mockk-icon.svg',

        // https://vitepress.dev/reference/default-theme-config
        nav: [
            {text: 'Docs', link: '/docs'},
            {text: 'API', link: 'https://jgrnrt.github.io/mockk/api'},
            {text: 'Help', link: 'https://github.com/mockk/mockk/discussions'},
        ],

        sidebar: {
            '/docs/': [
                {
                    text: 'Introduction',
                    items: [
                        {text: 'Get Started', link: '/docs/'},
                        {text: 'Installation', link: '/docs/introduction/install'},
                        {text: 'First Mock', link: '/docs/introduction/first-mock'},
                    ]
                },
                {
                    text: 'Features',
                    items: [
                        {text: 'Core Mocking', link: '/docs/features/core-mocking'},
                        {text: 'Annotations and JUnit', link: '/docs/features/annotations-and-junit'},
                        {text: 'Verification', link: '/docs/features/verification'},
                        {text: 'Coroutines', link: '/docs/features/coroutines'},
                        {text: 'Static and Extension Functions', link: '/docs/features/static-and-extensions'},
                        {text: 'Varargs, Private Calls, and Properties', link: '/docs/features/varargs-private-and-properties'},
                        {text: 'Behavior-Driven Development', link: '/docs/bdd'},
                        {text: 'Restricted Mocking', link: '/docs/restricted-mocking'},
                        {text: 'Advanced', link: '/docs/features/advanced'},
                    ]
                },
                {
                    text: 'Platforms',
                    items: [
                        {text: 'Android Support', link: '/docs/android'},
                    ]
                },
                {
                    text: 'Integrations',
                    items: [
                        {text: 'Spring and Quarkus', link: '/docs/integrations'},
                    ]
                },
                {
                    text: 'Reference',
                    items: [
                        {text: 'Configuration', link: '/docs/configuration'},
                        {text: 'DSL Reference', link: '/docs/dsl-reference'},
                        {text: 'Matcher Extensibility', link: '/docs/matcher-extensibility'},
                    ]
                },
                {
                    text: 'Troubleshooting',
                    items: [
                        {text: 'JDK 16+ Access Exceptions', link: '/docs/jdk16-access-exceptions'},
                        {text: 'Known Issues', link: '/docs/known-issues'},
                    ]
                },
                {
                    text: 'Resources',
                    items: [
                        {text: 'Guides & Articles', link: '/docs/guides-articles'},
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
        },
        footer: {
            message: "Licensed under the <a href=\"https://github.com/mockk/mockk/blob/master/LICENSE\">Apache 2 license</a>.",
            copyright: "© 2026 MockK Authors"
        }
    }
})
