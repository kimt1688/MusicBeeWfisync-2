package kim.tkland.musicbeewifisync

import java.net.InetAddress

interface IpAddressProvider : Iterable<InetAddress?>, MutableIterator<InetAddress?> {
    val deviceAddress: InetAddress?
    val ipSearchPrefix: String?
}