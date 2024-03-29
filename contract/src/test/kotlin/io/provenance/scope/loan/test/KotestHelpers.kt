package io.provenance.scope.loan.test

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.dartinc.registry.v1beta1.Controller
import io.dartinc.registry.v1beta1.ENote
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.UUIDVersion
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.uInt
import io.kotest.property.arbitrary.uuid
import io.provenance.scope.loan.LoanPackage
import io.provenance.scope.loan.LoanScopeProperties
import io.provenance.scope.loan.utility.ContractEnforcement
import io.provenance.scope.loan.utility.ContractViolationException
import io.provenance.scope.loan.utility.ContractViolationMap
import io.provenance.scope.loan.utility.IllegalContractStateException
import io.provenance.scope.loan.utility.UnexpectedContractStateException
import tech.figure.asset.v1beta1.Asset
import tech.figure.loan.v1beta1.Disbursement
import tech.figure.loan.v1beta1.Funding
import tech.figure.loan.v1beta1.LoanDocuments
import tech.figure.loan.v1beta1.MISMOLoanMetadata
import tech.figure.proto.util.toProtoAny
import tech.figure.servicing.v1beta1.LoanStateOuterClass.LoanStateMetadata
import tech.figure.servicing.v1beta1.LoanStateOuterClass.ServicingData
import tech.figure.servicing.v1beta1.ServicingRightsOuterClass.ServicingRights
import tech.figure.util.v1beta1.ACH
import tech.figure.util.v1beta1.ACH.AccountType
import tech.figure.util.v1beta1.Account
import tech.figure.util.v1beta1.Address
import tech.figure.util.v1beta1.AssetType
import tech.figure.util.v1beta1.DocumentMetadata
import tech.figure.util.v1beta1.FinancialAccount
import tech.figure.util.v1beta1.MoneyMovement
import tech.figure.util.v1beta1.ProvenanceAccount
import tech.figure.util.v1beta1.SWIFT
import tech.figure.util.v1beta1.Status
import tech.figure.util.v1beta1.WIRE
import tech.figure.validation.v1beta2.LoanValidation
import tech.figure.validation.v1beta2.ValidationItem
import tech.figure.validation.v1beta2.ValidationIteration
import tech.figure.validation.v1beta2.ValidationOutcome
import tech.figure.validation.v1beta2.ValidationRequest
import tech.figure.validation.v1beta2.ValidationResponse
import tech.figure.validation.v1beta2.ValidationResultsMetadata
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import java.time.LocalDate as JavaLocalDate
import tech.figure.loan.v1beta1.Loan as FigureTechLoan
import tech.figure.util.v1beta1.Borrowers as FigureTechBorrowers
import tech.figure.util.v1beta1.Checksum as FigureTechChecksum
import tech.figure.util.v1beta1.Date as FigureTechDate
import tech.figure.util.v1beta1.Money as FigureTechMoney
import tech.figure.util.v1beta1.Name as FigureTechName
import tech.figure.util.v1beta1.Person as FigureTechPerson
import tech.figure.util.v1beta1.UUID as FigureTechUUID

/**
 * Generators of [Arb]itrary instances of classes not defined in the metadata asset model, like primitives or Java classes.
 */
internal object PrimitiveArbs {
    /* Primitives */
    val anyNonEmptyString: Arb<String> = Arb.string().filter { it.isNotBlank() }
    val anyBlankString: Arb<String> = Arb.string().filter { it.isBlank() }
    val anyNonUuidString: Arb<String> = Arb.string().filterNot { it.length == 36 }
    val anyValidUli: Arb<String> = Arb.string(minSize = 23, maxSize = 45, codepoints = Codepoint.alphanumeric())
    val anyNonUliString: Arb<String> = Arb.string().filterNot { it.length in 23..45 }
    val anyDoubleString: Arb<String> = Arb.double().filterNot { double ->
        /** Simple hack to avoid inapplicable edge cases without delving into `arbitrary {}` construction */
        double in listOf(Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)
    }.toSimpleString()
    /* Java classes */
    val anyZoneOffset: Arb<ZoneOffset> = Arb.int(min = ZoneOffset.MIN.totalSeconds, max = ZoneOffset.MAX.totalSeconds).map { offsetInSeconds ->
        ZoneOffset.ofTotalSeconds(offsetInSeconds)
    }
    /* Contract requirements */
    val anyContractEnforcement: Arb<ContractEnforcement> = Arb.bind(
        Arb.boolean(),
        Arb.string(),
    ) { requirement, violationReport ->
        ContractEnforcement(requirement, violationReport)
    }
    val anyContractViolationMap: Arb<ContractViolationMap> = Arb.bind(
        Arb.list(Arb.string()),
        Arb.list(Arb.uInt()),
    ) { violationList, countList ->
        violationList.zip(countList).toMap().toMutableMap()
    }
}

/**
 * Generators of [Arb]itrary instances of classes defined in the metadata asset model.
 */
internal object MetadataAssetModelArbs {
    /* Helper classes */
    internal data class TimestampPair(
        val earlierTimestamp: Timestamp,
        val laterTimestamp: Timestamp,
    )
    /* Protobufs */
    val anyAssetType: Arb<AssetType> = Arb.bind(
        PrimitiveArbs.anyNonEmptyString,
        Arb.string(),
    ) { superType, subType ->
        AssetType.newBuilder().also { assetTypeBuilder ->
            assetTypeBuilder.supertype = superType
            assetTypeBuilder.subtype = subType
        }.build()
    }
    val anyValidChecksum: Arb<FigureTechChecksum> = Arb.bind(
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
    ) { checksum, algorithm ->
        FigureTechChecksum.newBuilder().also { checksumBuilder ->
            checksumBuilder.checksum = checksum
            checksumBuilder.algorithm = algorithm
        }.build()
    }
    fun anyChecksumSet(size: Int, slippage: Int = 10): Arb<List<FigureTechChecksum>> =
        /** Since we need each checksum to be unique, we must fix the set size & construct the arbs from scratch with primitives */
        Arb.bind(
            Arb.set(gen = PrimitiveArbs.anyNonEmptyString, size = size, slippage = slippage).map { it.toList() },
            Arb.list(gen = PrimitiveArbs.anyNonEmptyString, range = size..size),
        ) { checksums, algorithms ->
            checksums.indices.map { i ->
                FigureTechChecksum.newBuilder().also { checksumBuilder ->
                    checksumBuilder.checksum = checksums[i]
                    checksumBuilder.algorithm = algorithms[i]
                }.build()
            }
        }
    val anyNonNegativeMoney: Arb<FigureTechMoney> = Arb.bind(
        Arb.stringPattern(Regex("^([0-9]+(?:[.][0-9]+)?|\\.[0-9]+)$").pattern),
        Arb.string(size = 3, codepoints = Codepoint.az()),
    ) { amount, currency ->
        FigureTechMoney.newBuilder().also { moneyBuilder ->
            moneyBuilder.value = amount
            moneyBuilder.currency = currency
        }.build()
    }
    val anyUuid: Arb<FigureTechUUID> = Arb.uuid(UUIDVersion.V4).map { arbUuidV4 ->
        FigureTechUUID.newBuilder().apply {
            value = arbUuidV4.toString()
        }.build()
    }
    fun anyUuidSet(size: Int, slippage: Int = 10): Arb<List<FigureTechUUID>> =
        Arb.set(gen = Arb.uuid(UUIDVersion.V4), size = size, slippage = slippage).map { set ->
            set.toList().map { uuid ->
                FigureTechUUID.newBuilder().apply {
                    value = uuid.toString()
                }.build()
            }
        }
    val anyInvalidUuid: Arb<FigureTechUUID> = PrimitiveArbs.anyNonUuidString.map { arbInvalidUuid ->
        FigureTechUUID.newBuilder().apply {
            value = arbInvalidUuid
        }.build()
    }
    val anyPerson: Arb<FigureTechPerson> = Arb.bind(
        anyUuid,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
    ) { id, firstName, lastName ->
        FigureTechPerson.newBuilder().also { personBuilder ->
            personBuilder.id = id
            personBuilder.name = FigureTechName.newBuilder().also { nameBuilder ->
                nameBuilder.firstName = firstName
                nameBuilder.lastName = lastName
            }.build()
        }.build()
    }
    fun anyBorrowerInfo(additionalBorrowerCount: IntRange = 0..0): Arb<FigureTechBorrowers> = Arb.bind(
        anyPerson,
        Arb.list(gen = anyPerson, range = additionalBorrowerCount),
    ) { primaryBorrower, additionalBorrowers ->
        FigureTechBorrowers.newBuilder().also { borrowersBuilder ->
            borrowersBuilder.primary = primaryBorrower
            borrowersBuilder.clearAdditional()
            borrowersBuilder.addAllAdditional(additionalBorrowers)
        }.build()
    }
    val anyValidDocumentMetadata: Arb<DocumentMetadata> = Arb.bind(
        anyUuid,
        anyValidChecksum,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
    ) { id, checksumValue, contentType, documentType, filename, uri ->
        DocumentMetadata.newBuilder().also { documentBuilder ->
            documentBuilder.id = id
            documentBuilder.checksum = checksumValue
            documentBuilder.contentType = contentType
            documentBuilder.documentType = documentType
            documentBuilder.fileName = filename
            documentBuilder.uri = uri
        }.build()
    }
    val anyPastNonEpochDate: Arb<FigureTechDate> = Arb.localDate(
        maxDate = JavaLocalDate.now(),
    ).map { javaLocalDate ->
        FigureTechDate.newBuilder().also { dateBuilder ->
            dateBuilder.value = javaLocalDate.toString()
        }.build()
    }.filterNot { date ->
        date.value === JavaLocalDate.of(1970, 1, 1).toString()
    }
    val anyValidAddress: Arb<Address> = Arb.bind(
        PrimitiveArbs.anyNonEmptyString,
        Arb.string(),
        Arb.string(),
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        Arb.string(),
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
    ) { street, street2, street3, city, state, country, zip, unitNumber, addressType, ownershipType ->
        Address.newBuilder().also { addressBuilder ->
            addressBuilder.street = street
            addressBuilder.street2 = street2
            addressBuilder.street3 = street3
            addressBuilder.city = city
            addressBuilder.state = state
            addressBuilder.country = country
            addressBuilder.zip = zip
            addressBuilder.unitNumber = unitNumber
            addressBuilder.addressType = addressType
            addressBuilder.ownershipType = ownershipType
        }.build()
    }
    val anyValidTimestamp: Arb<Timestamp> = anyTimestampComponents.toTimestamp()
    val anyPastNonEpochTimestamp: Arb<Timestamp> = anyPastNonEpochTimestampComponents.toTimestamp()
    val anyPastNonEpochTimestampPair: Arb<TimestampPair> = Arb.set(
        gen = anyPastNonEpochTimestampComponents,
        range = 2..2,
    ).map { set ->
        set.toList().sortedWith(compareBy({ it.first }, { it.second })).map { (seconds, nanoSeconds) ->
            Timestamp.newBuilder().also { timestampBuilder ->
                timestampBuilder.seconds = seconds
                timestampBuilder.nanos = nanoSeconds
            }.build()
        }.let { timestamps ->
            TimestampPair(
                earlierTimestamp = timestamps[0],
                laterTimestamp = timestamps[1],
            )
        }
    }
    val anyFutureTimestamp: Arb<Timestamp> = anyFutureTimestampComponents.toTimestamp()
    val anyValidFundingStatus: Arb<Status> = Arb.bind(
        Arb.of("UNFUNDED", "INITIATED", "FUNDED", "CANCELLED"),
        anyPastNonEpochTimestamp,
    ) { status, effectiveTime ->
        Status.newBuilder().also { statusBuilder ->
            statusBuilder.status = status
            statusBuilder.effectiveTime = effectiveTime
        }.build()
    }
    val anyValidAch: Arb<ACH> = Arb.bind(
        Arb.of(AccountType.CHECKING, AccountType.SAVINGS, AccountType.OTHER),
        Arb.string(),
    ) { accountType, ownerType ->
        ACH.newBuilder().also { achBuilder ->
            achBuilder.accountType = accountType
            achBuilder.ownerType = ownerType
        }.build()
    }
    val anyValidWire: Arb<WIRE> = Arb.bind(
        anyValidAddress,
        PrimitiveArbs.anyNonEmptyString,
        Arb.bind(
            PrimitiveArbs.anyNonEmptyString,
            anyValidAddress,
        ) { swiftId, swiftBankAddress ->
            SWIFT.newBuilder().also { swiftBuilder ->
                swiftBuilder.swiftId = swiftId
                swiftBuilder.swiftBankAddress = swiftBankAddress
            }.build()
        },
    ) { accountAddress, wireInstructions, swiftInstructions ->
        WIRE.newBuilder().also { wireBuilder ->
            wireBuilder.accountAddress = accountAddress
            wireBuilder.wireInstructions = wireInstructions
            wireBuilder.swiftInstructions = swiftInstructions
        }.build()
    }
    fun anyValidMoneyMovement(
        useACH: Boolean? = null,
    ): Arb<MoneyMovement> = Arb.bind(
        anyValidAch,
        anyValidWire,
        useACH?.let { Arb.of(it) } ?: Arb.boolean(),
    ) { ach, wire, useAch ->
        MoneyMovement.newBuilder().also { movementBuilder ->
            if (useAch) {
                movementBuilder.ach = ach
                movementBuilder.clearWire()
            } else {
                movementBuilder.wire = wire
                movementBuilder.clearAch()
            }
        }.build()
    }
    val anyValidFinancialAccount: Arb<FinancialAccount> = Arb.bind(
        anyUuid,
        Arb.string(),
        Arb.string(),
        Arb.string(minSize = 4, maxSize = 17),
        Arb.string(size = 9),
        Arb.list(anyValidMoneyMovement(), range = 1..3),
    ) { id, ownerName, financialInstitution, accountNumber, routingNumber, moneyMovementList ->
        FinancialAccount.newBuilder().also { accountBuilder ->
            accountBuilder.id = id
            accountBuilder.ownerName = ownerName
            accountBuilder.financialInstitution = financialInstitution
            accountBuilder.accountNumber = accountNumber
            accountBuilder.routingNumber = routingNumber
            accountBuilder.clearMovement()
            accountBuilder.addAllMovement(moneyMovementList)
        }.build()
    }
    val anyValidProvenanceAccount: Arb<ProvenanceAccount> = Arb.bind(
        Arb.stringPattern(Regex("^(pb|tp)1.{38}.*").pattern),
        Arb.string(),
    ) { address, description ->
        ProvenanceAccount.newBuilder().also { accountBuilder ->
            accountBuilder.address = address
            accountBuilder.description = description
        }.build()
    }
    fun anyValidDisbursementAccount(
        useBankAccount: Boolean? = null,
    ): Arb<Account> = Arb.bind(
        anyUuid,
        anyValidFinancialAccount,
        anyValidProvenanceAccount,
        useBankAccount?.let { Arb.of(it) } ?: Arb.boolean(),
    ) { id, financialAccount, provenanceAccount, useFinancialAccount ->
        Account.newBuilder().also { accountBuilder ->
            accountBuilder.accountOwnerId = id
            if (useFinancialAccount) {
                accountBuilder.financial = financialAccount
                accountBuilder.clearProvenance()
            } else {
                accountBuilder.provenance = provenanceAccount
                accountBuilder.clearFinancial()
            }
        }.build()
    }
    val anyValidDisbursement: Arb<Disbursement> = Arb.bind(
        anyUuid,
        anyNonNegativeMoney,
        anyPastNonEpochTimestampPair,
        Arb.bind(
            PrimitiveArbs.anyNonEmptyString, /* NOTE: This forms a passing, but not necessarily coherent, disbursement status */
            anyPastNonEpochTimestamp,
        ) { status, effectiveTime ->
            Status.newBuilder().also { statusBuilder ->
                statusBuilder.status = status
                statusBuilder.effectiveTime = effectiveTime
            }.build()
        },
        anyValidDisbursementAccount(),
    ) { id, amount, timestamps, status, disbursementAccount ->
        Disbursement.newBuilder().also { disbursementBuilder ->
            disbursementBuilder.id = id
            disbursementBuilder.amount = amount
            disbursementBuilder.started = timestamps.earlierTimestamp
            disbursementBuilder.completed = timestamps.laterTimestamp
            disbursementBuilder.status = status
            disbursementBuilder.disburseAccount = disbursementAccount
        }.build()
    }
    fun anyValidDisbursementList(size: Int, slippage: Int = 10): Arb<List<Disbursement>> = Arb.bind(
        anyUuidSet(size = size, slippage = slippage),
        Arb.list(anyValidDisbursement, range = size..size),
    ) { disbursementIds, disbursements ->
        disbursements.mapIndexed { index, disbursement ->
            disbursement.toBuilder().also { disbursementBuilder ->
                disbursementBuilder.id = disbursementIds[index]
            }.build()
        }
    }
    fun anyValidFunding(
        disbursementCount: IntRange,
    ): Arb<Funding> = Arb.bind(
        anyValidFundingStatus,
        anyPastNonEpochTimestampPair,
        Arb.int(range = disbursementCount).flatMap { count -> anyValidDisbursementList(size = count) },
    ) { status, timestamps, disbursementList ->
        Funding.newBuilder().also { fundingBuilder ->
            fundingBuilder.status = status
            fundingBuilder.started = timestamps.earlierTimestamp
            fundingBuilder.completed = timestamps.laterTimestamp
            fundingBuilder.clearDisbursements()
            fundingBuilder.addAllDisbursements(disbursementList)
        }.build()
    }
    val anyValidENoteController: Arb<Controller> = Arb.bind(
        anyUuid,
        PrimitiveArbs.anyNonEmptyString,
    ) { controllerId, controllerName ->
        Controller.newBuilder().also { controllerBuilder ->
            controllerBuilder.controllerUuid = controllerId
            controllerBuilder.controllerName = controllerName
        }.build()
    }
    fun anyValidFigureTechLoan(
        hasFunding: Boolean,
    ): Arb<FigureTechLoan> = Arb.bind(
        anyUuid,
        anyUuid,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyValidUli,
        if (hasFunding) anyValidFunding(disbursementCount = 1..6) else Arb.of(Funding.getDefaultInstance()),
    ) { loanId, originatorUuid, originatorName, uli, funding ->
        FigureTechLoan.newBuilder().also { loanBuilder ->
            loanBuilder.id = loanId
            loanBuilder.originatorUuid = originatorUuid
            loanBuilder.originatorName = originatorName
            loanBuilder.uli = uli
            if (hasFunding) {
                loanBuilder.funding = funding
            }
        }.build()
    }
    val anyValidFigureTechLoan: Arb<FigureTechLoan> = Arb.boolean().flatMap { hasFunding -> anyValidFigureTechLoan(hasFunding = hasFunding) }
    val anyValidMismoLoan: Arb<MISMOLoanMetadata> = Arb.bind(
        PrimitiveArbs.anyValidUli,
        anyValidDocumentMetadata,
    ) { uli, document ->
        MISMOLoanMetadata.newBuilder().also { loanBuilder ->
            loanBuilder.uli = uli
            loanBuilder.document = document
        }.build()
    }
    val anyValidLoanState: Arb<LoanStateMetadata> = Arb.bind(
        anyUuid,
        anyValidChecksum,
        anyPastNonEpochTimestamp,
        PrimitiveArbs.anyNonEmptyString,
    ) { uuid, checksum, effectiveTime, uri ->
        LoanStateMetadata.newBuilder().also { loanStateBuilder ->
            loanStateBuilder.id = uuid
            loanStateBuilder.checksum = checksum
            loanStateBuilder.effectiveTime = effectiveTime
            loanStateBuilder.uri = uri
        }.build()
    }
    fun loanStateSet(size: Int, slippage: Int = 10): Arb<List<LoanStateMetadata>> =
        /** Since we need some properties to be unique, we must fix the set size & construct the arbs from scratch with primitives */
        Arb.bind(
            anyUuidSet(size = size, slippage = slippage),
            anyChecksumSet(size = size, slippage = slippage),
            Arb.set(gen = PrimitiveArbs.anyNonEmptyString, size = size, slippage = slippage).map { it.toList() },
            Arb.set(gen = anyPastNonEpochTimestampComponents, size = size, slippage = slippage).map { it.toList() },
        ) { loanIds, randomChecksums, randomUris, randomTimestamps ->
            loanIds.indices.map { i ->
                LoanStateMetadata.newBuilder().also { loanStateBuilder ->
                    loanStateBuilder.id = loanIds[i]
                    loanStateBuilder.checksum = randomChecksums[i]
                    loanStateBuilder.uri = randomUris[i]
                    loanStateBuilder.effectiveTime = Timestamp.newBuilder().also { timestampBuilder ->
                        timestampBuilder.seconds = randomTimestamps[i].first
                        timestampBuilder.nanos = randomTimestamps[i].second
                    }.build()
                }.build()
            }
        }
    fun anyValidDocumentSet(size: Int, slippage: Int = 10): Arb<List<DocumentMetadata>> =
        /** Since we need some properties to be unique, we must fix the set size & construct the arbs from scratch with primitives */
        Arb.bind(
            anyUuidSet(size = size, slippage = slippage),
            Arb.set(gen = PrimitiveArbs.anyNonEmptyString, size = size, slippage = slippage).map { it.toList() },
            Arb.list(gen = PrimitiveArbs.anyNonEmptyString, range = size..size),
            Arb.list(gen = PrimitiveArbs.anyNonEmptyString, range = size..size),
            Arb.list(gen = PrimitiveArbs.anyNonEmptyString, range = size..size),
            anyChecksumSet(size = size, slippage = slippage),
        ) { documentIds, uris, fileNames, contentTypes, documentTypes, checksums ->
            documentIds.indices.map { i ->
                DocumentMetadata.newBuilder().also { documentBuilder ->
                    documentBuilder.id = documentIds[i]
                    documentBuilder.uri = uris[i]
                    documentBuilder.fileName = fileNames[i]
                    documentBuilder.contentType = contentTypes[i]
                    documentBuilder.documentType = documentTypes[i]
                    documentBuilder.checksum = checksums[i]
                }.build()
            }
        }
    val anyValidValidationRequest: Arb<ValidationRequest> = Arb.bind(
        anyUuid,
        anyPastNonEpochTimestamp,
        anyUuid,
        Arb.long(min = 0L),
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
    ) { requestId, effectiveTime, ruleSetId, blockHeight, description, validatorName, requesterName ->
        ValidationRequest.newBuilder().also { requestBuilder ->
            requestBuilder.requestId = requestId
            requestBuilder.effectiveTime = effectiveTime
            requestBuilder.ruleSetId = ruleSetId
            requestBuilder.blockHeight = blockHeight
            requestBuilder.description = description
            requestBuilder.validatorName = validatorName
            requestBuilder.requesterName = requesterName
        }.build()
    }
    val anyValidValidationItem: Arb<ValidationItem> = Arb.bind(
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        PrimitiveArbs.anyNonEmptyString,
        Arb.of(ValidationOutcome.EXCEPTION, ValidationOutcome.WARNING, ValidationOutcome.VALID),
    ) { code, label, description, expression, outcome ->
        ValidationItem.newBuilder().also { validationItemBuilder ->
            validationItemBuilder.code = code
            validationItemBuilder.label = label
            validationItemBuilder.description = description
            validationItemBuilder.expression = expression
            validationItemBuilder.validationOutcome = outcome
        }.build()
    }
    val anyValidValidationResponse: Arb<ValidationResponse> = Arb.bind(
        anyUuid,
        anyUuid,
        anyPastNonEpochTimestamp,
        PrimitiveArbs.anyNonEmptyString,
        anyValidChecksum,
    ) { requestId, resultsId, effectiveTime, uri, checksum ->
        ValidationResponse.newBuilder().also { responseBuilder ->
            responseBuilder.requestId = requestId
            responseBuilder.results = ValidationResultsMetadata.newBuilder().also { resultsBuilder ->
                resultsBuilder.id = resultsId
                resultsBuilder.uri = uri
                resultsBuilder.checksum = checksum
                resultsBuilder.effectiveTime = effectiveTime
            }.build()
        }.build()
    }
    val anyValidValidationIteration: Arb<ValidationIteration> = Arb.bind(
        anyValidValidationRequest,
        anyUuid,
        anyPastNonEpochTimestamp,
        PrimitiveArbs.anyNonEmptyString,
        anyValidChecksum,
    ) { request, resultsId, effectiveTime, uri, checksum ->
        ValidationIteration.newBuilder().also { iterationBuilder ->
            iterationBuilder.request = request
            iterationBuilder.results = ValidationResultsMetadata.newBuilder().also { resultsBuilder ->
                resultsBuilder.id = resultsId
                resultsBuilder.uri = uri
                resultsBuilder.checksum = checksum
                resultsBuilder.effectiveTime = effectiveTime
            }.build()
        }.build()
    }
    /* Loan scope records */
    fun anyValidAsset(
        hasMismoLoan: Boolean,
        hasFunding: Boolean,
    ): Arb<Asset> =
        Arb.bind(
            anyUuid,
            PrimitiveArbs.anyNonEmptyString,
            anyValidFigureTechLoan(hasFunding = hasFunding),
            if (hasMismoLoan) anyValidMismoLoan else Arb.of(MISMOLoanMetadata.getDefaultInstance()),
        ) { assetId, assetType, figureTechLoan, mismoLoan ->
            Asset.newBuilder().also { assetBuilder ->
                assetBuilder.id = assetId
                assetBuilder.type = assetType
                assetBuilder.putKv(LoanScopeProperties.assetLoanKey, figureTechLoan.toProtoAny())
                if (hasMismoLoan) {
                    assetBuilder.putKv(LoanScopeProperties.assetMismoKey, mismoLoan.toProtoAny())
                }
            }.build()
        }
    val anyValidAsset: Arb<Asset> = Arb.pair(Arb.boolean(), Arb.boolean()).flatMap { (hasMismoLoan, hasFunding) ->
        anyValidAsset(hasMismoLoan = hasMismoLoan, hasFunding = hasFunding)
    }
    fun anyValidENote(
        minAssumptionCount: Int = 0,
        maxAssumptionCount: Int = 10,
        minModificationCount: Int = 0,
        maxModificationCount: Int = 10,
        minSignatureCount: Int = 0,
        maxSignatureCount: Int = 10,
    ): Arb<ENote> = Arb.bind(
        anyValidENoteController,
        anyValidDocumentMetadata,
        anyPastNonEpochDate,
        PrimitiveArbs.anyNonEmptyString,
        Arb.list(anyValidDocumentMetadata, range = minModificationCount..maxModificationCount),
        Arb.list(anyValidDocumentMetadata, range = minAssumptionCount..maxAssumptionCount),
        Arb.list(anyValidDocumentMetadata, range = minSignatureCount..maxSignatureCount),
    ) { controller, document, signedDate, vaultName, modifications, assumptions, signatures ->
        ENote.newBuilder().also { eNoteBuilder ->
            eNoteBuilder.controller = controller
            eNoteBuilder.eNote = document
            eNoteBuilder.signedDate = signedDate
            eNoteBuilder.vaultName = vaultName
            eNoteBuilder.clearModification()
            eNoteBuilder.addAllModification(modifications)
            eNoteBuilder.clearAssumption()
            eNoteBuilder.addAllAssumption(assumptions)
            eNoteBuilder.clearBorrowerSignatureImage()
            eNoteBuilder.addAllBorrowerSignatureImage(signatures)
        }.build()
    }
    val anyValidServicingRights: Arb<ServicingRights> = Arb.bind(
        anyUuid,
        PrimitiveArbs.anyNonEmptyString
    ) { servicerId, servicerName ->
        ServicingRights.newBuilder().also { servicingRightsBuilder ->
            servicingRightsBuilder.servicerId = servicerId
            servicingRightsBuilder.servicerName = servicerName
        }.build()
    }
    fun anyValidLoanDocumentSet(size: Int, slippage: Int = 10): Arb<LoanDocuments> =
        anyValidDocumentSet(size = size, slippage = slippage).map { documentList ->
            documentList.toRecord()
        }
    fun anyValidServicingData(loanStateAndDocumentCount: Int, slippage: Int = 10): Arb<ServicingData> =
        Arb.bind(
            anyValidDocumentSet(size = loanStateAndDocumentCount, slippage = slippage),
            loanStateSet(size = loanStateAndDocumentCount, slippage = slippage),
        ) { documents, loanStates ->
            ServicingData.newBuilder().also { servicingDataBuilder ->
                servicingDataBuilder.clearDocMeta()
                servicingDataBuilder.addAllDocMeta(documents)
                servicingDataBuilder.clearLoanState()
                servicingDataBuilder.addAllLoanState(loanStates)
            }.build()
        }
    fun anyValidValidationRecord(
        iterationCount: Int,
        slippage: Int = 30,
    ): Arb<LoanValidation> = Arb.bind(
        Arb.list(
            anyValidValidationIteration,
            range = iterationCount..iterationCount
        ),
        anyUuidSet(size = iterationCount, slippage = slippage),
    ) { randomIterations, requestIds ->
        randomIterations.mapIndexed { index, iteration ->
            iteration.toBuilder().also { iterationBuilder ->
                iterationBuilder.request = iterationBuilder.request.toBuilder().also { requestBuilder ->
                    requestBuilder.requestId = requestIds[index]
                }.build()
            }.build()
        }.let { iterations ->
            LoanValidation.newBuilder().also { recordBuilder ->
                recordBuilder.clearIteration()
                recordBuilder.addAllIteration(iterations)
            }.build()
        }
    }
    fun anyValidLoan(
        maxAssumptionCount: Int = 0,
        maxModificationCount: Int = 0,
        loanStateCount: Int = 3,
        iterationCount: Int = 3,
        loanDocumentCount: Int = 3,
    ): Arb<LoanPackage> = Arb.boolean().flatMap { hasMismoLoan ->
        anyValidLoan(
            maxAssumptionCount = maxAssumptionCount,
            maxModificationCount = maxModificationCount,
            loanStateCount = loanStateCount,
            iterationCount = iterationCount,
            loanDocumentCount = loanDocumentCount,
            hasMismoLoan = hasMismoLoan,
        )
    }

    fun anyValidLoan(
        maxAssumptionCount: Int = 0,
        maxModificationCount: Int = 0,
        loanStateCount: Int = 3,
        iterationCount: Int = 3,
        loanDocumentCount: Int = 3,
        hasMismoLoan: Boolean,
    ): Arb<LoanPackage> = Arb.bind(
        Arb.boolean().flatMap { hasFunding -> anyValidAsset(hasMismoLoan = hasMismoLoan, hasFunding = hasFunding) },
        anyValidENote(maxAssumptionCount = maxAssumptionCount, maxModificationCount = maxModificationCount),
        anyValidServicingRights,
        anyValidServicingData(loanStateAndDocumentCount = loanStateCount),
        anyValidValidationRecord(iterationCount = iterationCount),
        anyValidLoanDocumentSet(size = loanDocumentCount),
    ) { randomAsset, randomENote, randomServicingRights, randomServicingData, randomValidationRecord, randomLoanDocuments ->
        LoanPackage(
            asset = randomAsset,
            eNote = randomENote,
            servicingRights = randomServicingRights,
            servicingData = randomServicingData,
            loanValidations = randomValidationRecord,
            documents = randomLoanDocuments,
        )
    }
}

/** Based on [this StackOverflow answer](https://stackoverflow.com/a/25307973). */
internal fun Arb<Double>.toSimpleString(): Arb<String> = map { double ->
    DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).apply {
        maximumFractionDigits = 340
    }.format(double)
}

private val anyTimestampComponents: Arb<Pair<Long, Int>> = Arb.pair(
    Arb.long(min = Timestamps.MIN_VALUE.seconds, max = Timestamps.MAX_VALUE.seconds),
    Arb.int(min = Timestamps.MIN_VALUE.nanos, max = Timestamps.MAX_VALUE.nanos),
)

private val anyFutureTimestampComponents: Arb<Pair<Long, Int>> = Instant.now().let { now ->
    Arb.pair(
        Arb.long(min = now.epochSecond + 1800, max = Timestamps.MAX_VALUE.seconds), // 30 minute buffer
        Arb.int(min = now.nano, max = Timestamps.MAX_VALUE.nanos),
    )
}

private val anyPastNonEpochTimestampComponents: Arb<Pair<Long, Int>> = Instant.now().let { now ->
    if (KotestConfig.runTestsExtended) {
        Arb.pair(
            Arb.long(min = Timestamps.MIN_VALUE.seconds, max = now.epochSecond),
            Arb.int(min = Timestamps.MIN_VALUE.nanos, max = now.nano),
        ).filterNot { (seconds, nanoSeconds) ->
            seconds == 0L && nanoSeconds == 0
        }
    } else {
        Arb.pair(
            Arb.long(min = Timestamps.MIN_VALUE.seconds, max = now.epochSecond),
            Arb.int(min = Timestamps.MIN_VALUE.nanos + 1, max = now.nano), // Minor sacrifice of case where nanos = 0, in exchange for faster runs
        )
    }
}

private fun Arb<Pair<Long, Int>>.toTimestamp(): Arb<Timestamp> = map { (seconds, nanoSeconds) ->
    Timestamp.newBuilder().also { timestampBuilder ->
        timestampBuilder.seconds = seconds
        timestampBuilder.nanos = nanoSeconds
    }.build()
}

/**
 * Defines a custom [Matcher] to check the violation count value in a [ContractViolationException].
 */
private fun throwViolationCount(violationCount: UInt) = Matcher<ContractViolationException> { exception ->
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
                    "but we expected ${violationPrinter(violationCount)} - message was ${exception.message}"
            },
            { "Exception should not have ${violationPrinter(violationCount)} - message was ${exception.message}" },
        )
    }
}

private fun containAtLeastOneOf(substrings: Iterable<String>) = Matcher<String> { message ->
    MatcherResult(
        substrings.any { substring -> message.contains(substring) },
        {
            "None of the given strings [${substrings.joinToString(limit = 5)}] were a substring of $message"
        },
        {
            "None of the given strings [${substrings.joinToString(limit = 5)}] should have been a substring of $message"
        }
    )
}

/**
 * Wraps the custom matcher [throwViolationCount] following the style outlined in the
 * [Kotest documentation](https://kotest.io/docs/assertions/custom-matchers.html#extension-variants).
 * This should be called by the result of [`shouldThrow<ContractViolationException>`][io.kotest.assertions.throwables.shouldThrow].
 */
internal infix fun ContractViolationException.shouldHaveViolationCount(violationCount: UInt) = apply {
    this should throwViolationCount(violationCount)
}

/**
 * Wraps the custom matcher [throwViolationCount] following the style outlined in the
 * [Kotest documentation](https://kotest.io/docs/assertions/custom-matchers.html#extension-variants).
 * This should be called by the result of [`shouldThrow<ContractViolationException>`][io.kotest.assertions.throwables.shouldThrow].
 */
internal infix fun ContractViolationException.shouldHaveViolationCount(violationCount: Int) = shouldHaveViolationCount(violationCount.toUInt())

/**
 * Wraps the custom matcher [throwViolationCount] following the style outlined in the
 * [Kotest documentation](https://kotest.io/docs/assertions/custom-matchers.html#extension-variants).
 */
internal infix fun String.shouldContainAtLeastOneOf(substrings: Iterable<String>) = apply {
    this should containAtLeastOneOf(substrings)
}

internal fun IllegalContractStateException.shouldBeParseFailureFor(classifier: String, inputDescription: String = "input") = apply {
    message shouldContain "Could not unpack the $inputDescription as class $classifier"
}

internal infix fun UnexpectedContractStateException.shouldBeParseFailureFor(classifier: String) = apply {
    cause should beInstanceOf<InvalidProtocolBufferException>()
    message shouldBe "Could not unpack as class $classifier"
}

internal fun Iterable<DocumentMetadata>.toRecord(): LoanDocuments = LoanDocuments.newBuilder().also { recordBuilder ->
    recordBuilder.clearDocument()
    recordBuilder.addAllDocument(this)
}.build()

internal fun <T> List<T>.breakOffLast(): Pair<List<T>, T> {
    require(isNotEmpty()) {
        "Must supply a list with at least one element"
    }
    return dropLast(1) to last()
}

internal fun <T> List<T>.breakOffLast(split: Int): Pair<List<T>, List<T>> {
    require(split in 0..size) {
        "Must supply a valid split"
    }
    return dropLast(split) to takeLast(split)
}

internal fun <T> Arb<List<T>>.toPair(): Arb<Pair<T, T>> = map { list -> list[0] to list[1] }

internal fun <S, T> Arb<S>.toPair(fn: (S) -> List<T>): Arb<Pair<T, T>> = map(fn).toPair()
