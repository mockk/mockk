package io.mockk.proxy.android;

import io.mockk.agent.MockKAgentException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Util {
    // Some classes are so deeply optimized inside the runtime that they cannot be transformed
    private static final Set<Class<? extends java.io.Serializable>> EXCLUDES = new HashSet<>(
            Arrays.asList(Class.class,
                    Boolean.class,
                    Byte.class,
                    Short.class,
                    Character.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    String.class));

    static Class nameToType(String name) throws ClassNotFoundException {
        switch (name) {
            case "byte":
                return Byte.TYPE;
            case "short":
                return Short.TYPE;
            case "int":
                return Integer.TYPE;
            case "long":
                return Long.TYPE;
            case "char":
                return Character.TYPE;
            case "float":
                return Float.TYPE;
            case "double":
                return Double.TYPE;
            case "boolean":
                return Boolean.TYPE;
            case "byte[]":
                return byte[].class;
            case "short[]":
                return short[].class;
            case "int[]":
                return int[].class;
            case "long[]":
                return long[].class;
            case "char[]":
                return char[].class;
            case "float[]":
                return float[].class;
            case "double[]":
                return double[].class;
            case "boolean[]":
                return boolean[].class;
            default:
                return classForTypeName(name);
        }
    }

    static Class<?> classForTypeName(String name) throws ClassNotFoundException {
        int nArrays = 0;
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            nArrays++;
        }

        if (nArrays > 0) {
            return Class.forName(repeat(nArrays, "[") + "L" + name + ";");
        } else {
            return Class.forName(name);
        }
    }

    private static String repeat(int n, String str) {
        StringBuilder sb = new StringBuilder(n * str.length());
        while (n-- > 0) {
            sb.append(str);
        }
        return sb.toString();
    }
}
