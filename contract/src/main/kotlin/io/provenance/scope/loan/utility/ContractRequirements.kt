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
 * Denotes a mapping of the amount of times each unique [ContractViolation] was raised.
 */
internal typealias ContractViolationMap = Map<ContractViolation, Int>

/**
 * Maps a contract requirement to the appropriate [ContractViolation] that should be raised if it is not met.
 * Equivalent to [kotlin.to].
 */
internal infix fun Boolean.orError(error: ContractViolation): ContractEnforcement =
    Pair(this, error)

/**
 * Adds [ContractViolation]s to a [ContractViolationMap] that have their corresponding requirement violated.
 */
internal fun ContractViolationMap.requireThat(vararg enforcements: ContractEnforcement) = enforcements.forEach { (rule, violationReport) ->
    if (!rule) {
        plus(violationReport to getOrDefault(violationReport, 0) + 1)
    }
}

/**
 * Performs validation of any [ContractEnforcement]s specified by [requireThat] in the body.
 * Should be used to wrap code blocks that also performs actions other than validation.
 */
internal fun validateRequirements(
    checksBody: ContractViolationMap.() -> Unit
) = mutableMapOf<ContractViolation, Int>()
    .apply(checksBody)
    .handleViolations()

/**
 * Performs validation of one or more [ContractEnforcement]s.
 */
internal fun validateRequirements(
    vararg checks: ContractEnforcement
) = checks.fold<ContractEnforcement, ContractViolationMap>(emptyMap()) { acc, (rule, violationReport) ->
        if (!rule) {
            acc.plus(violationReport to (acc.getOrDefault(violationReport, 0) + 1))
        } else {
            acc
        }
    }.handleViolations()

/**
 * Aggregates multiple [ContractEnforcement]s into a single exception listing all of the [ContractViolation]s that were
 * found.
 *
 * @throws [IllegalArgumentException] if any of the supplied contract requirements were violated.
 * @return [Nothing] if no contract requirements were violated.
 */
private fun ContractViolationMap.handleViolations() =
    takeIf { map ->
        map.isNotEmpty()
    }?.let { violations -> // TODO: Determine how to best list out all the exceptions as a string
        violations.entries.joinToString { (violation, count) ->
            if (count > 1) {
                "$violation ($count occurrences)"
            } else {
                violation
            }
        }
    }?.let { formattedViolations ->
        throw IllegalArgumentException("The contract input was invalid: $formattedViolations")
    }
