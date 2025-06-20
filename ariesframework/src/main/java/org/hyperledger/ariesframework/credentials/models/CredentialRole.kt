package org.hyperledger.ariesframework.credentials.models

import kotlinx.serialization.Serializable

@Serializable
enum class CredentialRole {
    Holder,
    Issuer,
}
