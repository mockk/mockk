package io.mockk.proxy.android;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MethodDescriptor {
    /**
     * Pattern to decompose a instrumentedMethodWithTypeAndSignature
     */
    private final static Pattern METHOD_PATTERN = Pattern.compile("(.*)#(.*)\\((.*)\\)");

    final String className;
    final String methodName;
    final Class<?>[] methodParamTypes;

    MethodDescriptor(String methodWithTypeAndSignature) throws ClassNotFoundException {
        Matcher methodComponents = METHOD_PATTERN.matcher(methodWithTypeAndSignature);
        boolean wasFound = methodComponents.find();
        if (!wasFound) {
            throw new IllegalArgumentException();
        }

        className = methodComponents.group(1);
        methodName = methodComponents.group(2);
        String methodParamTypeNames[] = methodComponents.group(3).split(",");

        ArrayList<Class<?>> methodParamTypesList = new ArrayList<>(methodParamTypeNames.length);
        for (String methodParamName : methodParamTypeNames) {
            if (methodParamName.equals("")) {
                continue;
            }

            methodParamTypesList.add(Util.nameToType(methodParamName));
        }

        methodParamTypes = methodParamTypesList.toArray(new Class<?>[]{});
    }

    @Override
    public String toString() {
        return className + "#" + methodName;
    }
}
