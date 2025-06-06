package org.hyperledger.ariesframework.credentials.v2.models

import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.connection.repository.ConnectionRecord
import org.hyperledger.ariesframework.credentials.models.CredentialPreviewAttribute
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential

class CreateCredentialOfferOptionsV2(
    val connection: ConnectionRecord? = null,
    val credentialDefinitionId: String,
    val attributes: List<CredentialPreviewAttribute>,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val comment: String? = null,
    val formats: List<Format>,
    val offerAttachments: List<Attachment>,
    val goalCode: String? = null,
    val goal: String? = null,
    val credentialPreview: CredentialPreviewV2? = null,
    val replacementId: String? = null,
)
