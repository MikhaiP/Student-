package network

import android.util.Log
import models.ContentModel
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread

class Client (private val networkMessageInterface: NetworkMessageInterface, private  val encryptionManager: EncryptionManager){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var ip:String = ""
    private val seed = "SharedSecretSeed"



    init {
        thread {
            clientSocket = Socket("192.168.49.1", 9999)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!
            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)

                        val aesKey = encryptionManager.generateAESKey(seed)
                        val aesIv = encryptionManager.generateIV(seed)
                        val decryptedMessage = encryptionManager.decryptMessage(serverContent.message, aesKey,aesIv)

                        val decryptedContent = serverContent.copy(message = decryptedMessage)
                        networkMessageInterface.onContent(decryptedContent)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }
    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val aesKey = encryptionManager.generateAESKey(seed)
            val aesIv = encryptionManager.generateIV(seed)

            val encryptedMessage = encryptionManager.encryptMessage(content.message,aesKey,aesIv)

            val encryptedContent = ContentModel(encryptedMessage, content.senderIp)
            val contentAsStr:String = Gson().toJson(encryptedContent)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun close(){
        clientSocket.close()
    }
}