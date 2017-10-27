package io.mockk.junit;

import io.mockk.agent.MockKClassLoader;
import javassist.*;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.launcher.TestExecutionListener;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ExecutionInvokerPatcher implements TestExecutionListener {
    private static Unsafe unsafe;

    private static final ClassLoader CLASS_LOADER = MockKClassLoader.newClassLoader(ExecutionInvokerPatcher.class.getClassLoader());

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);

            System.out.println("PATCHING");
            patchExecutableInvokers(new String[]{
                    "org.junit.jupiter.engine.descriptor.ClassTestDescriptor",
                    "org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor"
            });
            Thread.currentThread().setContextClassLoader(CLASS_LOADER);
        } catch (Throwable ex) {
            System.err.println("WARNING: ExecutionInvokerPatcher failed: " + ex);
        }
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

            hackedEI.addMethod(CtNewMethod.make("public Object invoke(java.lang.reflect.Constructor constructor, org.junit.jupiter.api.extension.ExtensionContext extensionContext,\n" +
                    "org.junit.jupiter.engine.extension.ExtensionRegistry extensionRegistry) {\n" +
                    "\n" +
                    "constructor = io.mockk.junit.MockKJUnit5Extension.getConstructor(constructor);\n" +
                    "return super.invoke(constructor, extensionContext, extensionRegistry);\n" +
                    "}\n", hackedEI));

            hackedEI.addMethod(CtNewMethod.make("public Object invoke(java.lang.reflect.Method method, Object target, org.junit.jupiter.api.extension.ExtensionContext extensionContext,\n" +
                    "org.junit.jupiter.engine.extension.ExtensionRegistry extensionRegistry) {\n" +
                    "\n" +
                    "method = io.mockk.junit.MockKJUnit5Extension.getMethod(method);\n" +
                    "return super.invoke(method, target, extensionContext, extensionRegistry);\n" +
                    "}\n", hackedEI));
            hackedEICls = hackedEI.toClass();
        }
        return hackedEICls;
    }

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
