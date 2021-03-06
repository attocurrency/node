package org.atto.node.vote.weight

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@ExperimentalCoroutinesApi
@Component
class VoteWeightInfoContributor(val service: VoteWeightService) : InfoContributor {

    override fun contribute(builder: Info.Builder) {
        builder.withDetail("weights", service.getAll())
    }

}