package io.mockk



import kotlin.test.Test

import kotlin.test.assertFailsWith



class NonMockableClassesTest {



  @Test

  fun `should throw exception when mocking System class`() {

    assertFailsWith<IllegalArgumentException> {

      mockk<System>()

    }

  }



  @Test

  fun `should throw exception when mocking File class`() {

    assertFailsWith<IllegalArgumentException> {

      mockk<java.io.File>()

    }

  }



  @Test

  fun `should throw exception when mocking Path class`() {

    assertFailsWith<IllegalArgumentException> {

      mockk<java.nio.file.Path>()

    }

  }



  // Add more tests for other non-mockable classes as needed

}