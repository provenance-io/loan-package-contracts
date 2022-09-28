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
 * Denotes the type of contract requirement being evaluated by [validateRequirements].
 */
internal enum class ContractRequirementType(
    val toException: (UInt, String) -> Exception,
) {
    /**
     * A requirement for the state of a scope in order for a given contract to execute.
     */
    LEGAL_SCOPE_STATE({ violationCount, violations ->
        when (violationCount) {
            0U -> ""
            1U -> " - $violationCount violation was found"
            else -> " - $violationCount violations were found"
        }.let { violationCountSnippet ->
            IllegalContractStateException(
                "The contract state was invalid$violationCountSnippet" +
                    if (violations.isNotBlank()) ": $violations" else ""
            )
        }
    }),
    /**
     * A requirement for the input to a given contract in order for it to execute.
     */
    VALID_INPUT({ violationCount, violations ->
        when (violationCount) {
            0U -> ""
            1U -> " - $violationCount unique violation was found"
            else -> " - $violationCount unique violations were found"
        }.let { violationCountSnippet ->
            ContractViolationException(
                violationCount,
                "The contract input was invalid$violationCountSnippet" +
                    if (violations.isNotBlank()) ": $violations" else ""
            )
        }
    }),
}

/**
 * Maps a contract requirement to the appropriate [ContractViolation] that should be raised if it is not met.
 * Equivalent to [kotlin.to].
 */
internal infix fun Boolean.orError(error: ContractViolation): ContractEnforcement =
    Pair(this, error)

internal fun ContractEnforcementContext.raiseError(error: ContractViolation) = requireThat(false to error)

/**
 * Performs validation of one or more [ContractEnforcement]s.
 */
internal fun validateRequirements(
    requirementType: ContractRequirementType,
    vararg checks: ContractEnforcement,
) =
    checks.fold<ContractEnforcement, ContractViolationMap>(mutableMapOf()) { acc, (rule, violationReport) ->
        acc.apply {
            if (!rule) {
                acc[violationReport] = acc.getOrDefault(violationReport, 0U) + 1U
            }
        }
    }.handleViolations(requirementType)

/**
 * Performs validation of any [ContractEnforcement]s specified by [ContractEnforcementContext.requireThat] in the body.
 * Should be used to wrap code bodies that also performs actions other than validation.
 *
 * @param requirementType The type of validation being performed.
 * @param checksBody A body of code which contains calls to [ContractEnforcementContext.requireThat].
 * @throws Exception if at least one violation was collected in [checksBody].
 * @return The result of [checksBody], if no violations were collected.
 */
internal fun <T> validateRequirements(
    requirementType: ContractRequirementType,
    checksBody: ContractEnforcementContext.() -> T,
): T {
    with(ContractEnforcementContext(requirementType)) {
        val result = checksBody()
        handleViolations()
        return result
    }
}

/**
 * Aggregates multiple [ContractEnforcement]s into a single exception listing all of the [ContractViolation]s that
 * were found.
 *
 * @throws [Exception] if any of the supplied contract requirements were violated.
 * @return [Nothing] if no contract requirements were violated.
 */
private fun ContractViolationMap.handleViolations(
    requirementType: ContractRequirementType,
) =
    entries.fold(
        "" to 0U
    ) { (violationMessage, overallViolationCount), (violation, count) ->
        if (count > 0U) {
            if (count > 1U) {
                "\"$violation\" ($count occurrences)"
            } else {
                "\"$violation\""
            }.let { violationInstance ->
                "$violationMessage${if (overallViolationCount > 0U) ", " else ""} $violationInstance" to overallViolationCount + 1U
            }
        } else {
            violationMessage to overallViolationCount
        }
    }.takeIf { (_, overallViolationCount) ->
        overallViolationCount > 0U
    }?.let { (formattedViolations, overallViolationCount) ->
        throw requirementType.toException(overallViolationCount, formattedViolations)
    }

/**
 * Defines a body in which [ContractEnforcement]s can be freely defined and then collectively evaluated.
 */
internal class ContractEnforcementContext(
    private val requirementType: ContractRequirementType,
) {
    /**
     * A [ContractViolationMap] which can be updated by calls to [requireThat].
     */
    private val violations: ContractViolationMap = mutableMapOf()

    /**
     * Adds a [violation] to [violations].
     */
    private fun addViolation(violation: ContractViolation) {
        violations[violation] = violations.getOrDefault(violation, 0U) + 1U
    }
    /**
     * Adds [ContractViolation]s to a [ContractViolationMap] that have their corresponding requirement violated.
     */
    fun requireThat(vararg enforcements: ContractEnforcement): List<ContractEnforcement> = enforcements.toList().onEach { (rule, violationReport) ->
        if (!rule) {
            addViolation(violationReport)
        }
    }

    fun <T> List<T>.requireThatEach(iterationsDescription: String = "Iterations", requirement: (T) -> List<ContractEnforcement>) =
        fold(mutableMapOf<String, List<UInt>>()) { acc, item ->
            acc.also { map ->
                requirement(item).forEachIndexed { index, (rule, violationReport) ->
                    if (!rule) {
                        map[violationReport] = map.getOrDefault(violationReport, emptyList()) + index.toUInt()
                    }
                }
            }
        }.forEach { (violationMessage, iterations) ->
            val iterationsLimit = 5
            iterations.joinToString(
                limit = iterationsLimit,
                truncated = "...(${(iterations.size - iterationsLimit)} more omitted)",
            ).let { iterationIndicesSnippet ->
                addViolation("$violationMessage [$iterationsDescription $iterationIndicesSnippet]")
            }
        }
    /**
     * See [io.provenance.scope.loan.utility.handleViolations].
     */
    fun handleViolations() = violations.handleViolations(requirementType)
}
