package soundgram.animation;

public class CharAnimData {
    public final int length;
    public final long startTime;
    public final String text;

    public CharAnimData(long j, String str, int i) {
        this.startTime = j;
        this.text = str;
        this.length = i;
    }
}
