package io.provenance.scope.loan.test

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

class KotestConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Random
    override val parallelism = 2 // An experimental value - greater values may still improve performance
}
