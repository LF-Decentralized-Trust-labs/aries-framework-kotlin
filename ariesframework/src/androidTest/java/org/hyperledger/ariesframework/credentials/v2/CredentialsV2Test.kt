package org.hyperledger.ariesframework.credentials.v2

import androidx.test.filters.LargeTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hyperledger.ariesframework.TestHelper
import org.hyperledger.ariesframework.agent.Agent
import org.hyperledger.ariesframework.agent.decorators.Attachment
import org.hyperledger.ariesframework.agent.decorators.AttachmentData
import org.hyperledger.ariesframework.connection.repository.ConnectionRecord
import org.hyperledger.ariesframework.credentials.models.AcceptCredentialOptions
import org.hyperledger.ariesframework.credentials.models.AcceptOfferOptions
import org.hyperledger.ariesframework.credentials.models.AcceptRequestOptions
import org.hyperledger.ariesframework.credentials.models.CredentialState
import org.hyperledger.ariesframework.credentials.repository.CredentialExchangeRecord
import org.hyperledger.ariesframework.credentials.models.AutoAcceptCredential
import org.hyperledger.ariesframework.credentials.v2.messages.IssueCredentialMessageV2
import org.hyperledger.ariesframework.credentials.v2.models.CreateCredentialOfferOptionsV2
import org.hyperledger.ariesframework.credentials.v2.models.CredentialPreviewV2
import org.hyperledger.ariesframework.credentials.v2.models.Format
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class CredentialsV2Test {
    private val logger = LoggerFactory.getLogger(CredentialsV2Test::class.java)
    lateinit var faberAgent: Agent
    lateinit var aliceAgent: Agent
    lateinit var credDefId: String
    lateinit var faberConnection: ConnectionRecord
    lateinit var aliceConnection: ConnectionRecord
    lateinit var formats: List<Format>
    lateinit var offerAttachments: List<Attachment>

    val credentialPreview = CredentialPreviewV2.fromDictionary(mapOf("name" to "John", "age" to "99"))

    @Before
    fun setUp() = runTest(timeout = 5000.seconds) {
        val (agents, connections) = TestHelper.setupCredentialTests()
        faberAgent = agents.first
        aliceAgent = agents.second
        faberConnection = connections.first
        aliceConnection = connections.second
        credDefId = TestHelper.prepareForIssuance(faberAgent, listOf("name", "age"))
        formats = listOf(Format("indy", "hlindy/cred@v2.0"))
        offerAttachments = listOf(
            Attachment(
                id = "indy",
                mimetype = "application/json",
                data = AttachmentData(),
            ),
        )
    }

    @After
    fun tearDown() = runTest {
        faberAgent.reset()
        aliceAgent.reset()
    }

    suspend fun getCredentialRecord(agent: Agent, threadId: String): CredentialExchangeRecord {
        var credential = agent.credentialExchangeRepository.getByThreadAndConnectionId(threadId, null)
        credential.setToProtocolVersionV2()
        return credential
    }

    @Test @LargeTest
    fun testCredentialOffer() = runTest {
        // Faber starts with credential offer to Alice.
        var faberCredentialRecord = faberAgent.credentialsV2.offerCredential(
            CreateCredentialOfferOptionsV2(
                connection = faberConnection,
                credentialDefinitionId = credDefId,
                attributes = credentialPreview.attributes,
                autoAcceptCredential = null,
                comment = "Offer to Alice",
                formats = formats,
                goalCode = null,
                goal = null,
                credentialPreview = credentialPreview,
                replacementId = null,
                offerAttachments = offerAttachments,
            ),
        )

        val threadId = faberCredentialRecord.threadId
        var aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        assertEquals(aliceCredentialRecord.state, CredentialState.OfferReceived)

        aliceAgent.credentialsV2.acceptOffer(AcceptOfferOptions(aliceCredentialRecord.id))
        faberCredentialRecord = getCredentialRecord(faberAgent, threadId)
        assertEquals(faberCredentialRecord.state, CredentialState.RequestReceived)

        faberAgent.credentialsV2.acceptRequest(AcceptRequestOptions(faberCredentialRecord.id))
        aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        assertEquals(aliceCredentialRecord.state, CredentialState.CredentialReceived)

        aliceAgent.credentialsV2.acceptCredential(AcceptCredentialOptions(aliceCredentialRecord.id))
        aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        assertEquals(aliceCredentialRecord.state, CredentialState.Done)
        faberCredentialRecord = getCredentialRecord(faberAgent, threadId)
        assertEquals(faberCredentialRecord.state, CredentialState.Done)

        val credentialMessage = aliceAgent.credentialsV2.findCredentialMessage(aliceCredentialRecord.id)
        assertNotNull(credentialMessage)
        val attachment = credentialMessage?.getCredentialAttachmentById(IssueCredentialMessageV2.INDY_CREDENTIAL_ATTACHMENT_ID)
        assertNotNull(attachment)

        val credentialJson = attachment?.getDataAsString()
        val credential = Json.decodeFromString<JsonObject>(credentialJson!!)
        val values = credential["values"]!!.jsonObject
        val age = values["age"]!!.jsonObject
        assertEquals(age["raw"]!!.jsonPrimitive.content, "99")
        assertEquals(age["encoded"]!!.jsonPrimitive.content, "99")

        val name = values["name"]!!.jsonObject
        assertEquals(name["raw"]!!.jsonPrimitive.content, "John")
        assertEquals(
            name["encoded"]!!.jsonPrimitive.content,
            "76355713903561865866741292988746191972523015098789458240077478826513114743258",
        )
    }

    @Test @LargeTest
    fun testAutoAcceptAgentConfig() = runTest {
        aliceAgent.agentConfig.autoAcceptCredential = AutoAcceptCredential.Always
        faberAgent.agentConfig.autoAcceptCredential = AutoAcceptCredential.Always

        var faberCredentialRecord = faberAgent.credentialsV2.offerCredential(
            CreateCredentialOfferOptionsV2(
                connection = faberConnection,
                credentialDefinitionId = credDefId,
                attributes = credentialPreview.attributes,
                autoAcceptCredential = null,
                comment = "Offer to Alice",
                goal = null,
                goalCode = null,
                formats = formats,
                offerAttachments = offerAttachments,
            ),
        )

        val threadId = faberCredentialRecord.threadId
        var aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        faberCredentialRecord = getCredentialRecord(faberAgent, threadId)

        assertEquals(aliceCredentialRecord.state, CredentialState.Done)
        assertEquals(faberCredentialRecord.state, CredentialState.Done)
    }

    @Test @LargeTest
    fun testAutoAcceptOptions() = runTest {
        // Only faberAgent auto accepts.
        var faberCredentialRecord = faberAgent.credentialsV2.offerCredential(
            CreateCredentialOfferOptionsV2(
                connection = faberConnection,
                credentialDefinitionId = credDefId,
                attributes = credentialPreview.attributes,
                autoAcceptCredential = AutoAcceptCredential.Always,
                comment = "Offer to Alice",
                goal = null,
                goalCode = null,
                formats = formats,
                offerAttachments = offerAttachments,
            ),
        )

        val threadId = faberCredentialRecord.threadId
        var aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        faberCredentialRecord = getCredentialRecord(faberAgent, threadId)

        assertEquals(aliceCredentialRecord.state, CredentialState.OfferReceived)
        assertEquals(faberCredentialRecord.state, CredentialState.OfferSent)

        // aliceAgent auto accepts too.
        aliceAgent.credentialsV2.acceptOffer(
            AcceptOfferOptions(
                aliceCredentialRecord.id,
                autoAcceptCredential = AutoAcceptCredential.Always,
            ),
        )

        aliceCredentialRecord = getCredentialRecord(aliceAgent, threadId)
        faberCredentialRecord = getCredentialRecord(faberAgent, threadId)
        assertEquals(aliceCredentialRecord.state, CredentialState.Done)
        assertEquals(faberCredentialRecord.state, CredentialState.Done)
    }
}
