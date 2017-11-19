package io.mockk.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class MockKInstrumentationLoader {
    private static final String PKG = "io.mockk.proxy.";

    private static final String[] BOOTSTRAP_CLASS_NAMES = {
            PKG + "MockKDispatcher"
    };

    public static final MockKInstrumentationLoader LOADER = new MockKInstrumentationLoader();

    public final Class<?>[] bootstrapClasses;

    public Class<?> dispatcher() {
        return bootstrapClasses[0];
    }


    private MockKInstrumentationLoader() {
        bootstrapClasses = new Class[BOOTSTRAP_CLASS_NAMES.length];
    }

    public boolean loadBootJar(Instrumentation instrumentation) {
        File bootJar = getBootJar();
        if (bootJar == null) {
            return false;
        }

        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(bootJar));
        } catch (IOException e) {
            return false;
        }

        try {
            int i = 0;
            for (String name : BOOTSTRAP_CLASS_NAMES) {
                Class<?> cls = topClassLoader().loadClass(name);
                if (cls.getClassLoader() != null) {
                    return false;
                }
                bootstrapClasses[i] = cls;
            }
        } catch (ClassNotFoundException cnfe) {
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
