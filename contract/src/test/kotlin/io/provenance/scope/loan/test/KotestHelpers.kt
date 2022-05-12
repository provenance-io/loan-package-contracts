package io.provenance.scope.loan.test

import com.google.protobuf.InvalidProtocolBufferException
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.UUIDVersion
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uInt
import io.kotest.property.arbitrary.uuid
import io.provenance.scope.loan.utility.ContractEnforcement
import io.provenance.scope.loan.utility.ContractViolation
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.ContractViolationMap
import io.provenance.scope.loan.utility.UnexpectedContractStateException
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.UUID as FigureTechUUID

/**
 * Generators of [Arb]itrary instances.
 */
internal object LoanPackageArbs {
    /* Primitives */
    val anyNonEmptyString: Arb<String> = Arb.string().filter { it.isNotBlank() }
    /* Contract requirements */
    val anyContractViolation: Arb<ContractViolation> = Arb.string()
    val anyContractEnforcement: Arb<ContractEnforcement> = Arb.bind(
        Arb.boolean(),
        Arb.string(),
    ) { requirement, violationReport ->
        ContractEnforcement(requirement, violationReport)
    }
    val anyContractViolationMap: Arb<ContractViolationMap> = Arb.bind(
        Arb.list(anyContractViolation),
        Arb.list(Arb.uInt()),
    ) { violationList, countList ->
        violationList.zip(countList).toMap().toMutableMap()
    }
    /* Figure Tech Protobufs */
    val anyChecksum: Arb<FigureTechChecksum> = Arb.bind(
        Arb.string(),
        Arb.string(),
    ) { checksumValue, algorithmType ->
        FigureTechChecksum.newBuilder().apply {
            checksum = checksumValue
            algorithm = algorithmType
        }.build()
    }
    val anyUuid: Arb<FigureTechUUID> = Arb.uuid(UUIDVersion.V4).map { arbUuidV4 ->
        FigureTechUUID.newBuilder().apply {
            value = arbUuidV4.toString()
        }.build()
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

internal infix fun UnexpectedContractStateException.shouldBeParseFailureFor(classifier: String) = apply {
    this.cause should beInstanceOf<InvalidProtocolBufferException>()
    this.message shouldBe "Could not unpack as class $classifier"
}
