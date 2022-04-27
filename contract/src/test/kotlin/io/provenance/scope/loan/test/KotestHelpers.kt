package io.provenance.scope.loan.test

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uInt
import io.provenance.scope.loan.utility.ContractEnforcement
import io.provenance.scope.loan.utility.ContractViolation
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.ContractViolationMap

/**
 * Generators of [Arb]itrary instances.
 */
internal object LoanPackageArbs {
    /* Contract requirements */
    val contractViolation: Arb<ContractViolation> = Arb.string()
    val contractEnforcement: Arb<ContractEnforcement> = Arb.bind(
        Arb.boolean(),
        Arb.string(),
    ) { requirement, violationReport ->
        ContractEnforcement(requirement, violationReport)
    }
    val contractViolationMapArb: Arb<ContractViolationMap> = Arb.bind(
        Arb.list(contractViolation),
        Arb.list(Arb.uInt()),
    ) { violationList, countList ->
        violationList.zip(countList).toMap().toMutableMap()
    }
}

/**
 * Defines a custom [Matcher] to check the violation count value in a [ContractViolationException].
 */
internal fun throwViolationCount(violationCount: UInt) = Matcher<ContractViolationException> { exception ->
    { count: UInt ->
        if (count == 1U) {
            "$count violation"
        } else {
            "$count violations"
        }
    }.let { violationPrinter: (UInt) -> String ->
        return@Matcher MatcherResult(
            exception.overallViolationCount == violationCount,
            {
                "Exception had ${violationPrinter(exception.overallViolationCount)} " +
                    "but we expected ${violationPrinter(violationCount)}"
            },
            { "Exception should not have ${violationPrinter(violationCount)}" },
        )
    }
}

/**
 * Wraps the custom matcher [throwViolationCount] following the style outlined in the
 * [Kotest documentation](https://kotest.io/docs/assertions/custom-matchers.html#extension-variants).
 */
internal infix fun ContractViolationException.shouldHaveViolationCount(violationCount: UInt) = apply {
    this should throwViolationCount(violationCount)
}
