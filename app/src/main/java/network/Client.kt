package network

import android.util.Log
import models.ContentModel
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread

class Client (private val networkMessageInterface: NetworkMessageInterface){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var ip:String = ""



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
                        networkMessageInterface.onContent(serverContent)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }
    fun sendStudentID(studentID: String){
        thread {
            if (!clientSocket.isConnected) {
                Log.e("CLIENT", "We aren't currently connected to the server!")
                return@thread
            }
            try {
                val content = ContentModel(studentID, ip)
                val contentAsStr: String = Gson().toJson(content)
                writer.write("$contentAsStr\n")
                writer.flush()
            } catch (e: Exception) {
                Log.e("CLIENT", "Error sending student ID")
                e.printStackTrace()
            }
        }
    }
    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun close(){
        clientSocket.close()
    }
}