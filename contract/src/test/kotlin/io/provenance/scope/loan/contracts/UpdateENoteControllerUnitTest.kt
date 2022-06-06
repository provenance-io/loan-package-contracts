package io.provenance.scope.loan.contracts

import io.dartinc.registry.v1beta1.Controller
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyInvalidUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyUuid
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENote
import io.provenance.scope.loan.test.MetadataAssetModelArbs.anyValidENoteController
import io.provenance.scope.loan.test.PrimitiveArbs.anyBlankString
import io.provenance.scope.loan.test.PrimitiveArbs.anyNonEmptyString
import io.provenance.scope.loan.test.shouldHaveViolationCount
import io.provenance.scope.loan.utility.ContractViolationException

class UpdateENoteControllerUnitTest : WordSpec({
    "updateENoteController" When {
        "given an empty input" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote()) { randomExistingENote ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteControllerContract(
                            existingENote = randomExistingENote,
                        ).updateENoteController(
                            Controller.getDefaultInstance()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Controller is not set"
                    }
                }
            }
        }
        "given an input with an invalid controller ID" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote(), anyInvalidUuid, anyNonEmptyString) { randomExistingENote, randomInvalidId, randomControllerName ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteControllerContract(
                            existingENote = randomExistingENote,
                        ).updateENoteController(
                            Controller.newBuilder().also { controllerBuilder ->
                                controllerBuilder.controllerUuid = randomInvalidId
                                controllerBuilder.controllerName = randomControllerName
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Controller must have valid UUID"
                    }
                }
            }
        }
        "given an input without a controller name" should {
            "throw an appropriate exception" {
                checkAll(anyValidENote(), anyUuid, anyBlankString) { randomExistingENote, randomControllerId, randomBlankString ->
                    shouldThrow<ContractViolationException> {
                        UpdateENoteControllerContract(
                            existingENote = randomExistingENote,
                        ).updateENoteController(
                            Controller.newBuilder().also { controllerBuilder ->
                                controllerBuilder.controllerUuid = randomControllerId
                                controllerBuilder.controllerName = randomBlankString
                            }.build()
                        )
                    }.let { exception ->
                        exception shouldHaveViolationCount 1U
                        exception.message shouldContain "Controller is missing name"
                    }
                }
            }
        }
        "given a valid input" should {
            "not throw an exception" {
                checkAll(anyValidENote(), anyValidENoteController) { randomExistingENote, randomNewController ->
                    UpdateENoteControllerContract(
                        existingENote = randomExistingENote,
                    ).updateENoteController(
                        newController = randomNewController
                    ).let { updatedENote ->
                        updatedENote.controller shouldBe randomNewController
                    }
                }
            }
        }
    }
}) {
    override fun testCaseOrder() = TestCaseOrder.Random
}
