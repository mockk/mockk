package io.mockk;

public class JvmVarArgsCls {
    public int varArgsOp(int a, int... b) {
        for (int v : b) {
            a += v;
        }
        return a;
    }
}
