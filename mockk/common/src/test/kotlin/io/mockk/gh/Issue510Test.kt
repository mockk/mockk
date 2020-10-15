package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class Issue510Test {

    class ShopService {

        fun addProductAndOrders(products: List<Product>, orders: List<Order>) {
            println("Products $products and orders $orders")
        }
    }

    data class Product(val name: String, val price: Int)
    data class Order(val name: String)

    @Test
    fun `should match with two arguments of type list`() {
        // given
        val shopService = mockk<ShopService>()
        val products = listOf(Product("raspberry", 2), Product("banana", 1))
        val orders = listOf(Order("raspberry"), Order("banana"))

        every {
            shopService.addProductAndOrders(products = any(), orders = any())
        } returns Unit

        // when
        shopService.addProductAndOrders(products, orders)

        // then
        verify { shopService.addProductAndOrders(products, orders) }
    }
}
