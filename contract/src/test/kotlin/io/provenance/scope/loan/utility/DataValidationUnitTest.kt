package io.provenance.scope.loan.utility

import com.google.protobuf.Any
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.UUIDVersion
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.kotest.property.forNone
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonUuidString
import io.provenance.scope.loan.test.PrimitiveArbs.anyZoneOffset
import io.provenance.scope.util.toProtoTimestamp
import tech.figure.validation.v1beta1.ValidationIteration
import tech.figure.validation.v1beta1.ValidationRequest
import java.time.LocalDateTime
import java.time.ZoneOffset
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
import tech.figure.util.v1beta1.Money as FigureTechMoney
import tech.figure.util.v1beta1.UUID as FigureTechUUID

class DataValidationUnitTest : WordSpec({
    /* DataValidationExtensions */
    "Message.isSet" should {
        "return false for any default instance" {
            ValidationIteration.getDefaultInstance().isSet() shouldBe false
            ValidationIteration.newBuilder().build().isSet() shouldBe false
            Any.getDefaultInstance().isSet() shouldBe false
            Any.newBuilder().build().isSet() shouldBe false
        }

        "return true for any altered object" {
            checkAll(anyUuid) { randomId ->
                ValidationRequest.newBuilder().apply {
                    requestId = randomId
                }.build().let { modifiedValidationRequest ->
                    modifiedValidationRequest.isSet() shouldBe true
                }
            }
        }
        "return false for specific fields in an altered object" {
            checkAll(anyUuid) { randomId ->
                ValidationRequest.newBuilder().apply {
                    requestId = randomId
                }.build().let { modifiedValidationRequest ->
                    modifiedValidationRequest.isSet() shouldBe true
                    modifiedValidationRequest.effectiveTime.isSet() shouldBe false
                    modifiedValidationRequest.ruleSetId.isSet() shouldBe false
                }
            }
        }
    }

    "Timestamp.isValid" should {
        "return true for any date" {
            forAll(Arb.localDateTime(), anyZoneOffset) { randomLocalDateTime, randomZoneOffset ->
                randomLocalDateTime.atOffset(randomZoneOffset).toProtoTimestamp().isValid()
            }
        }
    }
    "Timestamp.isValidAndNotInFuture" should {
        "return true for any date in the past" {
            forAll(
                Arb.localDateTime(maxLocalDateTime = LocalDateTime.now()),
                anyZoneOffset,
            ) { randomLocalDateTime, randomZoneOffset ->
                randomLocalDateTime.atOffset(randomZoneOffset).toProtoTimestamp().isValidAndNotInFuture()
            }
        }
        "return false for any date in the future" {
            forNone(Arb.localDateTime(minLocalDateTime = LocalDateTime.now().plusDays(1L))) { randomLocalDateTime ->
                randomLocalDateTime.atOffset(ZoneOffset.UTC).toProtoTimestamp().isValidAndNotInFuture()
            }
        }
    }
    "Date.isValid" should {
        "return false for a default instance" {
            FigureTechDate.getDefaultInstance().isValid() shouldBe false
        }
        "return true for any valid date" {
            forAll(Arb.localDate()) { randomLocalDate ->
                FigureTechDate.newBuilder().apply {
                    value = randomLocalDate.toString()
                }.build().isValid()
            }
        }
    }
    "FigureTechUUID.isValid" should {
        "return false for a default instance" {
            FigureTechUUID.getDefaultInstance().isValid() shouldBe false
        }
        "return false for an invalid instance" {
            forNone(anyNonUuidString) { randomNonUuidString ->
                FigureTechUUID.newBuilder().apply {
                    value = randomNonUuidString
                }.build().isValid()
            }
        }
        "return true for any valid instance" {
            forAll(Arb.uuid(UUIDVersion.V4)) { randomJavaUuidV4 ->
                FigureTechUUID.newBuilder().apply {
                    value = randomJavaUuidV4.toString()
                }.build().isValid()
            }
        }
    }
    "Money.isValid" should {
        "return true for any double" {
            forAll(Arb.double()) { randomDouble ->
                FigureTechMoney.newBuilder().apply {
                    amount = randomDouble
                }.build().isValid()
            }
        }
    }
    /* LoanContractValidations */
    "checksumValidation" should {
        "throw an appropriate exception for a default instance" {
            shouldThrow<ContractViolationException> {
                validateRequirements(ContractRequirementType.VALID_INPUT) {
                    checksumValidation(checksum = FigureTechChecksum.getDefaultInstance())
                }
            }.let { exception ->
                exception.message shouldContain "Input's checksum is not set"
            }
        }
        "throw an appropriate exception if the checksum is set but not the algorithm" {
            checkAll(anyNonEmptyString) { randomChecksum ->
                shouldThrow<ContractViolationException> {
                    validateRequirements(ContractRequirementType.VALID_INPUT) {
                        checksumValidation(
                            checksum = FigureTechChecksum.newBuilder().apply {
                                clearAlgorithm()
                                checksum = randomChecksum
                            }.build()
                        )
                    }
                }.let { exception ->
                    exception.message shouldContain "Input must specify a checksum algorithm"
                }
            }
        }
        "throw an appropriate exception if the algorithm is set but not the checksum" {
            checkAll(anyNonEmptyString) { randomAlgorithm ->
                shouldThrow<ContractViolationException> {
                    validateRequirements(ContractRequirementType.VALID_INPUT) {
                        checksumValidation(
                            checksum = FigureTechChecksum.newBuilder().apply {
                                clearChecksum()
                                algorithm = randomAlgorithm
                            }.build()
                        )
                    }
                }.let { exception ->
                    exception.message shouldContain "Input must have a valid checksum string"
                }
            }
        }
        "not throw an exception for any non-empty checksum and algorithm strings" {
            checkAll(anyNonEmptyString, anyNonEmptyString) { randomChecksum, randomAlgorithm ->
                validateRequirements(ContractRequirementType.VALID_INPUT) {
                    checksumValidation(
                        checksum = FigureTechChecksum.newBuilder().apply {
                            checksum = randomChecksum
                            algorithm = randomAlgorithm
                        }.build()
                    )
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
