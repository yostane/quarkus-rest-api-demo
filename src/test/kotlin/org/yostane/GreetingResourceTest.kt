package org.yostane

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.apache.http.HttpStatus
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject
import javax.transaction.Transactional


@QuarkusTest
class GreetingResourceTest {

    @Inject
    lateinit var greetingRepository: GreetingRepository
    
    @Transactional
    @BeforeEach
    fun prepareData() {
        greetingRepository.deleteAll()
        val greeting = Greeting()
        greeting.message = "hello world"
        greetingRepository.persist(greeting)
        greetingRepository.flush()
    }

    @Test
    fun testHelloEndpoint() {
        given()
            .`when`().get("/greetings/hello")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("$.size()", equalTo(1))
            .body("message", hasItem("hello world"))
    }

}