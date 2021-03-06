package org.atto.protocol.network.codec.peer

import org.atto.protocol.network.AttoContextHolder
import org.atto.protocol.network.codec.peer.handshake.AttoHandshakeChallengeCodec
import org.atto.protocol.network.handshake.AttoHandshakeChallenge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.random.Random

internal class AttoAttoHandshakeChallengeCodecTest {
    val codec = AttoHandshakeChallengeCodec()

    @Test
    fun `should serialize and deserialize`() {
        // given
        val socketAddress = InetSocketAddress(InetAddress.getLocalHost(), 8330)
        AttoContextHolder.put("socketAddress", socketAddress)

        val expectedHandshakeChallenge = AttoHandshakeChallenge(
            value = Random.nextBytes(ByteArray(16))
        )

        // when
        val byteBuffer = codec.toByteBuffer(expectedHandshakeChallenge)
        val handshakeChallenge = codec.fromByteBuffer(byteBuffer)

        // then
        assertEquals(expectedHandshakeChallenge, handshakeChallenge)
    }
}