package com.example.student

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chatlist.ChatListAdapter
import models.ContentModel
import network.Client
import network.EncryptionInterface
import network.EncryptionManager
import network.NetworkMessageInterface
import peerlist.PeerListAdapter
import peerlist.PeerListAdapterInterface
import wifidirect.WifiDirectInterface
import wifidirect.WifiDirectManager
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface, NetworkMessageInterface{
    private var wfdManager: WifiDirectManager? = null

    private lateinit var  studentIDEditText: EditText
    private lateinit var client: Client
    private  var encryptionManager: EncryptionManager? = null


    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasDevices = false

    private var peerListAdapter: PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null
    private var deviceIp: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)
//        client = Client(object : NetworkMessageInterface {
//            override fun onContent(content: ContentModel) {
//                // Handle the server response here
//                Log.d("CommunicationActivity", "Received from server: ${content.message} from ${content.senderIp}")
//            }
//        })
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)

        peerListAdapter = PeerListAdapter(this)
        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.adapter = peerListAdapter
        rvPeerList.layoutManager = LinearLayoutManager(this)

        studentIDEditText = findViewById(R.id.studentIDText)



//        client = Client(object : NetworkMessageInterface {
//            override fun onContent(content: ContentModel) {
//                // Handle any message received from peers
//                runOnUiThread {
//                    chatListAdapter?.addItemToEnd(content)
//                    Log.d("CommunicationActivity", "Received message: ${content.message} from IP: ${content.senderIp}")
//                }
//            }
//        })

    }
    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }

    fun discoverNearbyPeers(view: View) {
        wfdManager?.discoverPeers()
    }
    private fun updateUI() {
        val wfdAdapterErrorView: ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val rvPeerList: RecyclerView = findViewById(R.id.rvPeerListing)
        rvPeerList.visibility = if (wfdAdapterEnabled && !wfdHasConnection && hasDevices) View.VISIBLE else View.GONE

        val wfdConnectedView:ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE
    }

    fun sendMessage(view: View) {
        val etMessage:EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString()
        val content = ContentModel(etString, deviceIp)
        etMessage.text.clear()
        client?.sendMessage(content)
        chatListAdapter?.addItemToEnd(content)

    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Updated listing of nearby WiFi Direct devices", Toast.LENGTH_SHORT)
        toast.show()
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        Log.d("PeerDiscovery", "Discovered ${deviceList.size} peers")
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        if (groupInfo == null) {
            Log.d("GroupStatus", "Group is not formed")
        } else {
            Log.d("GroupStatus", "Group has been formed with group owner: ${groupInfo.owner.deviceName}")
        }
        val text = if (groupInfo == null){
            "Group is not formed"

        } else {
            "Group has been formed"
        }
        val toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
        // here should change after connection

    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
        wfdHasConnection = true
        val studentID = studentIDEditText.text.toString()
        Log.d("Host connected", "Connection between student and : ${peer.deviceName} using address ${peer.deviceAddress}")
        if(studentID.isNotBlank()){
           val content = ContentModel(studentID, peer.deviceAddress)
            client.sendMessage(content)
            Log.d("CommunicationActivity", "Sent Student ID: $studentID to peer: ${peer.deviceName} at ${peer.deviceAddress}")
        }
        else {
            val toast = Toast.makeText(this, "Student ID cannot be empty", Toast.LENGTH_SHORT)
        }
    }
    override fun onContent(content: ContentModel) {
        runOnUiThread{
            chatListAdapter?.addItemToEnd(content)
        }
    }




}