package org.hyperledger.ariesframework.credentials.v1

import org.hyperledger.ariesframework.connection.repository.ConnectionRecord
import org.hyperledger.ariesframework.credentials.models.CredentialPreviewAttribute
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.hyperledger.ariesframework.credentials.v1.models.CredentialPreview

class CreateProposalOptions(
    val connection: ConnectionRecord,
    val credentialPreview: CredentialPreview? = null,
    val schemaIssuerDid: String? = null,
    val schemaId: String? = null,
    val schemaName: String? = null,
    val schemaVersion: String? = null,
    val credentialDefinitionId: String? = null,
    val issuerDid: String? = null,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val comment: String? = null,
)

class CreateOfferOptions(
    val connection: ConnectionRecord? = null,
    val credentialDefinitionId: String,
    val attributes: List<CredentialPreviewAttribute>,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val comment: String? = null,
)
