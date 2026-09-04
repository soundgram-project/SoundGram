package tw.nekomimi.nekogram.config;

public class ConfigItemKeyLinked extends ConfigItem {
    public final ConfigItem keyLinked;
    public final int targetValue;

    public ConfigItemKeyLinked(String key, ConfigItem keyLinked, int targetValue, Object defaultValue) {
        super(key, configTypeBoolLinkInt, defaultValue);
        this.keyLinked = keyLinked;
        this.targetValue = targetValue;
    }

    public void changedFromKeyLinked(int intValue) {
        value = (intValue == targetValue);
    }

    @Override
    public void saveConfig() {
        if (this.type == configTypeBoolLinkInt) {
            try {
                if (keyLinked != null && (boolean) this.value) {
                    keyLinked.setConfigInt(targetValue);
                }
            } catch (Exception ignored) {}
        } else {
            super.saveConfig();
        }
    }
}
