package io.mockk.gh

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test

data class Product(val name: String, val price: Int)
data class Order(val name: String)

class ShopService {

    fun buyProducts(products: List<Product>) {
        println("You bought $products...")
    }

    fun addProductAndOrders(products: List<Product>, orders: List<Order>) {
        println("Add $products and $orders...")
    }
}

class TestMockk {

    private val shopService = mockk<ShopService>()

    @BeforeTest
    internal fun setUp() {
        clearAllMocks()
    }

    @Test
    internal fun `should match list of arguments`() { // Passes

        every {
            shopService.buyProducts(any())
        } returns Unit
        val products = listOf(Product("raspberry", 2), Product("banana", 212))

        shopService.buyProducts(products)

    }

    @Test
    internal fun `should match with two arguments of type list`() { // Throws MockkException

        every {
            shopService.addProductAndOrders(products = any(), orders = any())
        } returns Unit

        val products = listOf(Product("raspberry", 2), Product("banana", 1))
        val orders = listOf(Order("raspber"), Order("banana"))

        shopService.addProductAndOrders(products, orders) // Throws MockkException
    }
}
