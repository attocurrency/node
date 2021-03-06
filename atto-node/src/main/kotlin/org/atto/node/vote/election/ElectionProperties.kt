package org.atto.node.vote.election

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "atto.vote.election")
class ElectionProperties {
    var stalingAfterTimeInSeconds: Long? = null
    var staledAfterTimeInSeconds: Long? = null
}