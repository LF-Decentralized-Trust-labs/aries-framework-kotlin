package org.hyperledger.ariesframework.credentials.v1.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.credentials.CredentialsConstants
import org.hyperledger.ariesframework.credentials.v1.models.CredentialPreview

@Serializable
class ProposeCredentialMessage(
    val comment: String? = null,
    @SerialName("credential_proposal")
    val credentialPreview: CredentialPreview? = null,
    @SerialName("schema_issuer_did")
    val schemaIssuerDid: String? = null,
    @SerialName("schema_id")
    val schemaId: String? = null,
    @SerialName("schema_name")
    val schemaName: String? = null,
    @SerialName("schema_version")
    val schemaVersion: String? = null,
    @SerialName("cred_def_id")
    val credentialDefinitionId: String? = null,
    @SerialName("issuer_did")
    val issuerDid: String? = null,
) : AgentMessage(generateId(), type) {
    companion object {
        const val type = CredentialsConstants.PROPOSE_CREDENTIAL_V1
    }
}
