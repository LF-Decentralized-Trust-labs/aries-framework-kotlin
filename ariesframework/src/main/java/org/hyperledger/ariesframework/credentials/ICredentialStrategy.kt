package org.hyperledger.ariesframework.credentials

import org.hyperledger.ariesframework.InboundMessageContext
import org.hyperledger.ariesframework.credentials.repository.CredentialExchangeRecord

// CreateProposalOptionsV2
// CreateCredentialOfferOptionsV2
// AcceptOfferOptionsV2
// AcceptCredentialOptionsV2
// AcceptRequestOptionsV2

// ProposeCredentialMessageV2
// OfferCredentialMessageV2
// RequestCredentialMessageV2
// CredentialAckMessageV2
// IssueCredentialMessageV2
interface ICredentialStrategy<O1, O2, O3, O4, O5, R1, R2, R3, R4, R5, P> {

    suspend fun createProposeCredentialMessage(options: O1): Pair<R1, CredentialExchangeRecord>

    suspend fun createOfferCredentialMessage(options: O2): Pair<R2, CredentialExchangeRecord>

    suspend fun createRequestCredentialMessage(options: O3): R3

    suspend fun processRequestCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord

    suspend fun processOfferCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord

    suspend fun processIssueCredentialMessage(messageContext: InboundMessageContext): CredentialExchangeRecord

    suspend fun createCredentialAckMessage(options: O4): R4

    suspend fun createIssueCredentialMessage(options: O5): R5

    suspend fun createOfferDeclinedProblemReport(options: O3): P
}
