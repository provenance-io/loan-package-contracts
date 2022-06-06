package io.provenance.scope.loan.contracts

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.Constructors.recordContractWithEmptyScope
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.PrimitiveArbs.anyBlankString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights

class OverwriteServicingRightsUnitTest : WordSpec({
    "A function initializing the servicing rights record" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                shouldThrow<ContractViolationException> {
                    recordContractWithEmptyScope.recordServicingRights(ServicingRights.getDefaultInstance())
                }.let { exception ->
                    exception shouldHaveViolationCount 1U
                    exception.message shouldContain "Servicing rights are not set"
                }
            }
        }
        "given an input with an invalid servicer ID" should {
            "throw an appropriate exception" {
                checkAll(anyInvalidUuid, anyNonEmptyString) { randomInvalidId, randomServicerName ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordServicingRights(
                            ServicingRights.newBuilder().also { servicingRightsBuilder ->
                                servicingRightsBuilder.servicerId = randomInvalidId
                                servicingRightsBuilder.servicerName = randomServicerName
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Servicing rights must have valid servicer UUID"
                    }
                }
            }
        }
        "given an input without a servicer name" should {
            "throw an appropriate exception" {
                checkAll(anyUuid, anyBlankString) { randomId, randomBlankString ->
                    shouldThrow<ContractViolationException> {
                        recordContractWithEmptyScope.recordServicingRights(
                            ServicingRights.newBuilder().also { servicingRightsBuilder ->
                                servicingRightsBuilder.servicerId = randomId
                                servicingRightsBuilder.servicerName = randomBlankString
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Servicing rights missing servicer name"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyUuid, anyNonEmptyString) { randomId, randomServicerName ->
                    recordContractWithEmptyScope.recordServicingRights(
                        ServicingRights.newBuilder().also { servicingRightsBuilder ->
                            servicingRightsBuilder.servicerId = randomId
                            servicingRightsBuilder.servicerName = randomServicerName
                        }.build()
                    ).let { newServicingRights ->
                        newServicingRights.servicerId shouldBe randomId
                        newServicingRights.servicerName shouldBe randomServicerName
                    }
                }
            }
        }
    }
    "A function overwriting the existing servicing rights record" When {
        UpdateServicingRightsContract().run {
            "given an empty input" should {
                "throw an appropriate exception" {
                    shouldThrow<ContractViolationException> {
                        updateServicingRights(ServicingRights.getDefaultInstance())
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Servicing rights are not set"
                    }
                }
            }
            "given an input with an invalid servicer ID" should {
                "throw an appropriate exception" {
                    checkAll(anyInvalidUuid, anyNonEmptyString) { randomInvalidId, randomServicerName ->
                        shouldThrow<ContractViolationException> {
                            updateServicingRights(
                                ServicingRights.newBuilder().also { servicingRightsBuilder ->
                                    servicingRightsBuilder.servicerId = randomInvalidId
                                    servicingRightsBuilder.servicerName = randomServicerName
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContain "Servicing rights must have valid servicer UUID"
                        }
                    }
                }
            }
            "given an input without a servicer name" should {
                "throw an appropriate exception" {
                    checkAll(anyUuid, anyBlankString) { randomId, randomBlankString ->
                        shouldThrow<ContractViolationException> {
                            updateServicingRights(
                                ServicingRights.newBuilder().also { servicingRightsBuilder ->
                                    servicingRightsBuilder.servicerId = randomId
                                    servicingRightsBuilder.servicerName = randomBlankString
                                }.build()
                            )
                        }.let { exception ->
                            exception shouldHaveViolationCount 1U
                            exception.message shouldContain "Servicing rights missing servicer name"
                        }
                    }
                }
            }
            "given a valid input" should {
                "not throw an exception" {
                    checkAll(anyUuid, anyNonEmptyString) { randomId, randomServicerName ->
                        updateServicingRights(
                            ServicingRights.newBuilder().also { servicingRightsBuilder ->
                                servicingRightsBuilder.servicerId = randomId
                                servicingRightsBuilder.servicerName = randomServicerName
                            }.build()
                        ).let { updatedServicingRights ->
                            updatedServicingRights.servicerId shouldBe randomId
                            updatedServicingRights.servicerName shouldBe randomServicerName
                        }
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
