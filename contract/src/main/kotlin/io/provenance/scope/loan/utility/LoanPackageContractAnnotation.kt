package io.provenance.scope.loan.utility

// Provides the ability to get loan package contract classes by type name specified in the annotation. This decouples consumers from specific loan class type,
// while providing the ability to extend this package repo without the need to update consumers.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoanPackageContract(
    val type: String
)