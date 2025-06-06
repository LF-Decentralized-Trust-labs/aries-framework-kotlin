package org.hyperledger.ariesframework.credentials.v2
import kotlinx.serialization.json.Json
import org.hyperledger.ariesframework.OutboundMessage
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.Dispatcher
import org.hyperledger.ariesframework.credentials.models.AcceptCredentialOptions
import org.hyperledger.ariesframework.credentials.models.AcceptOfferOptions
import org.hyperledger.ariesframework.credentials.models.AcceptRequestOptions
import org.hyperledger.ariesframework.credentials.models.CredentialPreviewAttribute
import org.hyperledger.ariesframework.credentials.repository.CredentialExchangeRecord
import org.hyperledger.ariesframework.credentials.v2.messages.IssueCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.OfferCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.messages.RequestCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.models.CreateCredentialOfferOptionsV2
import org.hyperledger.ariesframework.credentials.v2.models.CreateProposalOptionsV2
import org.slf4j.LoggerFactory

class CredentialsCommandV2(val agent: Agent, private val dispatcher: Dispatcher) {
    private val logger = LoggerFactory.getLogger(CredentialsCommandV2::class.java)

    suspend fun proposeCredential(options: CreateProposalOptionsV2): CredentialExchangeRecord {
        val (message, credentialRecord) = agent.credentialServiceV2.createProposeCredentialMessage(options)
        agent.messageSender.send(OutboundMessage(message, options.connection))
        return credentialRecord
    }

    suspend fun offerCredential(options: CreateCredentialOfferOptionsV2): CredentialExchangeRecord {
        val (message, credentialRecord) = agent.credentialServiceV2.createOfferCredentialMessage(options)
        val connection = options.connection ?: throw Exception("Connection is required for sending credential offer")
        agent.messageSender.send(OutboundMessage(message, connection))
        return credentialRecord
    }

    suspend fun acceptOffer(options: AcceptOfferOptions): CredentialExchangeRecord {
        logger.info("acceptOffer init")

        val message = agent.credentialServiceV2.createRequestCredentialMessage(options)
        val credentialRecord = agent.credentialExchangeRepository.getById(options.credentialRecordId)

        // printAttributesOfCredential(credentialRecord.credentialAttributes);

        val connection = agent.connectionRepository.getById(credentialRecord.connectionId)

        agent.messageSender.send(OutboundMessage(message, connection))

        return credentialRecord
    }

    /*
     * helper method to show the attributes
     */
    private fun printAttributesOfCredential(credentialAttributes: List<CredentialPreviewAttribute>?) {
        if (credentialAttributes != null) {
            credentialAttributes.forEach { attribute ->
                logger.info("[IDD] Attribute name: ${attribute.name}, Value: ${attribute.value}")
            }
        }
    }

    /**
     * Declines a credential offer as holder (by sending a problem report message) to the connection
     *
     * @param options options to decline the offer.
     * @return credential record associated with the declined credential.
     */
    suspend fun declineOffer(options: AcceptOfferOptions): CredentialExchangeRecord {
        val message = agent.credentialServiceV2.createOfferDeclinedProblemReport(options)
        val credentialRecord = agent.credentialExchangeRepository.getById(options.credentialRecordId)
        val connection = agent.connectionRepository.getById(credentialRecord.connectionId)
        agent.messageSender.send(OutboundMessage(message, connection))

        return credentialRecord
    }

    /**
     * Accept a credential request as issuer (by sending a credential message) to the connection
     * associated with the credential record.
     *
     * @param options options to accept the request.
     * @return credential record associated with the sent credential message (IssueCredentialMessageV2).
     */
    suspend fun acceptRequest(options: AcceptRequestOptions): CredentialExchangeRecord {
        val message = agent.credentialServiceV2.createIssueCredentialMessage(options)
        val credentialRecord = agent.credentialExchangeRepository.getById(options.credentialRecordId)
        val connection = agent.connectionRepository.getById(credentialRecord.connectionId)
        agent.messageSender.send(OutboundMessage(message, connection))

        return credentialRecord
    }

    /**
     * Accept a credential as holder (by sending a credential acknowledgement message) to the connection
     * associated with the credential record.
     *
     * @param options options to accept the credential.
     * @return credential record associated with the sent credential acknowledgement message.
     */
    suspend fun acceptCredential(options: AcceptCredentialOptions): CredentialExchangeRecord {
        val message = agent.credentialServiceV2.createCredentialAckMessage(options)
        val credentialRecord = agent.credentialExchangeRepository.getById(options.credentialRecordId)
        val connection = agent.connectionRepository.getById(credentialRecord.connectionId)
        agent.messageSender.send(OutboundMessage(message, connection))
        return credentialRecord
    }

    /**
     * Find a ``OfferCredentialMessageV2`` by credential record id.
     *
     * @param credentialRecordId: the id of the credential record.
     * @return the offer message associated with the credential record.
     */
    suspend fun findOfferMessage(credentialRecordId: String): OfferCredentialMessageV2? {
        val messageJson = agent.didCommMessageRepository.findAgentMessage(credentialRecordId, OfferCredentialMessageV2.type)

        return if (messageJson != null) {
            Json.decodeFromString<OfferCredentialMessageV2>(messageJson)
        } else {
            null
        }
    }

    /**
     * Find a ``RequestCredentialMessageV2`` by credential record id.
     *
     * @param credentialRecordId: the id of the credential record.
     * @return the request message associated with the credential record.
     */
    suspend fun findRequestMessage(credentialRecordId: String): RequestCredentialMessageV2? {
        val messageJson = agent.didCommMessageRepository.findAgentMessage(credentialRecordId, RequestCredentialMessageV2.type)

        return if (messageJson != null) {
            Json.decodeFromString<RequestCredentialMessageV2>(messageJson)
        } else {
            null
        }
    }

    /**
     * Find a ``IssueCredentialMessageV2`` by credential record id.
     *
     * @param credentialRecordId: the id of the credential record.
     * @return the credential message associated with the credential record.
     */
    suspend fun findCredentialMessage(credentialRecordId: String): IssueCredentialMessageV2? {
        val messageJson = agent.didCommMessageRepository.findAgentMessage(credentialRecordId, IssueCredentialMessageV2.type)
        return if (messageJson != null) {
            Json.decodeFromString<IssueCredentialMessageV2>(messageJson)
        } else {
            null
        }
    }
}
