package soundgram.bridge;

import soundgram.zerochat.ZeroChat;

public class SoundGramChatHooks {

    public static String processOutgoingMessage(long chatId, String message) {
        if (message == null) return null;
        try {
            if (ZeroChat.isEncrypted(chatId)) {
                return ZeroChat.encrypt(chatId, message);
            }
        } catch (Exception e) {
            // Fallback to original message
        }
        return message;
    }

    public static String processIncomingMessage(long chatId, String message) {
        if (message == null) return null;
        try {
            if (ZeroChat.isEncryptedMessage(message)) {
                return ZeroChat.decrypt(chatId, message);
            }
        } catch (Exception e) {
            // Fallback to raw text if decryption fails
        }
        return message;
    }
}
