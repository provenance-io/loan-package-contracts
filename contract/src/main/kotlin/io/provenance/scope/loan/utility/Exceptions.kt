package io.provenance.scope.loan.utility

/**
 * Denotes an [Exception] thrown when a contract's assumptions are violated.
 */
class UnexpectedContractStateException(message: String, cause: Exception?) : IllegalStateException(message, cause) {
    constructor(message: String) : this(message, null)
}

/**
 * Denotes an [Exception] thrown by [validateRequirements] when a contract is executed for an inapplicable loan scope.
 */
class IllegalContractStateException(message: String) : IllegalStateException(message)

/**
 * Denotes an [Exception] thrown by [validateRequirements] when a contract is executed with invalid input.
 */
class ContractViolationException(val overallViolationCount: UInt, message: String) : IllegalArgumentException(message)
