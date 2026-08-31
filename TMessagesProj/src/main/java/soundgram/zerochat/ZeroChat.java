package soundgram.zerochat;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

import soundgram.music.SoundGramConfig;

public class ZeroChat {
    // Невидимые маркеры
    public static final String MARKER = "\u200D\u200D\u200D"; 
    private static final char CHAR_0 = '\u200B'; 
    private static final char CHAR_1 = '\u200C'; 

    public static boolean isEncrypted(long chatId) {
        return SoundGramConfig.activeZeroChatKey != null;
    }

    public static boolean isEncryptedMessage(String message) {
        return message != null && message.contains(MARKER);
    }

    public static String encrypt(long chatId, String message) {
        String activeKey = SoundGramConfig.activeZeroChatKey;
        if (activeKey != null) {
            return encryptAndEmbed(message, message, activeKey);
        }
        return message;
    }

    public static String decrypt(long chatId, String message) {
        CharSequence decrypted = decrypt((CharSequence) message);
        return decrypted != null ? decrypted.toString() : message;
    }

    public static String encryptAndEmbed(String publicText, String secretText, String password) {
        try {
            byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8")), 32);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(secretText.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder(publicText);
            sb.append(MARKER);
            for (byte b : encrypted) {
                for (int i = 0; i < 8; i++) {
                    sb.append(((b >> i) & 1) == 1 ? CHAR_1 : CHAR_0);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return publicText;
        }
    }

    public static CharSequence decrypt(CharSequence text) {
        if (TextUtils.isEmpty(text)) return text;
        String textStr = text.toString();
        if (!textStr.contains(MARKER)) return text;

        String[] parts = textStr.split(MARKER);
        CharSequence publicText = text.subSequence(0, parts[0].length());
        if (parts.length < 2) return publicText;
        
        String invisiblePart = parts[1];
        
        try {
            byte[] encrypted = new byte[invisiblePart.length() / 8];
            for (int i = 0; i < encrypted.length; i++) {
                byte b = 0;
                for (int j = 0; j < 8; j++) {
                    if (invisiblePart.charAt(i * 8 + j) == CHAR_1) b |= (1 << j);
                }
                encrypted[i] = b;
            }

            for (String pass : SoundGramConfig.zeroChatKeys.values()) {
                try {
                    byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(pass.getBytes("UTF-8")), 32);
                    SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    
                    String secretText = new String(cipher.doFinal(encrypted), "UTF-8");
                    
                    SpannableStringBuilder ssb = new SpannableStringBuilder(publicText);
                    ssb.append("\n\n🔐 ").append(secretText);
                    return ssb;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        
        return publicText; // Если нет ключа, отдаем публичный текст без шифра
    }
}