package io.mockk.proxy;

import io.mockk.agent.MockKAgentLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MockKInstrumentationLoader {
    public static MockKAgentLogger log = MockKAgentLogger.NO_OP;

    private static final String PKG = "io.mockk.proxy.";

    private static final String[] BOOTSTRAP_CLASS_NAMES = {
            PKG + "MockKDispatcher",
            PKG + "MockKWeakMap",
            PKG + "MockKWeakMap$StrongKey",
            PKG + "MockKWeakMap$WeakKey",
    };

    public static final MockKInstrumentationLoader LOADER = new MockKInstrumentationLoader();


    private MockKInstrumentationLoader() {
    }

    public boolean loadBootJar(Instrumentation instrumentation) {
        File bootJar = getBootJar();
        if (bootJar == null) {
            return false;
        }

        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(bootJar));
        } catch (IOException e) {
            log.trace(e, "Can't add to bootstrap classpath");
            return false;
        }

        try {
            int i = 0;
            for (String name : BOOTSTRAP_CLASS_NAMES) {
                Class<?> cls = topClassLoader().loadClass(name);
                if (cls.getClassLoader() != null) {
                    log.trace("Classloader is not bootstrap for " + name);
                    return false;
                }
                log.trace("Bootstrap class loaded " + cls.getName());
            }
        } catch (ClassNotFoundException cnfe) {
            log.trace(cnfe, "Can't load class");
            return false;
        }
        return true;
    }

    private ClassLoader topClassLoader() {
        ClassLoader cls = ClassLoader.getSystemClassLoader();
        while (cls.getParent() != null) {
            cls = cls.getParent();
        }
        return cls;
    }

    private File getBootJar() {
        try {
            File boot = File.createTempFile("mockk_boot", ".jar");
            boot.deleteOnExit();

            JarOutputStream out = new JarOutputStream(new FileOutputStream(boot));
            try {
                for (String name : BOOTSTRAP_CLASS_NAMES) {
                    if (!addClass(out, name)) {
                        return null;
                    }
                }
            } finally {
                out.close();
            }
            return boot;
        } catch (IOException ex) {
            log.trace(ex, "Error creating boot jar");
            return null;
        }
    }

    private boolean addClass(JarOutputStream out, String source) throws IOException {
        source = source.replace('.', '/');

        InputStream inputStream = MockKInstrumentationLoader.class.getClassLoader().getResourceAsStream(source + ".clazz");
        if (inputStream == null) {
            inputStream = MockKInstrumentationLoader.class.getClassLoader().getResourceAsStream(source + ".class");
        }
        if (inputStream == null) {
            log.trace(source + " not found");
            return false;
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
        return true;
    }
}
