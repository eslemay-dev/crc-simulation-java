

import java.util.Scanner;

// =====================================================================
//  CRC Algoritmaları
// =====================================================================
class CRCAlgorithm {

    // CRC-16 polinom: x^16 + x^15 + x^2 + 1 = 0x8005
    private static final int CRC16_POLYNOMIAL = 0x8005;

    // CRC-CCITT polinom: x^16 + x^12 + x^5 + 1 = 0x1021
    private static final int CRC_CCITT_POLYNOMIAL = 0x1021;
    //Bunlar sabit polinom değerleri. static final = değişmez, her yerden erişilebilir.
    /**
     * CRC-16 hesaplar (IBM standardı)
     */
    public static int calculateCRC16(int data) {
        int crc = 0x0000; // Başlangıç değeri

        for (int i = 15; i >= 0; i--) {
            int bit = (data >> i) & 1;
            int msb = (crc >> 15) & 1;
            crc = (crc << 1) & 0xFFFF;
            crc |= bit;

            if (msb == 1) {
                crc ^= CRC16_POLYNOMIAL;
            }
        }

        // Son 16 bit bölme
        for (int i = 0; i < 16; i++) {
            int msb = (crc >> 15) & 1;
            crc = (crc << 1) & 0xFFFF;
            if (msb == 1) {
                crc ^= CRC16_POLYNOMIAL;
            }
        }

        return crc & 0xFFFF;
    }

    /**
     * CRC-CCITT hesaplar (ITU standardı, Bluetooth, X.25'te kullanılır)
     */
    public static int calculateCRCCCITT(int data) {
        int crc = 0xFFFF; // Başlangıç değeri 0xFFFF

        for (int i = 15; i >= 0; i--) {
            int bit = (data >> i) & 1;
            int msb = (crc >> 15) & 1;
            crc = (crc << 1) & 0xFFFF;

            if (msb != bit) {
                crc ^= CRC_CCITT_POLYNOMIAL;
            }
        }

        return crc & 0xFFFF;
    }

    /**
     * Veriyi 16-bit binary stringe çevirir
     */
    public static String toBinary16(int value) {
        String bin = Integer.toBinaryString(value & 0xFFFF);
        // 16 bite tamamla
        while (bin.length() < 16) {
            bin = "0" + bin;
        }
        return bin;
    }
}

// =====================================================================
//  Cihaz A - Gönderici
// =====================================================================
class DeviceA {
    private String name;
    private int originalData;
    private int crc16;
    private int crcCCITT;

    public DeviceA(String name, int data) {
        this.name = name;
        this.originalData = data & 0xFFFF; // 16-bit'e kır
        this.crc16 = CRCAlgorithm.calculateCRC16(originalData);
        this.crcCCITT = CRCAlgorithm.calculateCRCCCITT(originalData);
    }

    public void displayInfo() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║            CİHAZ A (GÖNDERİCİ)                 ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  Cihaz Adı   : %s%n", name);
        System.out.printf("  Orijinal Veri (decimal) : %d%n", originalData);
        System.out.printf("  Orijinal Veri (hex)     : 0x%04X%n", originalData);
        System.out.printf("  Orijinal Veri (binary)  : %s%n", CRCAlgorithm.toBinary16(originalData));
        System.out.println();
        System.out.printf("  [CRC-16]    CRC değeri  : 0x%04X (%s)%n",
                crc16, CRCAlgorithm.toBinary16(crc16));
        System.out.printf("  [CRC-CCITT] CRC değeri  : 0x%04X (%s)%n",
                crcCCITT, CRCAlgorithm.toBinary16(crcCCITT));
    }

    public int getOriginalData() { return originalData; }
    public int getCRC16() { return crc16; }
    public int getCRCCCITT() { return crcCCITT; }
    public String getName() { return name; }
}

// =====================================================================
//  Cihaz B - Alıcı
// =====================================================================
class DeviceB {
    private String name;

    public DeviceB(String name) {
        this.name = name;
    }

    /**
     * Gelen veriyi doğrular
     * @param receivedData  alınan veri
     * @param receivedCRC16 gönderilen CRC-16 değeri
     * @param receivedCCITT gönderilen CRC-CCITT değeri
     * @param simulateError true ise veriyi boz (hata simülasyonu)
     */
    public void receiveAndVerify(int receivedData, int receivedCRC16, int receivedCCITT, boolean simulateError) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║              CİHAZ B (ALICI)                   ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  Cihaz Adı : %s%n", name);

        // Hata simülasyonu: veriyi boz
        if (simulateError) {
            receivedData ^= 0b0000000000000001; // Son biti değiştir (bozulma)
            System.out.println("   UYARI: Kanal hatası simüle edildi! 1 bit bozuldu.");
        }

        System.out.printf("  Alınan Veri (binary)    : %s%n", CRCAlgorithm.toBinary16(receivedData));
        System.out.printf("  Alınan Veri (hex)       : 0x%04X%n", receivedData);

        // Alıcı tarafında CRC yeniden hesaplanır
        int computedCRC16   = CRCAlgorithm.calculateCRC16(receivedData);
        int computedCCITT   = CRCAlgorithm.calculateCRCCCITT(receivedData);

        System.out.println();
        System.out.println("  ──── CRC-16 Doğrulama ────");
        System.out.printf("  Gelen  CRC-16 : 0x%04X%n", receivedCRC16);
        System.out.printf("  Hesaplanan    : 0x%04X%n", computedCRC16);

        boolean crc16OK = (receivedCRC16 == computedCRC16);
        System.out.printf("  Sonuç         : %s%n", crc16OK
                ? " VERİ ORİJİNAL - Bütünlük sağlandı"
                : "HATA TESPİT EDİLDİ - Veri bozulmuş!");

        System.out.println();
        System.out.println("  ──── CRC-CCITT Doğrulama ────");
        System.out.printf("  Gelen  CRC-CCITT : 0x%04X%n", receivedCCITT);
        System.out.printf("  Hesaplanan       : 0x%04X%n", computedCCITT);

        boolean ccittOK = (receivedCCITT == computedCCITT);
        System.out.printf("  Sonuç            : %s%n", ccittOK
                ? "VERİ ORİJİNAL - Bütünlük sağlandı"
                : "HATA TESPİT EDİLDİ - Veri bozulmuş!");

        System.out.println();
        System.out.println("  ──── Algoritma Karşılaştırması ────");
        if (crc16OK == ccittOK) {
            System.out.println("  Her iki algoritma aynı sonucu verdi → Tutarlı");
        } else {
            System.out.println("  Algoritmalar farklı sonuç verdi → Dikkat!");
        }
    }
}

// =====================================================================
//  Ana Sınıf - Simülasyonu Çalıştırır
// =====================================================================
public class CRCSimulation {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     CRC SİMÜLASYONU - CRC-16 vs CRC-CCITT       ║");
        System.out.println("║         İki Cihaz Arası Veri Doğrulama           ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");

        // Kullanıcıdan 16-bit veri al
        System.out.print("\n16-bit veri girin (0–65535 arası bir sayı): ");
        int userData = 0;
        try {
            userData = Integer.parseInt(scanner.nextLine().trim());
            if (userData < 0 || userData > 65535) {
                System.out.println("Geçersiz aralık! Varsayılan değer kullanılıyor: 43981");
                userData = 43981; // 0xABCD
            }
        } catch (NumberFormatException e) {
            System.out.println("Sayı formatı hatalı! Varsayılan değer kullanılıyor: 43981");
            userData = 43981;
        }

        // Cihaz A (gönderici) oluştur ve veriyi hazırla
        DeviceA sender   = new DeviceA("Laptop (Cihaz A)", userData);
        DeviceB receiver = new DeviceB("Router (Cihaz B)");

        sender.displayInfo();

        System.out.println("\n┌──────────────────────────────────────────────────┐");
        System.out.println("│    İletişim Kanalı üzerinden veri gönderiliyor  │");
        System.out.println("└──────────────────────────────────────────────────┘");

        // ── Senaryo 1: Hatasız iletim ──────────────────────────────
        System.out.println("\n━━━━ SENARYO 1: Hatasız İletim ━━━━");
        receiver.receiveAndVerify(
                sender.getOriginalData(),
                sender.getCRC16(),
                sender.getCRCCCITT(),
                false   // hata yok
        );

        // ── Senaryo 2: Hatalı iletim (1 bit bozuldu) ───────────────
        System.out.println("\n━━━━ SENARYO 2: Hatalı İletim (1 bit bozuldu) ━━━━");
        receiver.receiveAndVerify(
                sender.getOriginalData(),
                sender.getCRC16(),
                sender.getCRCCCITT(),
                true    // hata simüle et
        );

        // ── Özet ───────────────────────────────────────────────────
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║                    ÖZET                        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  Test edilen veri : 0x%04X (%s)%n",
                userData, CRCAlgorithm.toBinary16(userData));
        System.out.printf("  CRC-16 değeri    : 0x%04X%n", sender.getCRC16());
        System.out.printf("  CRC-CCITT değeri : 0x%04X%n", sender.getCRCCCITT());
        System.out.println();
        System.out.println("  CRC-16   : Endüstriyel uygulamalar, USB, Modbus");
        System.out.println("  CRC-CCITT: Bluetooth, X.25, SD kartlar, HDLC");
        System.out.println("  Her iki algoritma da tek bit hatayı yakalar.");
        System.out.println("\n  Simülasyon tamamlandı.");

        scanner.close();
    }
}