package moe.feng.aoba.api

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object RemoteYeelightApi {

	private lateinit var serverSocket: ServerSocket

	private var socket: Socket? = null
	private var reader: BufferedReader? = null

	fun startListening() {
		serverSocket = ServerSocket(1000)
		thread {
			while (true) {
				socket = serverSocket.accept()
				reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
			}
		}
		thread {
			while (true) {
				Thread.sleep(1000)
				if (socket?.isClosed == true) {
					socket = null
				}
			}
		}
	}

	fun isOnline() = socket?.isClosed == false

	fun toggle() : String? {
		socket?.let {
			val outputStream = it.getOutputStream()
			outputStream.write("toggle\r\n".toByteArray())
			outputStream.flush()
		}
		return reader?.readLine()
	}

}