package org.jrivets.util;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class GUID implements Serializable {

    private static final long serialVersionUID = -3429779675715864204L;

    private final long mostSigBits;

    private final long leastSigBits;

    private transient int hashCode;

    private transient String stringId;

    public GUID() {
        this(Generator.getMostSigBits(), Generator.getLeastSigBits());
    }

    public GUID(String guid) {
        guid = guid.toLowerCase();
        if (!isValid(guid)) {
            throw new IllegalArgumentException("Invalid identifier " + guid
                    + ", it must contain 32 hexadecimal digits.");
        }
        this.mostSigBits = 
                (Long.parseLong(guid.substring(0, 8), 16) << 32) +
                Long.parseLong(guid.substring(8, 16), 16);
        this.leastSigBits =                 
                (Long.parseLong(guid.substring(16, 24), 16) << 32) +
                Long.parseLong(guid.substring(24, 32), 16);
        this.stringId = guid;
    }

    public GUID(GUID guid) {
        this.mostSigBits = guid.mostSigBits;
        this.leastSigBits = guid.leastSigBits;
        this.stringId = guid.stringId;
        this.hashCode = guid.hashCode;
    }

    public GUID(long l1, long l2) {
        this.mostSigBits = l1;
        this.leastSigBits = l2;
    }
    
    public long getMSB() {
        return mostSigBits;
    }
    
    public long getLSB() {
        return leastSigBits;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GUID) {
            GUID guid = (GUID) other;
            return this.mostSigBits == guid.mostSigBits && this.leastSigBits == guid.leastSigBits;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 17 * hashCode(mostSigBits) + 31 * hashCode(leastSigBits);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (this.stringId == null) {
            this.stringId = convert(mostSigBits) + convert(leastSigBits);
        }
        return stringId;
    }

    private int hashCode(long l) {
        return (int) (l ^ (l >>> 32));
    }

    private static String convert(long l) {
        String s = Long.toHexString(l);
        if (s.length() < 16) {
            char[] zeros = new char[16 - s.length()];
            Arrays.fill(zeros, '0');
            s = new String(zeros) + s;
        }
        return s;
    }

    private static boolean isValid(String guid) {
        if (guid.length() != 32) {
            return false;
        }
        for (int idx = 0; idx < guid.length(); idx++) {
            char c = guid.charAt(idx);
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'a' && c <= 'f') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static class Generator {

        private static Generator instance = new Generator();

        private final AtomicInteger atomicInt;

        private long msb;

        private long lsb;

        private Generator() {
            SecureRandom numberGenerator = new SecureRandom(getSeed());
            byte[] randomBytes = new byte[16];
            numberGenerator.nextBytes(randomBytes);

            for (int idx = 0; idx < 8; idx++) {
                msb = (msb << 8) | (randomBytes[idx] & 0xFF);
            }
            for (int idx = 8; idx < 16; idx++) {
                lsb = (lsb << 8) | (randomBytes[idx] & 0xFF);
            }
            atomicInt = new AtomicInteger((int) (lsb & 0xFFFFFFFF));
            msb = msb&0xFFFFFFFF00000000L;
            lsb = lsb&0xFFFFFFFF00000000L;
        }

        static long getMostSigBits() {
            int timestamp = (int) (System.currentTimeMillis() >> 10);
            return instance.msb | (timestamp&0xFFFFFFFFL);
        }

        static long getLeastSigBits() {
            return instance.lsb | (instance.atomicInt.addAndGet(1)&0xFFFFFFFFL);
        }

        private static byte[] getSeed() {
            Random random = new Random();
            StringBuilder sb = new StringBuilder(128);
            addRandomInt(sb, random);
            addNetworkInterfacesDesc(sb, random);
            addProcessDesc(sb, random);
            addLoaderHash(sb);
            return sb.toString().getBytes();
        }

        private static void addRandomInt(StringBuilder sb, Random random) {
            sb.append(Integer.toHexString(random.nextInt()));
        }

        private static void addNetworkInterfacesDesc(StringBuilder sb, Random random) {
            Enumeration<NetworkInterface> interfaces = null;
            try {
                interfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException e1) {
                // oops
            }
            if (interfaces == null) {
                addRandomInt(sb, random);
                return;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                sb.append(intf.toString());
            }
        }

        private static void addProcessDesc(StringBuilder sb, Random random) {
            try {
                sb.append(java.lang.management.ManagementFactory.getRuntimeMXBean().getName());
            } catch (RuntimeException re) {
                addRandomInt(sb, random);
            }
        }

        private static void addLoaderHash(StringBuilder sb) {
            ClassLoader loader = GUID.class.getClassLoader();
            if (loader != null) {
                sb.append(Integer.toHexString(System.identityHashCode(loader)));
            }
        }
    }
}
