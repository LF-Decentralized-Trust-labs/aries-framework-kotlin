package org.hyperledger.ariesframework.credentials.v1.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hyperledger.ariesframework.agent.AgentMessage
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.credentials.CredentialsConstants.Companion.OFFER_CREDENTIAL_V1
import org.hyperledger.ariesframework.credentials.v1.models.CredentialPreview

@Serializable
class OfferCredentialMessage(
    val comment: String? = null,
    @SerialName("credential_preview")
    val credentialPreview: CredentialPreview,
    @SerialName("offers~attach")
    val offerAttachments: List<Attachment>,
) : AgentMessage(generateId(), type) {
    companion object {
        const val INDY_CREDENTIAL_OFFER_ATTACHMENT_ID = "libindy-cred-offer-0"
        const val type = OFFER_CREDENTIAL_V1
    }

    fun getOfferAttachmentById(id: String): Attachment? {
        return offerAttachments.firstOrNull { it.id == id }
    }

    fun getCredentialOffer(): String {
        val attachment = getOfferAttachmentById(INDY_CREDENTIAL_OFFER_ATTACHMENT_ID)
        return attachment?.getDataAsString() ?: throw Exception("Credential offer attachment not found")
    }
}
