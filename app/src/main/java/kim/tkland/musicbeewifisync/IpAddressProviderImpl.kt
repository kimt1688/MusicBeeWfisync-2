package kim.tkland.musicbeewifisync

import android.Manifest
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import java.math.BigInteger
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class IpAddressProviderImpl(context: Context, overrideSearchIP: InetAddress?) : IpAddressProvider {
    private val cnt = AtomicInteger(1)
    override val deviceAddress: InetAddress?
    override var ipSearchPrefix: String? = null

    init {
        deviceAddress = detectDeviceIP(context)
        if (overrideSearchIP != null) {
            ipSearchPrefix = getIpPrefix(overrideSearchIP)
        } else {
            ipSearchPrefix = getIpPrefix(deviceAddress)
        }
    }

    private fun getIpPrefix(deviceIP: InetAddress?): String? {
        if (deviceIP == null || deviceIP.isLoopbackAddress) {
            return null
        }
        var ipSplit = deviceIP.hostAddress!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        @Suppress("UNCHECKED_CAST")
        ipSplit = ipSplit.copyOf(3) as Array<String>
        var ipPrefix = ""
        for (ipPart in listOf<String>(*ipSplit)) {
            ipPrefix += "$ipPart."
        }
        return ipPrefix
    }

    private fun detectDeviceIP(context: Context): InetAddress? {
        //val wifiManager =
        //    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        //@Suppress("DEPRECATION") val connectionInfo = wifiManager.connectionInfo ?: return null
        //@Suppress("DEPRECATION") var ipAddress = connectionInfo.ipAddress
        var ipAddress: InetAddress? = null
        val manager: ConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties = manager.getLinkProperties(manager.activeNetwork)
        for(i in 0 until (linkProperties?.linkAddresses?.size ?:0)) {
            if (linkProperties?.linkAddresses[i]?.address is Inet4Address) {
                ipAddress = linkProperties.linkAddresses[i].address
                break
            }
        }
        return ipAddress
    }

    override fun iterator(): Iterator<InetAddress?> {
        return this
    }

    override fun hasNext(): Boolean {
        return ipSearchPrefix != null && cnt.get() <= MAX_IP_IN_SUBNET
    }

    override fun next(): InetAddress? {
        var tmp: InetAddress? = null
        try {
            tmp = InetAddress.getByName(ipSearchPrefix + cnt.getAndIncrement())
            if (tmp == deviceAddress) {
                tmp = InetAddress.getByName(ipSearchPrefix + cnt.getAndIncrement())
            }
        } catch (e: UnknownHostException) {
            // ignored as only IP format is checked in this case
        }
        return tmp
    }

    override fun remove() {this.remove()}

    companion object {
        private const val MAX_IP_IN_SUBNET = 254
    }
}