package io.mockk.agent;

import io.mockk.agent.inline.MockKHotAgent;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

class MockKByteCodeTranslationRules {
    public static final MockKByteCodeTranslationRules RULES = new MockKByteCodeTranslationRules();

    private final List<String> ignoredPackagesAndClasses;

    private MockKByteCodeTranslationRules() {
        ignoredPackagesAndClasses = new ArrayList<String>();
        ignoredPackagesAndClasses.add("jdk.internal.");
        ignoredPackagesAndClasses.add("org.junit.");
        ignoredPackagesAndClasses.add("junit.");
        ignoredPackagesAndClasses.add("org.testng.");
        ignoredPackagesAndClasses.add("org.easymock.");
        ignoredPackagesAndClasses.add("org.powermock.");
        ignoredPackagesAndClasses.add("net.sf.cglib.");
        ignoredPackagesAndClasses.add("javassist.");
        ignoredPackagesAndClasses.add("org.hamcrest.");
        ignoredPackagesAndClasses.add("java.");
        ignoredPackagesAndClasses.add("java.accessibility.");
        ignoredPackagesAndClasses.add("java.accessibility.");
        ignoredPackagesAndClasses.add("org.pitest");
        ignoredPackagesAndClasses.add("org.jacoco.agent.rt.");
        ignoredPackagesAndClasses.add("sun.");
        ignoredPackagesAndClasses.add(MockKHotAgent.DISPATCHER_CLASS_NAME);
    }

    public List<String> getIgnoredPackagesAndClasses() {
        return unmodifiableList(ignoredPackagesAndClasses);
    }

    public boolean isIgnored(String className) {
        for (String prefix : ignoredPackagesAndClasses) {
            if (className.replace('/', '.').startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
