
fun main(args: Array<String>) {
    println("Hello world!")
}

class Test {
    fun abc(a: Int, b: Int) = a + b
}


/**** something similar to transformed code ****

fun main(args: Array<String>) {
    IM.main.intercept({ arg(0, args) }) {
        println("Hello world!")
    }
}

object IM {
    var main: Interceptor = PassThroughInterceptor

    val class = PassThroughInterceptor::class

    init {
        Interceptors.register() {
            main = execute(main, "main", Array<String>::class)
        }
    }
}

class Test {
    fun abc(a: Int, b: Int) = IM.abc.intercept(
            { arg(0, a); arg(1, b) },
            { a + b })

    object $IM : ClassInterceptorMap {
        var abc: Interceptor = PassThroughInterceptor

        val class = Test::class

        fun apply() {
            abc = execute(abc, "abc", Int::class, Int::class)
        }
}

 */