package org.emulinker.kaillera.pico

import com.google.common.truth.Truth.assertThat
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import org.emulinker.eval.client.EvalClient
import org.emulinker.kaillera.model.GameStatus
import org.emulinker.kaillera.model.UserStatus
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class ServerMainStartupTest {
  @get:Rule val timeout = Timeout(1.minutes.inWholeMilliseconds.toInt())

  @Test
  fun startup() =
      runBlocking(Dispatchers.IO) {
        val component = DaggerAppComponent.create()

        val kailleraServerControllerTask =
            launch { component.kailleraServerController.start() } // Apparently cannot be removed.
        val serverTask =
            launch { component.server.start(component.udpSocketProvider, coroutineContext) }

        // Make sure it stays alive for 3 seconds.
        delay(3.seconds)

        // Stop the server.
        component.kailleraServerController.stop()
        component.server.stop()
        delay(1.seconds)

        // Make sure that the coroutines for those tasks were successful.
        assertThat(kailleraServerControllerTask.isCompleted).isTrue()
        assertThat(serverTask.isCompleted).isTrue()
      }

  @Test
  fun `create game`() =
      runBlocking(Dispatchers.IO) {
        val component = DaggerAppComponent.create()

        val server = component.server

        launch { component.kailleraServerController.start() } // Apparently cannot be removed.
        launch { server.start(component.udpSocketProvider, coroutineContext) }

        delay(20.milliseconds)

        val user1 = EvalClient("testuser1", InetSocketAddress("127.0.0.1", 27888))

        user1.connectToDedicatedPort()
        user1.start()

        val controller = server.controllers.first()
        val clientHandler = controller.clientHandlers.values.single()

        user1.createGame()

        assertThat(clientHandler.user.status).isEqualTo(UserStatus.IDLE)
        assertThat(clientHandler.user.game).isNotNull()
        assertThat(clientHandler.user.game!!.status == GameStatus.WAITING)

        user1.quitGame()
        user1.quitServer()

        assertThat(controller.clientHandlers).isEmpty()

        // Clean up.
        user1.close()
        component.kailleraServerController.stop()
        component.server.stop()
      }

  @Test
  fun `create 50 games`() =
      runBlocking(Dispatchers.IO) {
        val component = DaggerAppComponent.create()

        val server = component.server

        launch { component.kailleraServerController.start() } // Apparently cannot be removed.
        launch { server.start(component.udpSocketProvider, coroutineContext) }

        delay(50.milliseconds)

        val connectAddress = InetSocketAddress("127.0.0.1", 27888)

        val numUsers = 30
        val users = (1..numUsers).map { EvalClient("testuser$it", connectAddress) }

        // Wrap this in a coroutineScope so that we wait for all of it to finish before proceeding.
        coroutineScope {
          users.forEach { user ->
            launch {
              user.connectToDedicatedPort()
              user.start()
            }
          }
        }

        delay(3.seconds)

        coroutineScope { users.forEach { launch { it.createGame() } } }

        delay(3.seconds)

        val clientHandlers = server.controllers.first().clientHandlers
        assertThat(clientHandlers.size).isEqualTo(numUsers)
        clientHandlers.values.forEach { assertThat(it.user.game).isNotNull() }

        // Clean up.
        users.forEach { it.close() }
        component.kailleraServerController.stop()
        component.server.stop()
      }
}
