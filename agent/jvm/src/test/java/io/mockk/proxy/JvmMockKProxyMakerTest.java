package io.mockk.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import io.mockk.proxy.jvm.JvmMockKAgentFactory;

public class JvmMockKProxyMakerTest {
    MockKProxyMaker maker;
    MockKStaticProxyMaker staticMaker;

    static boolean[] executed = new boolean[10];

    ListAppendingHandler handler;


    @Before
    public void setUp() {
        Arrays.fill(executed, false);
        handler = new ListAppendingHandler();

        JvmMockKAgentFactory agentFactory = new JvmMockKAgentFactory();

        agentFactory.init(MockKAgentLogFactory.Companion.getNO_OP());

        maker = agentFactory.getProxyMaker();
        staticMaker = agentFactory.getStaticProxyMaker();
    }

    static class A {
        public void a() {
            executed[0] = true;
        }
    }

    private <T> T makeProxy(Class<T> cls) {
        return maker.proxy(cls, new Class[0], handler, false, null).get();
    }

    @Test
    public void openClassProxy() throws Exception {
        A proxy = makeProxy(A.class);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        A proxy = makeProxy(A.class);

        proxy.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static final class B {
        public void a() {
            executed[0] = true;
        }
    }

    @Test
    public void finalClassProxy() throws Exception {
        B proxy = makeProxy(B.class);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void finalClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        B proxy = makeProxy(B.class);

        proxy.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    interface C {
        void a();
    }

    @Test
    public void interfaceProxy() throws Exception {
        C proxy = makeProxy(C.class);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static abstract class D {
        abstract void a();
    }

    @Test
    public void abstractClassProxy() throws Exception {
        D proxy = makeProxy(D.class);

        proxy.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E1 extends A {
        public void a() {
            executed[1] = true;
            super.a();
        }
    }

    @Test
    public void openSubClassProxy1() throws Exception {
        E1 proxy = makeProxy(E1.class);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy1() throws Exception {
        handler.callOriginal = true;
        E1 proxy = makeProxy(E1.class);

        proxy.a();

        assertTrue(executed[0]);
        assertTrue(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E2 extends A {
        public void a() {
            executed[1] = true;
        }
    }

    @Test
    public void openSubClassProxy2() throws Exception {
        E2 proxy = makeProxy(E2.class);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy2() throws Exception {
        handler.callOriginal = true;
        E2 proxy = makeProxy(E2.class);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    static class E3 extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    @Test
    public void openSubClassProxy3() throws Exception {
        E3 proxy = makeProxy(E3.class);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openSubClassCallOriginalProxy3() throws Exception {
        handler.callOriginal = true;
        E3 proxy = makeProxy(E3.class);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static class F extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    static class G extends F {
        public void a() {
            executed[3] = true;
            super.a();
        }

        public void b() {
            executed[4] = true;
            super.b();
        }
    }

    @Test
    public void openComplexSubClassProxy() throws Exception {
        G proxy = makeProxy(G.class);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        assertFalse(executed[3]);
        assertFalse(executed[4]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void openComplexSubClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        G proxy = makeProxy(G.class);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        assertTrue(executed[3]);
        assertTrue(executed[4]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static abstract class F1 extends A {
        public void a() {
            executed[1] = true;
            b();
        }

        public void b() {
            executed[2] = true;
        }
    }

    static final class G1 extends F1 {
        public void a() {
            executed[3] = true;
            super.a();
        }

        public void b() {
            executed[4] = true;
            super.b();
        }
    }

    @Test
    public void finalComplexSubClassProxy() throws Exception {
        G1 proxy = makeProxy(G1.class);

        proxy.a();

        assertFalse(executed[0]);
        assertFalse(executed[1]);
        assertFalse(executed[2]);
        assertFalse(executed[3]);
        assertFalse(executed[4]);
        checkProxyHandlerCalled(1, proxy, "a");
    }

    @Test
    public void finalComplexSubClassCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        G1 proxy = makeProxy(G1.class);

        proxy.a();

        assertFalse(executed[0]);
        assertTrue(executed[1]);
        assertTrue(executed[2]);
        assertTrue(executed[3]);
        assertTrue(executed[4]);
        checkProxyHandlerCalled(2, proxy, "a");
    }

    static class H {
        static void a() {
            executed[0] = true;
        }
    }

    @Test
    public void staticProxy() throws Exception {
        staticMaker.staticProxy(H.class, handler);

        H.a();

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, H.class, "a");
    }

    @Test
    public void staticCallOriginalProxy() throws Exception {
        handler.callOriginal = true;
        staticMaker.staticProxy(H.class, handler);

        H.a();

        assertTrue(executed[0]);
        checkProxyHandlerCalled(1, H.class, "a");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface ClassAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface MethodAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    @interface ParameterAnnotation {
    }

    @ClassAnnotation
    static class I {
        @MethodAnnotation
        public void i(@ParameterAnnotation String parameter) {
            executed[0] = true;
        }
    }

    @Test
    public void openClassAnnotationProxy() throws Exception {
        I proxy = makeProxy(I.class);

        Class proxyClass = proxy.getClass();
        assertTrue(proxyClass.isAnnotationPresent(ClassAnnotation.class));
        @SuppressWarnings("unchecked")
        Method proxyMethod = proxyClass.getMethod("i", String.class);
        assertTrue(proxyMethod.isAnnotationPresent(MethodAnnotation.class));
        assertEquals(ParameterAnnotation.class, proxyMethod.getParameterAnnotations()[0][0].annotationType());

        proxy.i("");

        assertFalse(executed[0]);
        checkProxyHandlerCalled(1, proxy, "i", 1, 1);
    }

    private void checkProxyHandlerCalled(int nTimes, Object proxy, String methodName) {
        checkProxyHandlerCalled(nTimes, proxy, methodName, 0, 0);
    }

    private void checkProxyHandlerCalled(int nTimes, Object proxy, String methodName, int parameterType, int argsLength) {

        StringBuilder sb = new StringBuilder();
        for (Call call : handler.calls) {
            sb.append(call.method);
            sb.append('\n');
        }

        assertEquals(
                "Amount of calls differ. Calls:\n" + sb.toString(),
                nTimes,
                handler.calls.size()
        );
        Call call = handler.calls.get(0);
        assertSame(proxy, call.self);
        assertSame(methodName, call.method.getName());
        assertSame(parameterType, call.method.getParameterTypes().length);
        assertSame(argsLength, call.args.length);
    }

    private static class ListAppendingHandler implements MockKInvocationHandler {
        boolean callOriginal = false;

        Object returnValue = null;

        List<Call> calls = new ArrayList<Call>();

        @Override
        public Object invocation(Object self,
                                 Method method,
                                 Callable<?> originalCall,
                                 Object[] args) throws Exception {

            calls.add(new Call(self, method, args));
            if (callOriginal) {
                return originalCall.call();
            } else {
                return returnValue;
            }
        }
    }

    private static class Call {
        StackTraceElement[] stackTrace;
        Object self;
        Method method;
        Object[] args;

        public Call(Object self, Method method, Object[] args) {
            this.self = self;
            this.method = method;
            this.args = args;
            this.stackTrace = new Exception().getStackTrace();
        }

        @Override
        public String toString() {
            return "Call{method clazz=" + Arrays.toString(this.stackTrace) + '}';
        }
    }
}
