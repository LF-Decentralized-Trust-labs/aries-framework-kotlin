package org.hyperledger.ariesframework.credentials.models

class AcceptRequestOptions(
    val credentialRecordId: String,
    val autoAcceptCredential: AutoAcceptCredential? = null,
    val comment: String? = null,
)
