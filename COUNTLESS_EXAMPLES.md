Besides [documentation](mockk.io) you can find many other examples here. 
Fill free to submit pull request, it is really easy to do.

#### Calling lambda
```kotlin
class A { 
  suspend fun do(callback: (Result) -> Unit) {
    ...
  }
}

val reportResults: (Result) -> Unit = slot()
val aMock: A = mockk()
coEvery { 
  aMock.do(reportResults) 
} answers {
  reportResults(Result())
}
```
