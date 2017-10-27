package io.mockk.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.unmodifiableList;

class MockKByteCodeTranslationRules {
    public static final MockKByteCodeTranslationRules RULES = new MockKByteCodeTranslationRules();

    private final List<String> ignoredPackages;

    private MockKByteCodeTranslationRules() {
        ignoredPackages = new ArrayList<String>();
        ignoredPackages.add("jdk.internal.");
        ignoredPackages.add("org.junit.");
        ignoredPackages.add("junit.");
        ignoredPackages.add("org.testng.");
        ignoredPackages.add("org.easymock.");
        ignoredPackages.add("org.powermock.");
        ignoredPackages.add("net.sf.cglib.");
        ignoredPackages.add("javassist.");
        ignoredPackages.add("org.hamcrest.");
        ignoredPackages.add("java.");
        ignoredPackages.add("java.accessibility.");
        ignoredPackages.add("java.accessibility.");
        ignoredPackages.add("org.pitest");
        ignoredPackages.add("org.jacoco.agent.rt.");
        ignoredPackages.add("sun.");
    }

    public List<String> getIgnoredPackages() {
        return unmodifiableList(ignoredPackages);
    }

    public boolean isIgnored(String className) {
        for (String prefix : ignoredPackages) {
            if (className.replace('/', '.').startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
