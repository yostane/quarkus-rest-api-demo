package org.yostane

import io.quarkus.hibernate.orm.panache.PanacheEntity
import io.quarkus.hibernate.orm.panache.PanacheRepository
import kotlinx.serialization.Serializable
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.persistence.Entity
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Entity
@Serializable
class Greeting : PanacheEntity() {
    lateinit var message: String
}

@ApplicationScoped
class GreetingRepository : PanacheRepository<Greeting> {
    fun findByPrefix(prefix: String) = list("message like ?1", "$prefix%")
}

@Path("/")
class GreetingResource {

    @Inject
    lateinit var greetingRepository: GreetingRepository

    @GET
    @Path("/greetings/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    fun greetings(@PathParam("prefix") prefix: String) = greetingRepository.findByPrefix(prefix).orEmpty()
}