package org.hyperledger.ariesframework.credentials.v2

import anoncreds_uniffi.Credential
import anoncreds_uniffi.CredentialDefinition
import anoncreds_uniffi.CredentialDefinitionPrivate
import anoncreds_uniffi.CredentialKeyCorrectnessProof
import anoncreds_uniffi.CredentialOffer
import anoncreds_uniffi.CredentialRequest
import anoncreds_uniffi.CredentialRequestMetadata
import anoncreds_uniffi.CredentialRevocationConfig
import anoncreds_uniffi.Issuer
import anoncreds_uniffi.Prover
import anoncreds_uniffi.RevocationRegistryDefinition
import anoncreds_uniffi.RevocationRegistryDefinitionPrivate
import anoncreds_uniffi.RevocationStatusList
import anoncreds_uniffi.Schema
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hyperledger.ariesframework.AckStatus
import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.AgentEvents
import org.hyperledger.ariesframework.agent.MessageSerializer
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.agent.decorators.ThreadDecorator
import org.hyperledger.ariesframework.anoncreds.storage.CredentialRecord
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.ICredentialStrategy
import org.hyperledger.ariesframework.credentials.models.AcceptCredentialOptions
import org.hyperledger.ariesframework.credentials.models.AcceptOfferOptions
import org.hyperledger.ariesframework.credentials.models.AcceptRequestOptions
import org.hyperledger.ariesframework.credentials.models.CredentialRole
import org.hyperledger.ariesframework.credentials.models.CredentialState
import org.hyperledger.ariesframework.credentials.repository.CredentialExchangeRecord
import org.hyperledger.ariesframework.credentials.repository.CredentialRecordBinding
import org.hyperledger.ariesframework.credentials.v2.messages.CredentialAckMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.IssueCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.OfferCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.ProposeCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.RequestCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.models.CreateCredentialOfferOptionsV2
import org.hyperledger.ariesframework.credentials.v2.models.CreateProposalOptionsV2
import org.hyperledger.ariesframework.credentials.v2.models.CredentialPreviewV2
import org.hyperledger.ariesframework.problemreports.messages.CredentialProblemReportNotificationMessage
import org.hyperledger.ariesframework.storage.BaseRecord
import org.hyperledger.ariesframework.storage.DidCommMessageRole
import org.slf4j.LoggerFactory
import java.util.UUID

class CredentialServiceV2(val agent: Agent) :
    ICredentialStrategy<
        CreateProposalOptionsV2,
        CreateCredentialOfferOptionsV2,
        AcceptOfferOptions,
        AcceptCredentialOptions,
        AcceptRequestOptions,
        ProposeCredentialMessageV2,
        OfferCredentialMessageV2,
        RequestCredentialMessageV2,
        CredentialAckMessageV2,
        IssueCredentialMessageV2,
        CredentialProblemReportNotificationMessage,
        > {
    private val logger = LoggerFactory.getLogger(CredentialServiceV2::class.java)

    private val credentialExchangeRepository = agent.credentialExchangeRepository
    private val didCommMessageRepository = agent.didCommMessageRepository
    private val ledgerService = agent.ledgerService

    init {
        Registers(agent).initialize()
    }

    /**
     * Create a ``ProposeCredentialMessageV2`` not bound to an existing credential record.
     *
     * @param options options for the proposal.
     * @return proposal message and associated credential record.
     */
    override suspend fun createProposeCredentialMessage(options: CreateProposalOptionsV2): Pair<ProposeCredentialMessageV2, CredentialExchangeRecord> {
        logger.debug("[2.0] createProposeCredentialMessage init")

        val credentialRecord = CredentialExchangeRecord(
            connectionId = options.connection.id,
            threadId = BaseRecord.generateId(),
            state = CredentialState.ProposalSent,
            role = CredentialRole.Holder,
            autoAcceptCredential = options.autoAcceptCredential,
            protocolVersion = CredentialsConstants.PROTOCOL_VERSION_V2,
        )

        var proposalCredentialMessageV2 = ProposeCredentialMessageV2.Builder()
            .comment(options.comment)
            .proposalAttachments(options.proposalAttachments)
            .goal(options.goal)
            .goalCode(options.goalCode)
            .credentialPreview(options.credentialPreview)
            .formats(options.formats)
            .build()
        proposalCredentialMessageV2.id = credentialRecord.threadId

        agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Sender, proposalCredentialMessageV2, credentialRecord.id)
        credentialExchangeRepository.save(credentialRecord)
        agent.eventBus.publish(AgentEvents.CredentialEventV2(credentialRecord.copy()))

        return Pair(proposalCredentialMessageV2, credentialRecord)
    }

    /**
     * Create a ``OfferCredentialMessageV2`` not bound to an existing credential record.
     *
     * @param options options for the offer.
     * @return offer message and associated credential record.
     */
    override suspend fun createOfferCredentialMessage(options: CreateCredentialOfferOptionsV2): Pair<OfferCredentialMessageV2, CredentialExchangeRecord> {
        logger.debug("[2.0] createOfferCredentialMessage init")

        if (options.connection == null) {
            logger.info("Creating credential offer without connection. This should be used for out-of-band request message with handshake.")
        }

        val credentialRecord = CredentialExchangeRecord(
            connectionId = options.connection?.id ?: "connectionless-offer",
            threadId = BaseRecord.generateId(),
            state = CredentialState.OfferSent,
            autoAcceptCredential = options.autoAcceptCredential,
            protocolVersion = CredentialsConstants.PROTOCOL_VERSION_V2,
        )

        val credentialDefinitionRecord = agent.credentialDefinitionRepository.getByCredDefId(options.credentialDefinitionId)
        val offer = Issuer().createCredentialOffer(
            credentialDefinitionRecord.schemaId,
            credentialDefinitionRecord.credDefId,
            CredentialKeyCorrectnessProof(credentialDefinitionRecord.keyCorrectnessProof),
        )
        val attachment = Attachment.fromData(offer.toJson().toByteArray(), OfferCredentialMessageV2.INDY_CREDENTIAL_OFFER_ATTACHMENT_ID)
        val credentialPreview = CredentialPreviewV2(options.attributes)

        val message = OfferCredentialMessageV2(
            formats = options.formats,
            offerAttachments = listOf(attachment),
            comment = options.comment,
            credentialPreview = credentialPreview,
            goal = options.goal,
            goalCode = options.goalCode,
            replacementId = options.replacementId,
        )

        message.id = credentialRecord.threadId

        agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Sender, message, credentialRecord.id)

        credentialRecord.credentialAttributes = options.attributes
        credentialExchangeRepository.save(credentialRecord)
        agent.eventBus.publish(AgentEvents.CredentialEvent(credentialRecord.copy()))

        return Pair(message, credentialRecord)
    }

    /**
     * Create a ``RequestCredentialMessageV2`` as response to a received credential offer.
     *
     * @param options options for the request.
     * @return request message.
     */
    override suspend fun createRequestCredentialMessage(options: AcceptOfferOptions): RequestCredentialMessageV2 {
        logger.debug("[2.0] createRequestCredentialMessage init")

        val credentialRecord = credentialExchangeRepository.getById(options.credentialRecordId)
        credentialRecord.assertProtocolVersion(CredentialsConstants.PROTOCOL_VERSION_V2)
        credentialRecord.assertState(CredentialState.OfferReceived)

        val offerMessageJson = didCommMessageRepository.getAgentMessage(credentialRecord.id, OfferCredentialMessageV2.type)
        val offerMessage = OfferCredentialMessageV2.decode(offerMessageJson)
        offerMessage.validateIndyAttachId()

        val holderDid = options.holderDid ?: getHolderDid(credentialRecord)
        val formats = offerMessage.formats
        val goal = offerMessage.goal
        val goalCode = offerMessage.goalCode
        val comment = offerMessage.comment
        val credentialOfferJson = offerMessage.getCredentialOffer()
        val credentialOffer = CredentialOffer(credentialOfferJson)
        val credentialDefinition = ledgerService.getCredentialDefinition(credentialOffer.credDefId())
        val linkSecret = agent.anoncredsService.getLinkSecret(agent.wallet.linkSecretId!!)

        val credReqTuple = Prover().createCredentialRequest(
            entropy = null,
            proverDid = holderDid,
            credDef = CredentialDefinition(credentialDefinition),
            linkSecret = linkSecret,
            linkSecretId = agent.wallet.linkSecretId!!,
            credOffer = credentialOffer,
        )

        credentialRecord.indyRequestMetadata = credReqTuple.metadata.toJson()
        credentialRecord.credentialDefinitionId = credentialOffer.credDefId()

        val attachment = Attachment.fromData(
            credReqTuple.request.toJson().toByteArray(),
            RequestCredentialMessageV2.INDY_CREDENTIAL_REQUEST_ATTACHMENT_ID,
        )

        val requestMessage = RequestCredentialMessageV2(
            formats = formats,
            requestAttachments = listOf(attachment),
            goalCode = goalCode,
            goal = goal,
            comment = comment,
        )
        requestMessage.thread = ThreadDecorator(credentialRecord.threadId)

        credentialRecord.credentialAttributes = offerMessage.credentialPreview?.attributes
        credentialRecord.autoAcceptCredential = options.autoAcceptCredential ?: credentialRecord.autoAcceptCredential

        didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Sender, requestMessage, credentialRecord.id)
        updateState(credentialRecord, CredentialState.RequestSent)

        return requestMessage
    }

    override suspend fun processRequestCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord {
        logger.debug("[2.0] processRequestCredentialMessage init")
        val requestMessage = MessageSerializer.decodeFromString(messageContext.plaintextMessage) as RequestCredentialMessageV2

        require(requestMessage.getRequestAttachmentById(RequestCredentialMessageV2.INDY_CREDENTIAL_REQUEST_ATTACHMENT_ID) != null) {
            "Indy attachment with id ${RequestCredentialMessageV2.INDY_CREDENTIAL_REQUEST_ATTACHMENT_ID} not found in request message"
        }

        var credentialRecord = credentialExchangeRepository.getByThreadAndConnectionId(
            requestMessage.threadId,
            null,
        )

        val connection = messageContext.assertReadyConnection()
        credentialRecord.connectionId = connection.id

        agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Receiver, requestMessage, credentialRecord.id)
        updateState(credentialRecord, CredentialState.RequestReceived)

        return credentialRecord
    }

    /**
     * Method called by {@link OfferCredentialHandlerV2} on reception of a offer credential message (2.0)
     * We do the necessary processing here to accept the offer and do the state change, emit event etc.
     * @param messageContext the inbound offer credential message
     * @returns credential record appropriate for this incoming message (once accepted)
     */
    override suspend fun processOfferCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord {
        logger.debug("[2.0] processOfferCredentialMessage init")

        val offerMessage = OfferCredentialMessageV2.decode(messageContext.plaintextMessage)
        offerMessage.validateIndyAttachId()

        var credentialExchangeRecord = credentialExchangeRepository.findByThreadAndConnectionId(offerMessage.threadId, messageContext.connection?.id)

        if (credentialExchangeRecord != null) {
            agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Receiver, offerMessage, credentialExchangeRecord.id)
            updateState(credentialExchangeRecord, CredentialState.OfferReceived)
        } else {
            val connection = messageContext.assertReadyConnection()
            credentialExchangeRecord = CredentialExchangeRecord(
                connectionId = connection.id,
                threadId = offerMessage.threadId,
                parentThreadId = offerMessage.threadId, // todo
                state = CredentialState.OfferReceived,
                role = CredentialRole.Holder,
                protocolVersion = CredentialsConstants.PROTOCOL_VERSION_V2,
            )

            agent.didCommMessageRepository.saveAgentMessage(
                role = DidCommMessageRole.Receiver,
                agentMessage = offerMessage,
                associatedRecordId = credentialExchangeRecord.id,
            )

            credentialExchangeRepository.save(credentialExchangeRecord)

            agent.eventBus.publish(AgentEvents.CredentialEventV2(credentialExchangeRecord.copy())) // accept credential?
        }

        return credentialExchangeRecord
    }

    /**
     * Process a received ``IssueCredentialMessageV2``.
     * This will store the credential, but not accept it yet.
     * Use ``createAck(options:)`` after calling this method to accept the credential and create an ack message.
     *
     * @param messageContext message context containing the credential message.
     * @return credential record associated with the credential message.
     */
    override suspend fun processIssueCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord {
        logger.debug("[2.0] processIssueCredentialMessage init")
        val issueMessage = IssueCredentialMessageV2.decode(messageContext.plaintextMessage)
        val issueAttachment = issueMessage.getCredentialAttachmentById(IssueCredentialMessageV2.INDY_CREDENTIAL_ATTACHMENT_ID)

        var credentialRecord = credentialExchangeRepository.getByThreadAndConnectionId(issueMessage.threadId, messageContext.connection?.id)
        val credential = Credential(issueAttachment.getDataAsString())
        logger.debug("Storing credential: ${credential.values()}")

        val (schemaJson, _) = ledgerService.getSchema(credential.schemaId())
        val schema = Schema(schemaJson)

        val credentialDefinition = CredentialDefinition(ledgerService.getCredentialDefinition(credential.credDefId()))
        val revocationRegistryJson = credential.revRegId()?.let { ledgerService.getRevocationRegistryDefinition(it) }
        val revocationRegistry = revocationRegistryJson?.let { RevocationRegistryDefinition(it) }
        if (revocationRegistry != null) {
            GlobalScope.launch {
                agent.revocationService.downloadTails(revocationRegistry)
            }
        }

        val linkSecret = agent.anoncredsService.getLinkSecret(agent.wallet.linkSecretId!!)
        val processedCredential = Prover().processCredential(
            cred = credential,
            credReqMetadata = CredentialRequestMetadata(credentialRecord.indyRequestMetadata!!),
            linkSecret = linkSecret,
            credDef = credentialDefinition,
            revRegDef = revocationRegistry,
        )

        val credentialId = UUID.randomUUID().toString()

        agent.credentialRepository.save(
            CredentialRecord(
                credentialId = credentialId,
                credentialRevocationId = processedCredential.revRegIndex()?.toString(),
                revocationRegistryId = processedCredential.revRegId(),
                linkSecretId = agent.wallet.linkSecretId!!,
                credentialObject = processedCredential,
                schemaId = processedCredential.schemaId(),
                schemaName = schema.name(),
                schemaVersion = schema.version(),
                schemaIssuerId = schema.issuerId(),
                issuerId = credentialDefinition.issuerId(),
                credentialDefinitionId = processedCredential.credDefId(),
            ),
        )

        credentialRecord.credentials.add(CredentialRecordBinding("indy", credentialId))
        agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Receiver, issueMessage, credentialRecord.id)
        updateState(credentialRecord, CredentialState.CredentialReceived)

        return credentialRecord
    }

    override suspend fun createCredentialAckMessage(options: AcceptCredentialOptions): CredentialAckMessageV2 {
        var credentialRecord = credentialExchangeRepository.getById(options.credentialRecordId)
        credentialRecord.assertProtocolVersion(CredentialsConstants.PROTOCOL_VERSION_V2)
        credentialRecord.assertState(CredentialState.CredentialReceived)

        updateState(credentialRecord, CredentialState.Done)

        return CredentialAckMessageV2(credentialRecord.threadId, AckStatus.OK)
    }

    /**
     * Create a ``IssueCredentialMessageV2`` as response to a received credential request.
     *
     * @param options options for the credential issueance.
     * @return credential message.
     */
    override suspend fun createIssueCredentialMessage(options: AcceptRequestOptions): IssueCredentialMessageV2 {
        logger.debug("[2.0] createIssueCredentialMessage init")

        var credentialRecord = credentialExchangeRepository.getById(options.credentialRecordId)
        credentialRecord.assertProtocolVersion(CredentialsConstants.PROTOCOL_VERSION_V2)
        credentialRecord.assertState(CredentialState.RequestReceived)

        val offerMessageJson = agent.didCommMessageRepository.getAgentMessage(credentialRecord.id, OfferCredentialMessageV2.type)
        val offerMessage = MessageSerializer.decodeFromString(offerMessageJson) as OfferCredentialMessageV2
        val requestMessageJson = agent.didCommMessageRepository.getAgentMessage(credentialRecord.id, RequestCredentialMessageV2.type)
        val requestMessage = MessageSerializer.decodeFromString(requestMessageJson) as RequestCredentialMessageV2

        val offerAttachment = offerMessage.getOfferAttachmentById(OfferCredentialMessageV2.INDY_CREDENTIAL_OFFER_ATTACHMENT_ID)
        val requestAttachment = requestMessage.getRequestAttachmentById(RequestCredentialMessageV2.INDY_CREDENTIAL_REQUEST_ATTACHMENT_ID)
        check(offerAttachment != null && requestAttachment != null) {
            "Missing data payload in offer or request attachment in credential Record ${credentialRecord.id}"
        }

        val formats = offerMessage.formats
        val goal = offerMessage.goal
        val comment = options.comment
        val offer = CredentialOffer(offerAttachment.getDataAsString())
        val request = CredentialRequest(requestAttachment.getDataAsString())
        val credDefId = offer.credDefId()
        val credentialDefinitionRecord = agent.credentialDefinitionRepository.getByCredDefId(credDefId)

        var revocationConfig: CredentialRevocationConfig? = null
        val revocationRecord = agent.revocationRegistryRepository.findByCredDefId(credDefId)
        if (revocationRecord != null) {
            val registryIndex = agent.revocationRegistryRepository.incrementRegistryIndex(credDefId)
            logger.debug("Revocation registry index: $registryIndex")
            revocationConfig = CredentialRevocationConfig(
                regDef = RevocationRegistryDefinition(revocationRecord.revocRegDef),
                regDefPrivate = RevocationRegistryDefinitionPrivate(revocationRecord.revocRegPrivate),
                statusList = RevocationStatusList(revocationRecord.revocStatusList),
                registryIndex = registryIndex.toUInt(),
            )
        }

        val credential = Issuer().createCredential(
            CredentialDefinition(credentialDefinitionRecord.credDef),
            CredentialDefinitionPrivate(credentialDefinitionRecord.credDefPriv),
            offer,
            request,
            credentialRecord.getCredentialInfo()!!.claims,
            null,
            revocationConfig,
        )

        val attachment = Attachment.fromData(
            credential.toJson().toByteArray(),
            IssueCredentialMessageV2.INDY_CREDENTIAL_ATTACHMENT_ID,
        )
        val issueMessage = IssueCredentialMessageV2(
            formats,
            listOf(attachment),
            goal,
            comment,
        )
        issueMessage.thread = ThreadDecorator(credentialRecord.threadId)

        agent.didCommMessageRepository.saveAgentMessage(DidCommMessageRole.Sender, issueMessage, credentialRecord.id)
        credentialRecord.autoAcceptCredential = options.autoAcceptCredential ?: credentialRecord.autoAcceptCredential
        updateState(credentialRecord, CredentialState.CredentialIssued)

        return issueMessage
    }

    /**
     * Create an ``CredentialProblemReportMessagev2`` as response to a received offer.
     *
     * @param options options for the problem report message.
     * @return credential problem report message.
     */
    override suspend fun createOfferDeclinedProblemReport(options: AcceptOfferOptions): CredentialProblemReportNotificationMessage {
        logger.info("[2.0] createOfferDeclinedProblemReport init")
        var credentialRecord = credentialExchangeRepository.getById(options.credentialRecordId)
        credentialRecord.assertProtocolVersion(CredentialsConstants.PROTOCOL_VERSION_V2)
        credentialRecord.assertState(CredentialState.OfferReceived)

        updateState(credentialRecord, CredentialState.Declined)

        return CredentialProblemReportNotificationMessage()
    }

    private suspend fun getHolderDid(credentialRecord: CredentialExchangeRecord): String {
        val connection = agent.connectionRepository.getById(credentialRecord.connectionId)
        return connection.did
    }

    suspend fun updateState(credentialRecord: CredentialExchangeRecord, newState: CredentialState) {
        logger.debug("[updateState] Updating credential record ${credentialRecord.id} to state $newState (previous=${credentialRecord.state}")
        credentialRecord.setToState(newState)
        credentialExchangeRepository.update(credentialRecord)
        agent.eventBus.publish(AgentEvents.CredentialEventV2(credentialRecord.copy()))
    }

    suspend fun processAck(messageContext: InboundMessageContext): CredentialExchangeRecord {
        val ackMessage = MessageSerializer.decodeFromString(messageContext.plaintextMessage) as CredentialAckMessageV2

        var credentialRecord = credentialExchangeRepository.getByThreadAndConnectionId(ackMessage.threadId, messageContext.connection?.id)
        updateState(credentialRecord, CredentialState.Done)

        return credentialRecord
    }

//    /**
//     * Create an ``CredentialProblemReportMessage`` as response to a received offer.
//     *
//     * @param options options for the problem report message.
//     * @return credential problem report message.
//     */
//    override suspend fun createOfferDeclinedProblemReport(options: AcceptOfferOptions): CredentialProblemReportMessage {
//        var credentialRecord = credentialExchangeRepository.getById(options.credentialRecordId)
//        credentialRecord.setToProtocolVersionV2()
//        credentialRecord.assertState(CredentialState.OfferReceived)
//
//        updateState(credentialRecord, CredentialState.Declined)
//
//        return CredentialProblemReportMessage(credentialRecord.threadId)
//    }
}
