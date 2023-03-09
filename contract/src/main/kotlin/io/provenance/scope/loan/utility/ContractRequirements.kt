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

internal sealed interface EnforcementContext {
    fun raiseError(error: ContractViolation)
    fun requireThat(vararg newEnforcements: ContractEnforcement)
}

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
) : EnforcementContext {
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
    override fun requireThat(vararg newEnforcements: ContractEnforcement) = newEnforcements.forEach { (rule, violationReport) ->
        if (!rule) {
            addViolation(violationReport)
        }
    }

    /**
     * Immediately raises a [ContractViolation].
     *
     * @param error The violation message to propagate.
     */
    override fun raiseError(error: ContractViolation) = requireThat(false to error)

    internal class RequireThatEach(
        private val enforcements: MutableList<Item>,
    ) : Iterable<Pair<ContractViolation, List<UInt>>> {
        constructor() : this(mutableListOf())

        internal class Item(
            internal val index: UInt,
            internal val enforcements: MutableList<ContractEnforcement>,
        ) : EnforcementContext {
            constructor(index: Int) : this(index.toUInt(), mutableListOf<ContractEnforcement>())

            override fun requireThat(vararg newEnforcements: ContractEnforcement) {
                enforcements.addAll(newEnforcements)
            }

            override fun raiseError(error: ContractViolation) = requireThat(false to error)

            fun <T> List<T>.requireThatEach(
                listIndices: Boolean = true,
                requirement: Item.(T) -> Unit
            ) {
                RequireThatEach().apply {
                    forEachItem(requirement)
                }.forEach { (violationMessage, iterations) ->
                    iterations.count().let { iterationsCount ->
                        if (listIndices && iterationsCount < 6) {
                            iterations.joinToString().let { indicesSnippet ->
                                if (iterationsCount == 1) {
                                    "Iteration $indicesSnippet"
                                } else {
                                    "Iterations $indicesSnippet"
                                }
                            }
                        } else {
                            if (iterationsCount == 1) {
                                "1 instance"
                            } else {
                                "$iterationsCount instances"
                            }
                        }.let { iterationsSnippet ->
                            enforcements.add(false to "$violationMessage [$iterationsSnippet]")
                        }
                    }
                }
            }
        }

        internal fun <T> List<T>.forEachItem(requirement: Item.(T) -> Unit) {
            forEachIndexed { index, item ->
                enforcements.add(Item(index).apply { requirement(item) })
            }
        }

        override fun iterator(): Iterator<Pair<ContractViolation, List<UInt>>> =
            enforcements.fold(mutableMapOf<ContractViolation, List<UInt>>()) { acc, item ->
                item.enforcements.forEach { (rule, violationReport) ->
                    if (!rule) {
                        acc[violationReport] = acc.getOrDefault(violationReport, emptyList()) + item.index
                    }
                }
                return@fold acc
            }.toList().iterator()
    }

    fun <T> List<T>.requireThatEach(
        listIndices: Boolean = true,
        requirement: RequireThatEach.Item.(T) -> Unit
    ) {
        RequireThatEach().apply {
            forEachItem(requirement)
        }.forEach { (violationMessage, iterations) ->
            iterations.count().let { iterationsCount ->
                if (listIndices && iterationsCount < 6) {
                    iterations.joinToString().let { indicesSnippet ->
                        if (iterationsCount == 1) {
                            "Iteration $indicesSnippet"
                        } else {
                            "Iterations $indicesSnippet"
                        }
                    }
                } else {
                    if (iterationsCount == 1) {
                        "1 instance"
                    } else {
                        "$iterationsCount instances"
                    }
                }.let { iterationsSnippet ->
                    addViolation("$violationMessage [$iterationsSnippet]")
                }
            }
        }
    }

    /**
     * See [io.provenance.scope.loan.utility.handleViolations].
     */
    fun handleViolations() = violations.handleViolations(requirementType)
}
