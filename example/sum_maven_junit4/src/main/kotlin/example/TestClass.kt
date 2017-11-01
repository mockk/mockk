package example

class TestClass(val mocked : MockedClass) {
    fun result() = mocked.sum(5, 3)
}
