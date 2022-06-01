package io.provenance.scope.loan.test

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

object KotestConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Random
    override val parallelism = 4 // An experimental value - greater values may still improve performance
    /** Runs tests without cutting corners in edge cases or iteration counts. If true, will greatly increase time for tests to run. */
    val runTestsExtended: Boolean
        get() = System.getenv("RUN_KOTEST_EXTENDED") == "true"
}
