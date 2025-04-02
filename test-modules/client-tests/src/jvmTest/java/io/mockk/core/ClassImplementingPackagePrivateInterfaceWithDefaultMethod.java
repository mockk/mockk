package io.mockk.core;


interface InterfaceWithDefault {
    default int foo() {
        return 10;
    }
}

public class ClassImplementingPackagePrivateInterfaceWithDefaultMethod implements InterfaceWithDefault {
}
