package io.provenance.scope.loan.test

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
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
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uInt
import io.kotest.property.arbitrary.uuid
import io.provenance.scope.loan.utility.ContractEnforcement
import io.provenance.scope.loan.utility.ContractViolation
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.ContractViolationMap
import io.provenance.scope.loan.utility.UnexpectedContractStateException
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.UUID as FigureTechUUID

/**
 * Generators of [Arb]itrary instances.
 */
internal object LoanPackageArbs {
    /* Primitives */
    val anyNonEmptyString: Arb<String> = Arb.string().filter { it.isNotBlank() }
    val anyNonUuidString: Arb<String> = Arb.string().filterNot { it.length == 36 }
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
    val anyValidChecksum: Arb<FigureTechChecksum> = Arb.bind(
        anyNonEmptyString,
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
    val anyValidTimestamp: Arb<Timestamp> = anyTimestampComponents.map { (seconds, nanoSeconds) ->
        Timestamp.newBuilder().also { timestampBuilder ->
            timestampBuilder.seconds = seconds
            timestampBuilder.nanos = nanoSeconds
        }.build()
    }
    val anyValidLoanState: Arb<LoanStateMetadata> = Arb.bind(
        anyUuid,
        anyValidChecksum,
        anyValidTimestamp,
        anyNonEmptyString,
    ) { uuid, checksum, effectiveTime, uri ->
        LoanStateMetadata.newBuilder().also { loanStateBuilder ->
            loanStateBuilder.id = uuid
            loanStateBuilder.checksum = checksum
            loanStateBuilder.effectiveTime = effectiveTime
            loanStateBuilder.uri = uri
        }.build()
    }
    fun loanStateSet(size: Int, slippage: Int = 10): Arb<Set<LoanStateMetadata>> =
        /** Since we need each *property* to be unique, we must fix the set size & construct the arbs from scratch with primitives */
        Arb.bind(
            Arb.set(gen = Arb.uuid(UUIDVersion.V4), size = size, slippage = slippage).map { it.toList() },
            Arb.set(gen = anyNonEmptyString, size = size, slippage = slippage).map { it.toList() },
            Arb.set(gen = anyNonEmptyString, size = size, slippage = slippage).map { it.toList() },
            Arb.set(gen = anyTimestampComponents, size = size, slippage = slippage).map { it.toList() },
        ) { randomIds, randomChecksums, randomUris, randomTimestamps ->
            randomIds.indices.map { i ->
                LoanStateMetadata.newBuilder().also { loanStateBuilder ->
                    loanStateBuilder.id = FigureTechUUID.newBuilder().also { uuidBuilder ->
                        uuidBuilder.value = randomIds[i].toString()
                    }.build()
                    loanStateBuilder.checksum = FigureTechChecksum.newBuilder().also { checksumBuilder ->
                        checksumBuilder.checksum = randomChecksums[i]
                    }.build()
                    loanStateBuilder.uri = randomUris[i]
                    loanStateBuilder.effectiveTime = Timestamp.newBuilder().also { timestampBuilder ->
                        timestampBuilder.seconds = randomTimestamps[i].first
                        timestampBuilder.nanos = randomTimestamps[i].second
                    }.build()
                }.build()
            }.toSet()
        }
}

private val anyTimestampComponents: Arb<Pair<Long, Int>> = Arb.pair(
    Arb.long(min = Timestamps.MIN_VALUE.seconds, max = Timestamps.MAX_VALUE.seconds),
    Arb.int(min = Timestamps.MIN_VALUE.nanos, max = Timestamps.MAX_VALUE.nanos),
)

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
