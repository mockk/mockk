package io.mockk.agent.inline;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MockKHotAgent {
    public static final String DISPATCHER_CLASS_NAME = "io.mockk.agent.inline.MockKDispatcher";

    private static boolean disabled = false;

    static {
        try {
            ByteBuddyAgent.install();

            loadDispatcherAtBootstrapClassLoader();
        } catch (IllegalStateException ex) {
            disabled = true;
        }

    }

    private static void loadDispatcherAtBootstrapClassLoader() {
        File bootJar = getBootJar();
        if (bootJar == null) {
            disabled = true;
            return;
        }

        try {
            ByteBuddyAgent.getInstrumentation()
                    .appendToBootstrapClassLoaderSearch(new JarFile(bootJar));
        } catch (IOException e) {
            disabled = true;
            return;
        }

        try {
            Class<?> cls = getClassLoader()
                    .loadClass(DISPATCHER_CLASS_NAME);
            if (cls.getClassLoader() != null) {
                disabled = true;
            }
        } catch (ClassNotFoundException cnfe) {
            disabled = true;
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader cls = ClassLoader.getSystemClassLoader();
        while (cls.getParent() != null) {
            cls = cls.getParent();
        }
        return cls;
    }

    public static Instrumentation getInstrumentation() {
        if (disabled) {
            return null;
        }
        return ByteBuddyAgent.getInstrumentation();
    }

    private static File getBootJar() {
        try {
            File boot = File.createTempFile("mockk", ".jar");
            boot.deleteOnExit();

            JarOutputStream out = new JarOutputStream(new FileOutputStream(boot));
            try {
                String source = DISPATCHER_CLASS_NAME.replace('.', '/');
                InputStream inputStream = MockKHotAgent.class.getClassLoader().getResourceAsStream(source + ".clazz");
                if (inputStream == null) {
                    inputStream = MockKHotAgent.class.getClassLoader().getResourceAsStream(source + ".class");
                }
                if (inputStream == null) {
                    return null;
                }

                out.putNextEntry(new JarEntry(source + ".class"));
                try {
                    int length;
                    byte[] buffer = new byte[1024];
                    while ((length = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                } finally {
                    inputStream.close();
                }
                out.closeEntry();
            } finally {
                out.close();
            }
            return boot;
        } catch (IOException ex) {
            return null;
        }
    }
}
