package io.mockk.core;


interface AnInterface {
    int foo();
}

interface InterfaceWithDefaultImpl extends AnInterface {
    @Override
    default int foo() {
        return 10;
    }
}

public class ClassImplementingInterfaceWithDefaultMethod implements InterfaceWithDefaultImpl {
}
