package io.provenance.scope.loan.utility

/**
 * Denotes a violation of a contract requirement to be returned in an error to the requester.
 */
internal typealias ContractViolation = String

/**
 * Denotes a contract requirement and the [ContractViolation] that should be raised if it is not met.
 */
internal typealias ContractEnforcement = Pair<Boolean, ContractViolation>

/**
 * Denotes a mapping of the amount of times a unique [ContractViolation] was raised.
 */
internal typealias ContractViolationMap = MutableMap<ContractViolation, UInt>

/**
 * Denotes an [Exception] thrown by [validateRequirements] when at least one [ContractViolation] is found.
 */
internal class ContractViolationException(val overallViolationCount: UInt, message: String) : IllegalArgumentException(message)

/**
 * Maps a contract requirement to the appropriate [ContractViolation] that should be raised if it is not met.
 * Equivalent to [kotlin.to].
 */
internal infix fun Boolean.orError(error: ContractViolation): ContractEnforcement =
    Pair(this, error)

/**
 * Performs validation of one or more [ContractEnforcement]s.
 */
internal fun validateRequirements(
    vararg checks: ContractEnforcement
) = checks.fold<ContractEnforcement, ContractViolationMap>(mutableMapOf()) { acc, (rule, violationReport) ->
        acc.apply {
            if (!rule) {
                acc[violationReport] = acc.getOrDefault(violationReport, 0U) + 1U
            }
        }
    }.handleViolations()

/**
 * Performs validation of any [ContractEnforcement]s specified by [requireThat] in the body.
 * Should be used to wrap code blocks that also performs actions other than validation.
 */
internal fun validateRequirements(
    checksBody: ContractEnforcementContext.() -> Unit
) = ContractEnforcementContext()
    .apply(checksBody)
    .handleViolations()

/**
 * Aggregates multiple [ContractEnforcement]s into a single exception listing all of the [ContractViolation]s that
 * were found.
 *
 * @throws [ContractViolationException] if any of the supplied contract requirements were violated.
 * @return [Nothing] if no contract requirements were violated.
 */
private fun ContractViolationMap.handleViolations() =
    entries.fold(
        "" to 0U
    ) { (violationMessage, overallViolationCount), (violation, count) ->
        if (count > 0U) {
            "$violationMessage; $violation ($count occurrences)" to overallViolationCount + 1U
        } else {
            violationMessage to overallViolationCount
        }
    }.takeIf { (_, overallViolationCount) ->
        overallViolationCount > 0U
    }?.let { (formattedViolations, overallViolationCount) ->
        throw ContractViolationException(
            overallViolationCount,
            "The contract input was invalid - $overallViolationCount unique violations were found: $formattedViolations"
        )
    }

internal class ContractEnforcementContext {
    /**
     * A [ContractViolationMap] which can be updated by calls to [requireThat].
     */
    private val violations: ContractViolationMap = mutableMapOf()
    /**
     * Adds [ContractViolation]s to a [ContractViolationMap] that have their corresponding requirement violated.
     */
    fun requireThat(vararg enforcements: ContractEnforcement) = enforcements.forEach { (rule, violationReport) ->
        if (!rule) {
            violations[violationReport] = violations.getOrDefault(violationReport, 0U) + 1U
        }
    }
    /**
     * See [io.provenance.scope.loan.utility.handleViolations].
     */
    fun handleViolations() = violations.handleViolations()
}
