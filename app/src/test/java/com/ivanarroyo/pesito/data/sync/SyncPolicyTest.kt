package com.ivanarroyo.pesito.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SyncPolicyTest {

    @Test
    fun `success is success`() =
        assertEquals(SyncOutcome.SUCCESS, SyncPolicy.decide(Result.success(Unit), runAttemptCount = 0))

    @Test
    fun `no internet retries`() =
        assertEquals(SyncOutcome.RETRY, SyncPolicy.decide(Result.failure(IOException()), runAttemptCount = 0))

    @Test
    fun `no internet after three attempts gives up until next time`() =
        assertEquals(
            SyncOutcome.FAILURE,
            SyncPolicy.decide(Result.failure(IOException()), runAttemptCount = SyncPolicy.MAX_ATTEMPTS),
        )

    @Test
    fun `other errors do not retry`() =
        assertEquals(SyncOutcome.FAILURE, SyncPolicy.decide(Result.failure(IllegalStateException()), runAttemptCount = 0))
}
