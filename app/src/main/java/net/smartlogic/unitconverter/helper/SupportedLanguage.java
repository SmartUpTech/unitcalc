package net.smartlogic.unitconverter.helper;

import net.smartlogic.unitconverter.R;
import java.util.List;
import java.util.Locale;

public final class SupportedLanguage {
    public final String tag;
    public final String language;
    public final int nativeNameResource;
    public final String suggestedRegion;
    private SupportedLanguage(String tag, int label, String region) {
        this.tag = tag;
        language = Locale.forLanguageTag(tag).getLanguage();
        nativeNameResource = label;
        suggestedRegion = region;
    }
    public static final List<SupportedLanguage> ALL = List.of(
            new SupportedLanguage("en-US", R.string.language_en, "US"),
            new SupportedLanguage("en-GB", R.string.language_en_gb, "GB"),
            new SupportedLanguage("en-CA", R.string.language_en_ca, "CA"),
            new SupportedLanguage("en-AU", R.string.language_en_au, "AU"),
            new SupportedLanguage("hi-IN", R.string.language_hi, "IN"),
            new SupportedLanguage("es-ES", R.string.language_es, "ES"),
            new SupportedLanguage("es-MX", R.string.language_es_mx, "MX"),
            new SupportedLanguage("fr-FR", R.string.language_fr, "FR"),
            new SupportedLanguage("pt-BR", R.string.language_pt, "BR"),
            new SupportedLanguage("ru-RU", R.string.language_ru, "RU"),
            new SupportedLanguage("id-ID", R.string.language_id, "ID"),
            new SupportedLanguage("de-DE", R.string.language_de, "DE"),
            new SupportedLanguage("tr-TR", R.string.language_tr, "TR"),
            new SupportedLanguage("vi-VN", R.string.language_vi, "VN"),
            new SupportedLanguage("it-IT", R.string.language_it, "IT"),
            new SupportedLanguage("pl-PL", R.string.language_pl, "PL"),
            new SupportedLanguage("nl-NL", R.string.language_nl, "NL"),
            new SupportedLanguage("en-IN", R.string.language_en_in, "IN"),
            new SupportedLanguage("en-IE", R.string.language_en_ie, "IE"),
            new SupportedLanguage("en-NZ", R.string.language_en_nz, "NZ"),
            new SupportedLanguage("en-SG", R.string.language_en_sg, "SG"),
            new SupportedLanguage("en-ZA", R.string.language_en_za, "ZA"),
            new SupportedLanguage("es-AR", R.string.language_es_ar, "AR"),
            new SupportedLanguage("es-CL", R.string.language_es_cl, "CL"),
            new SupportedLanguage("es-CO", R.string.language_es_co, "CO"),
            new SupportedLanguage("es-PE", R.string.language_es_pe, "PE"),
            new SupportedLanguage("es-US", R.string.language_es_us, "US"),
            new SupportedLanguage("fr-CA", R.string.language_fr_ca, "CA"),
            new SupportedLanguage("fr-BE", R.string.language_fr_be, "BE"),
            new SupportedLanguage("fr-CH", R.string.language_fr_ch, "CH"),
            new SupportedLanguage("pt-PT", R.string.language_pt_pt, "PT"),
            new SupportedLanguage("de-AT", R.string.language_de_at, "AT"),
            new SupportedLanguage("de-CH", R.string.language_de_ch, "CH"),
            new SupportedLanguage("it-CH", R.string.language_it_ch, "CH"),
            new SupportedLanguage("nl-BE", R.string.language_nl_be, "BE"));

    public static SupportedLanguage resolve(String tag) {
        for (SupportedLanguage item : ALL) if (item.tag.equalsIgnoreCase(tag)) return item;
        String language = Locale.forLanguageTag(tag).getLanguage();
        for (SupportedLanguage item : ALL) if (item.language.equals(language)) return item;
        return ALL.get(0);
    }
}
