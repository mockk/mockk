package io.mockk.junit;

import io.mockk.agent.MockKAgent;
import io.mockk.agent.MockKClassLoader;
import javassist.*;
import org.junit.platform.launcher.TestExecutionListener;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ExecutionInvokerPatcher implements TestExecutionListener {
    public static final String INVOKER_PATCHER_CLASS = ExecutionInvokerPatcher.class.getName();
    public static final String EXTENSION_REGISTRY_CLASS = "org.junit.jupiter.engine.extension.ExtensionRegistry";
    public static final String EXTENSION_CONTEXT_CLASS = "org.junit.jupiter.api.extension.ExtensionContext";
    public static final String CONSTRUCTOR_CLASS = "java.lang.reflect.Constructor";
    public static final String METHOD_CLASS = "java.lang.reflect.Method";
    private static Unsafe unsafe;

    private static final ClassLoader CLASS_LOADER = MockKClassLoader.newClassLoader(ExecutionInvokerPatcher.class.getClassLoader());

    static {
        if (!MockKAgent.running) {
            try {
                patch();
            } catch (Throwable ex) {
                System.err.println("WARNING: ExecutionInvokerPatcher failed: " + ex);
            }
        }
    }

    private static void patch() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);

        patchExecutableInvokers(new String[]{
                "org.junit.jupiter.engine.descriptor.ClassTestDescriptor",
                "org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor"
        });
        Thread.currentThread().setContextClassLoader(CLASS_LOADER);
    }


    private static void updateFinalStatic(Class<?> cls, String field, Object value) throws NoSuchFieldException {
        final Field fieldToUpdate = cls.getDeclaredField(field);
        final Object base = unsafe.staticFieldBase(fieldToUpdate);
        final long offset = unsafe.staticFieldOffset(fieldToUpdate);
        unsafe.putObject(base, offset, value);
    }

    private static void patchExecutableInvokers(String[] descriptorClasses) throws Exception {
        for (String descriptorClassName : descriptorClasses) {
            patchExecutableInvoker(Class.forName(descriptorClassName));
        }
    }

    private static void patchExecutableInvoker(Class<?> classToPatch) throws InstantiationException, IllegalAccessException, CannotCompileException, NotFoundException, NoSuchFieldException {
        Object newExecutableInvoker = buildNewExecutableInvokerClass().newInstance();
        updateFinalStatic(classToPatch, "executableInvoker", newExecutableInvoker);
    }

    private static Class<?> buildNewExecutableInvokerClass() throws CannotCompileException, NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        Class<?> hackedEICls;
        try {
            hackedEICls = Class.forName("io.mockk.junit.HackedExecutableInvoker");
        } catch (ClassNotFoundException ex) {
            CtClass hackedEI = pool.makeClass("io.mockk.junit.HackedExecutableInvoker");

            hackedEI.setSuperclass(pool.get("org.junit.jupiter.engine.execution.ExecutableInvoker"));

            hackedEI.addMethod(CtNewMethod.make("public Object invoke(" +
                    CONSTRUCTOR_CLASS + " constructor, " +
                    EXTENSION_CONTEXT_CLASS + " extensionContext,\n" +
                    EXTENSION_REGISTRY_CLASS + " extensionRegistry) {\n" +
                    "\n" +
                    "constructor = " + INVOKER_PATCHER_CLASS + ".getConstructor(constructor);\n" +
                    "return super.invoke(constructor, extensionContext, extensionRegistry);\n" +
                    "}\n", hackedEI));

            hackedEI.addMethod(CtNewMethod.make("public Object invoke(" +
                    METHOD_CLASS + " method, Object target, " +
                    EXTENSION_CONTEXT_CLASS + " extensionContext,\n" +
                    EXTENSION_REGISTRY_CLASS + " extensionRegistry) {\n" +
                    "\n" +
                    "method = " + INVOKER_PATCHER_CLASS + ".getMethod(method);\n" +
                    "return super.invoke(method, target, extensionContext, extensionRegistry);\n" +
                    "}\n", hackedEI));

            hackedEICls = hackedEI.toClass();
        }
        return hackedEICls;
    }

    @SuppressWarnings("unused") // use from generated class
    public static final Constructor getConstructor(Constructor<?> constructor) {
        try {
            Class<?> cls = constructor.getDeclaringClass();
            Class<?> newCls = CLASS_LOADER.loadClass(cls.getName());
            for (Constructor ct : newCls.getDeclaredConstructors()) {
                if (areSame(ct.getParameterTypes(), constructor.getParameterTypes())) {
                    return ct;
                }
            }
        } catch (Throwable throwable) {
            // skip
        }
        return constructor;
    }

    private static boolean areSame(Class<?>[] params1, Class<?>[] params2) {
        if (params1.length != params2.length) {
            return false;
        }
        for (int i = 0; i < params1.length; i++) {
            if (!params1[i].getName().equals(params2[i].getName())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused") // use from generated class
    public static Method getMethod(Method method) {
        try {
            Class<?> cls = method.getDeclaringClass();
            Class<?> newCls = CLASS_LOADER.loadClass(cls.getName());
            for (Method mt : newCls.getDeclaredMethods()) {
                if (mt.getName().equals(method.getName()) &&
                        areSame(mt.getParameterTypes(), method.getParameterTypes())) {
                    return mt;
                }
            }
        } catch (Throwable throwable) {
            // skip
        }
        return method;
    }
}
