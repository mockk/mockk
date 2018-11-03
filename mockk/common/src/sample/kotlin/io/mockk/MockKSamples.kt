package io.mockk

import kotlin.test.assertEquals


class MockKSamples {
    
    fun basicMockkCreation() {
        class Foo(val bar: String)
    
    
        val foo: Foo = mockk()
        
    }
    
    fun mockkWithCreationBlock() {
        class Foo(val bar: String)
        
        val foo: Foo = mockk {
            //Code that creates behaviour for this Mockk
            every { bar } returns "baz"
        }
    }
}

class SpykSamples {
    
    
    fun spyOriginalBehaviourCopyingFields() {
        class Foo(val bar: String = "bar", val baz: String = "baz")
        
        val foo = Foo(bar = "bar", baz = "baz")
        
        val spiedFoo = spyk(foo) {
            every { baz } returns "boz"
        }
        
        assertEquals("bar", spiedFoo.bar) //Will call the real behaviour
        
        assertEquals("boz", spiedFoo.baz)  //Will call the mocked behaviour
        
    }
    
    fun spyOriginalBehaviourDefaultConstructor() {
        
        class Foo(val bar: String = "bar", val baz: String = "baz")
        
        val spiedFoo = spyk<Foo> {
            every { baz } returns "boz"
        }
        
        assertEquals("bar", spiedFoo.bar) //Will call the real behaviour
        
        assertEquals("boz", spiedFoo.baz)  //Will call the mocked behaviour
        
    }
    
    fun spyOriginalBehaviourWithPrivateCalls() {
        
        class Foo(val bar: String, val baz: String) {
        
            fun callPrivateBarBaz(): String {
                return barBaz()
            }
            private fun barBaz() = "$bar.$baz"
        }
        
        val foo = Foo(bar = "bar", baz = "baz")
        
        val spiedFoo = spyk(foo, recordPrivateCalls = true)
        
        val barBaz = spiedFoo.callPrivateBarBaz()
        
        verify {
            spiedFoo invoke "barBaz" withArguments emptyList()  //Private method barBaz
        }
    }
    
}

class SlotSample {
    
    fun captureSlot() {
        class Foo {
            fun foo(string: String): String {
                return "$string.foo"
            }
        }
        
        val slot = slot<String>()
        
        val foo = mockk<Foo> {
            every { foo(capture(slot)) } answers { slot.captured + ".bar" }
        }
        
        val returnedValue = foo.foo("foo")
        assertEquals("foo.bar", returnedValue)
    }
    
}

class EverySample {
    
    fun simpleEvery() {
        class Foo {
            fun foo(): String {
                return "foo"
            }
        }
        
        val foo: Foo = mockk {
            every { foo() } returns "bar"
        }
        
        assertEquals("bar", foo.foo())
        
    }
    
}

class StaticMockkSample {
    
    fun mockJavaStatic() {
        public class JavaStatic {
            public static Foo staticMethod() {
                //...
            }
        }
        
        mockkStatic(JavaStatic::class)
        
        every { JavaStatic.staticMethod() } returns mockk<Foo>()
    }
    
    fun mockJavaStaticString() {
        package foo.bar
        public class JavaStatic {
            public static Foo staticMethod() {
                //...
            }
        }
        
        mockkStatic("foo.bar.JavaStatic")
        
        every { JavaStatic.staticMethod() } returns mockk<Foo>()
    }
}

class ObjectMockkSample {

    fun mockSimpleObject() {
        object Foo {
            val bar = "bar"
        }
        
        mockkObject(Foo)
        
        every { Foo.bar } returns "baz"
        
        assertEquals("baz", Foo.bar)
    }
    
    fun mockEnumeration() {
        enum class Enumeration(val goodInt: Int) {
            CONSTANT(35),
            OTHER_CONSTANT(45);
        }
    
        mockkObject(Enumeration.CONSTANT)
        every { Enumeration.CONSTANT.goodInt } returns 42
        assertEquals(42, Enumeration.CONSTANT.goodInt)
    }

}

class VerifySample {
    
    object random {
        fun shouldExecute(): Boolean = true
    }
    
    fun verifyAmount() {
        class Foo(val bar: String)
        
        val foo: Foo = mockk {
            every { bar } returns "bar"
        }
        val firstBar = foo.bar
        val secondBar = foo.bar
        
        verify(exactly = 2) { foo.bar }
        
    }
    
    fun verifyOrder() {
        class Foo {
            fun bar() = "bar"
            fun baz() = "baz"
            fun boo() = "boo"
        }
        
        val foo: Foo = mockk {
            every { bar() } returns "bot"
            every { baz() } returns "far"
            every { boo() } returns "laz"
        }
        
        foo.bar()
        foo.boo()
        foo.baz()
        
        verifyOrder {
            foo.bar()
            foo.baz()
        }   //These calls happened in this order, and it doesn't matter that foo.boo() also happened
    }
    
    fun verifySequence() {
        class Foo {
            fun bar() = "bar"
            fun baz() = "baz"
        }
        
        val foo: Foo = mockk {
            every { bar() } returns "bot"
            every { baz() } returns "far"
        }
        
        foo.bar()
        foo.baz()
        
        verifySequence {
            foo.bar()
            foo.baz()
        }   //These calls happened in this exact order, and only those were made in the mock
    }
    
    fun failingVerifySequence() {
        class Foo {
            fun bar() = "bar"
            fun baz() = "baz"
        }
        
        val foo: Foo = mockk {
            every { bar() } returns "bot"
            every { baz() } returns "far"
        }
        
        foo.bar()
        foo.baz()
        
        verifySequence {
            foo.bar()
        }   //This will fail, as no call to foo.baz() was expected in the call sequence, but it happened
    }
    
    
    fun verifyRange() {
        class Foo {
            fun foo() = println("foo")
            fun bar() {
                if (random.shouldExecute()) {
                    foo()
                }
            }
        }
        
        val spiedFoo = spyk<Foo>()
        
        spiedFoo.foo()
        
        for (i in 1..5) {
            spiedFoo.bar()
        }
        
        verify(atLeast = 1, atMost = 6) {
            spiedFoo.foo()
        }
        
    }
}