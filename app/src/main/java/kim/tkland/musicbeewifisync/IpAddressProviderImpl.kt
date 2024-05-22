package kim.tkland.musicbeewifisync

import android.content.Context
import android.net.wifi.WifiManager
import java.math.BigInteger
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
        ipSplit = Arrays.copyOf(ipSplit, 3)
        var ipPrefix = ""
        for (ipPart in Arrays.asList<String>(*ipSplit)) {
            ipPrefix += "$ipPart."
        }
        return ipPrefix
    }

    private fun detectDeviceIP(context: Context): InetAddress? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val connectionInfo = wifiManager.connectionInfo ?: return null
        @Suppress("DEPRECATION") var ipAddress = connectionInfo.ipAddress
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }
        val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
        return try {
            InetAddress.getByAddress(ipByteArray)
        } catch (e: UnknownHostException) {
            null
        }
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