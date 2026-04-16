package tw.nekomimi.nekogram.helpers;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.TLRPC;

import java.util.Locale;

public class LocaleHelper {

    public static String getSuggestedBetaLanguageCode(String systemLang) {
        Locale systemLocale = LocaleController.getInstance().getSystemDefaultLocale();
        if (systemLocale == null) {
            return null;
        }
        String language = systemLocale.getLanguage();
        if ("ja".equals(language)) {
            return "ja_beta";
        }
        if (!"zh".equals(language) && (systemLang == null || !systemLang.toLowerCase().startsWith("zh"))) {
            return null;
        }
        if (isTraditionalChineseLocale(systemLocale, systemLang)) {
            return "zh_hant_beta";
        }
        return "zh_hans_beta";
    }

    public static boolean isTraditionalChineseLocale(Locale locale, String systemLang) {
        String script = locale.getScript();
        if ("Hant".equalsIgnoreCase(script)) {
            return true;
        }
        if ("Hans".equalsIgnoreCase(script)) {
            return false;
        }
        String country = locale.getCountry();
        if ("TW".equalsIgnoreCase(country) || "HK".equalsIgnoreCase(country) || "MO".equalsIgnoreCase(country)) {
            return true;
        }
        if ("CN".equalsIgnoreCase(country) || "SG".equalsIgnoreCase(country)) {
            return false;
        }
        String normalizedSystemLang = systemLang == null ? "" : systemLang.replace('-', '_').toLowerCase();
        return normalizedSystemLang.contains("hant") || normalizedSystemLang.endsWith("_tw") || normalizedSystemLang.endsWith("_hk") || normalizedSystemLang.endsWith("_mo");
    }

    public static LocaleController.LocaleInfo localeInfoFromLanguage(TLRPC.TL_langPackLanguage language) {
        if (language == null || TextUtils.isEmpty(language.lang_code)) {
            return null;
        }
        language.lang_code = language.lang_code.replace('-', '_').toLowerCase();
        language.plural_code = language.plural_code.replace('-', '_').toLowerCase();
        if (language.base_lang_code != null) {
            language.base_lang_code = language.base_lang_code.replace('-', '_').toLowerCase();
        }
        LocaleController.LocaleInfo info = new LocaleController.LocaleInfo();
        info.name = language.native_name;
        info.nameEnglish = language.name;
        info.shortName = language.lang_code;
        info.baseLangCode = language.base_lang_code;
        info.pluralLangCode = language.plural_code;
        info.isRtl = language.rtl;
        info.pathToFile = language.official ? "remote" : "unofficial";
        return info;
    }
}
