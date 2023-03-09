package io.provenance.scope.loan.test

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder

@OptIn(ExperimentalKotest::class)
object KotestConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Random
    override val parallelism = 4
    override val concurrentSpecs = 2
    override val concurrentTests = 4
    /** Option to run tests without cutting corners in edge cases or iteration counts. If true, may greatly increase test runtime. */
    val runTestsExtended: Boolean
        get() = System.getenv("RUN_KOTEST_EXTENDED") == "true"
}
