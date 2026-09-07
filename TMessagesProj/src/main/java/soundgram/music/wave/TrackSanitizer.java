package soundgram.music.wave;

import java.security.MessageDigest;
import java.util.regex.Pattern;

public final class TrackSanitizer {

    private static final Pattern JUNK_REGEX = Pattern.compile(
            "(?i)\\s*(\\(|\\[)(official\\s*(audio|video|music\\s*video|lyric\\s*video)?|remastered(\\s*\\d{4})?|audio|video|lyrics|hd|4k)(\\)|\\])"
    );
    private static final Pattern FEAT_REGEX = Pattern.compile(
            "(?i)\\s*(feat\\.|ft\\.|featuring)\\s+.*"
    );
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s+");

    private TrackSanitizer() {}

    public static String cleanTitle(String rawTitle) {
        if (rawTitle == null) return "";
        String cleaned = rawTitle;
        int bulletIdx = cleaned.indexOf("•");
        if (bulletIdx >= 0) {
            cleaned = cleaned.substring(0, bulletIdx);
        }
        int bulletUnicode = cleaned.indexOf("\u2022");
        if (bulletUnicode >= 0) {
            cleaned = cleaned.substring(0, bulletUnicode);
        }
        cleaned = JUNK_REGEX.matcher(cleaned).replaceAll("");
        cleaned = FEAT_REGEX.matcher(cleaned).replaceAll("");
        cleaned = EXTRA_SPACES.matcher(cleaned).replaceAll(" ");
        return cleaned.trim();
    }

    public static String cleanArtist(String rawArtist) {
        if (rawArtist == null) return "";
        String cleaned = rawArtist;
        int bulletIdx = cleaned.indexOf("•");
        if (bulletIdx >= 0) {
            cleaned = cleaned.substring(0, bulletIdx);
        }
        int bulletUnicode = cleaned.indexOf("\u2022");
        if (bulletUnicode >= 0) {
            cleaned = cleaned.substring(0, bulletUnicode);
        }
        cleaned = FEAT_REGEX.matcher(cleaned).replaceAll("");
        cleaned = EXTRA_SPACES.matcher(cleaned).replaceAll(" ");
        return cleaned.trim();
    }

    public static String generateHash(String artist, String title) {
        String normArtist = cleanArtist(artist).toLowerCase().trim();
        String normTitle = cleanTitle(title).toLowerCase().trim();
        String combined = normArtist + "_" + normTitle;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(combined.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(combined.hashCode());
        }
    }
}
