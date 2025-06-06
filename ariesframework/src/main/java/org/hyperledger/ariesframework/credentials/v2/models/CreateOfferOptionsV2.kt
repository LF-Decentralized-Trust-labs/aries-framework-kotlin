package org.hyperledger.ariesframework.credentials.v2.models

import org.hyperledger.ariesframework.connection.repository.ConnectionRecord
import org.hyperledger.ariesframework.credentials.models.CredentialPreviewAttribute
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential

class CreateOfferOptionsV2(
    val connection: ConnectionRecord? = null,
    val credentialDefinitionId: String,
    val attributes: List<CredentialPreviewAttribute>,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val comment: String? = null,
    val format: List<Format>,
)
