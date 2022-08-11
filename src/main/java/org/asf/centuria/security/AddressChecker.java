package org.asf.centuria.security;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AddressChecker {
    public static boolean isIPv4(String ipAddress) {
        boolean isIPv4 = false;
        
        if (ipAddress != null) {
            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                isIPv4 = (inetAddress instanceof Inet4Address) && inetAddress.getHostAddress().equals(ipAddress);
            } catch (UnknownHostException ex) {
            }
        }
 
        return isIPv4;
    }
    
    public static boolean isIPv6(String ipAddress) {
        boolean isIPv6 = false;
        
        if (ipAddress != null) {
            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                isIPv6 = (inetAddress instanceof Inet6Address);
            } catch (UnknownHostException ex) {
            }
        }
 
        return isIPv6;
    }
}
