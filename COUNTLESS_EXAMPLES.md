Besides [documentation](mockk.io) you can find many other examples here. 
Fill free to submit pull request, it is really easy to do.

<table>
<thead>
  <tr>
      <th>Description</th>
      <th>MockK</th>
      <th>mockito-kotlin</th>
  </tr>
</thead>
<tr>
<td>Calling lambda</td>
<td><pre>

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
</pre></td>
<td><pre>

</pre></td>
</tr>
</table>
