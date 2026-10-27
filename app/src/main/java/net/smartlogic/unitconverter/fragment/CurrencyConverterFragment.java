package net.smartlogic.unitconverter.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.graphy.integration.ConversionGraphAdapter;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.helper.HttpHandler;
import net.smartlogic.unitconverter.helper.IdleHistoryRecorder;
import net.smartlogic.unitconverter.model.CalculatorCatalog;
import net.smartlogic.unitconverter.helper.CurrencyRateStore;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.model.Currency;
import net.smartlogic.unitconverter.utils.GenericFunctions;
import net.smartlogic.unitconverter.utils.NumberUtils;
import net.smartlogic.unitconverter.utils.DecimalMath;
import java.math.BigDecimal;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class CurrencyConverterFragment extends Fragment implements OnClickListener, OnLongClickListener {

    private static final ExecutorService RATE_EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private IdleHistoryRecorder historyRecorder;
    private boolean refreshing;
    private int viewGeneration;
    private CurrencyRateStore.Snapshot rateSnapshot;

    private EditText inputValue, outputValue;
    private TextView inputSymbol, outputSymbol;
    private Context context;
    private HashMap<String, BigDecimal> currencyRates;
    private Preferences prefs;
    private TextView fromCurrency, fromCurrencyISO, toCurrency, toCurrencyISO, txtRate;
    private TextView lastRefreshTime;
    private ImageView fromFlag, toFlag, refresh;
    private LinearLayout llMain;
    private RelativeLayout rlFromUnit, rlToUnit;
    private ArrayList<Currency> currencies;

    public CurrencyConverterFragment() {
        // Required empty public constructor
    }

    public static CurrencyConverterFragment newInstance() {
        CurrencyConverterFragment fragment = new CurrencyConverterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void initCurrencies() {
        currencies = new ArrayList<>();

        currencies.add(new Currency(getString(R.string.currency_name_aud), getString(R.string.currency_code_aud), getString(R.string.currency_country_aud), getString(R.string.currency_symbol_aud), R.drawable.au));
        currencies.add(new Currency(getString(R.string.currency_name_brl), getString(R.string.currency_code_brl), getString(R.string.currency_country_brl), getString(R.string.currency_symbol_brl), R.drawable.br));
        currencies.add(new Currency(getString(R.string.currency_name_eur_bg), getString(R.string.currency_code_eur_bg), getString(R.string.currency_country_eur_bg), getString(R.string.currency_symbol_eur_bg), R.drawable.bg));
        currencies.add(new Currency(getString(R.string.currency_name_cad), getString(R.string.currency_code_cad), getString(R.string.currency_country_cad), getString(R.string.currency_symbol_cad), R.drawable.ca));
        currencies.add(new Currency(getString(R.string.currency_name_cny), getString(R.string.currency_code_cny), getString(R.string.currency_country_cny), getString(R.string.currency_symbol_cny), R.drawable.cn));
        currencies.add(new Currency(getString(R.string.currency_name_eur_hr), getString(R.string.currency_code_eur_hr), getString(R.string.currency_country_eur_hr), getString(R.string.currency_symbol_eur_hr), R.drawable.hr));
        currencies.add(new Currency(getString(R.string.currency_name_czk), getString(R.string.currency_code_czk), getString(R.string.currency_country_czk), getString(R.string.currency_symbol_czk), R.drawable.cz));
        currencies.add(new Currency(getString(R.string.currency_name_dkk), getString(R.string.currency_code_dkk), getString(R.string.currency_country_dkk), getString(R.string.currency_symbol_dkk), R.drawable.dk));
        currencies.add(new Currency(getString(R.string.currency_name_hkd), getString(R.string.currency_code_hkd), getString(R.string.currency_country_hkd), getString(R.string.currency_symbol_hkd), R.drawable.hk));
        currencies.add(new Currency(getString(R.string.currency_name_huf), getString(R.string.currency_code_huf), getString(R.string.currency_country_huf), getString(R.string.currency_symbol_huf), R.drawable.hu));
        currencies.add(new Currency(getString(R.string.currency_name_isk), getString(R.string.currency_code_isk), getString(R.string.currency_country_isk), getString(R.string.currency_symbol_isk), R.drawable.is));
        currencies.add(new Currency(getString(R.string.currency_name_inr), getString(R.string.currency_code_inr), getString(R.string.currency_country_inr), getString(R.string.currency_symbol_inr), R.drawable.in));
        currencies.add(new Currency(getString(R.string.currency_name_idr), getString(R.string.currency_code_idr), getString(R.string.currency_country_idr), getString(R.string.currency_symbol_idr), R.drawable.id));
        currencies.add(new Currency(getString(R.string.currency_name_ils), getString(R.string.currency_code_ils), getString(R.string.currency_country_ils), getString(R.string.currency_symbol_ils), R.drawable.il));
        currencies.add(new Currency(getString(R.string.currency_name_jpy), getString(R.string.currency_code_jpy), getString(R.string.currency_country_jpy), getString(R.string.currency_symbol_jpy), R.drawable.jp));
        currencies.add(new Currency(getString(R.string.currency_name_krw), getString(R.string.currency_code_krw), getString(R.string.currency_country_krw), getString(R.string.currency_symbol_krw), R.drawable.kr));
        currencies.add(new Currency(getString(R.string.currency_name_myr), getString(R.string.currency_code_myr), getString(R.string.currency_country_myr), getString(R.string.currency_symbol_myr), R.drawable.my));
        currencies.add(new Currency(getString(R.string.currency_name_mxn), getString(R.string.currency_code_mxn), getString(R.string.currency_country_mxn), getString(R.string.currency_symbol_mxn), R.drawable.mx));
        currencies.add(new Currency(getString(R.string.currency_name_nzd), getString(R.string.currency_code_nzd), getString(R.string.currency_country_nzd), getString(R.string.currency_symbol_nzd), R.drawable.nz));
        currencies.add(new Currency(getString(R.string.currency_name_nok), getString(R.string.currency_code_nok), getString(R.string.currency_country_nok), getString(R.string.currency_symbol_nok), R.drawable.no));
        currencies.add(new Currency(getString(R.string.currency_name_php), getString(R.string.currency_code_php), getString(R.string.currency_country_php), getString(R.string.currency_symbol_php), R.drawable.ph));
        currencies.add(new Currency(getString(R.string.currency_name_pln), getString(R.string.currency_code_pln), getString(R.string.currency_country_pln), getString(R.string.currency_symbol_pln), R.drawable.pl));
        currencies.add(new Currency(getString(R.string.currency_name_ron), getString(R.string.currency_code_ron), getString(R.string.currency_country_ron), getString(R.string.currency_symbol_ron), R.drawable.ro));
        currencies.add(new Currency(getString(R.string.currency_name_rub), getString(R.string.currency_code_rub), getString(R.string.currency_country_rub), getString(R.string.currency_symbol_rub), R.drawable.ru));
        currencies.add(new Currency(getString(R.string.currency_name_sgd), getString(R.string.currency_code_sgd), getString(R.string.currency_country_sgd), getString(R.string.currency_symbol_sgd), R.drawable.sg));
        currencies.add(new Currency(getString(R.string.currency_name_zar), getString(R.string.currency_code_zar), getString(R.string.currency_country_zar), getString(R.string.currency_symbol_zar), R.drawable.za));
        currencies.add(new Currency(getString(R.string.currency_name_sek), getString(R.string.currency_code_sek), getString(R.string.currency_country_sek), getString(R.string.currency_symbol_sek), R.drawable.se));
        currencies.add(new Currency(getString(R.string.currency_name_chf), getString(R.string.currency_code_chf), getString(R.string.currency_country_chf), getString(R.string.currency_symbol_chf), R.drawable.ch));
        currencies.add(new Currency(getString(R.string.currency_name_thb), getString(R.string.currency_code_thb), getString(R.string.currency_country_thb), getString(R.string.currency_symbol_thb), R.drawable.th));
        currencies.add(new Currency(getString(R.string.currency_name_try), getString(R.string.currency_code_try), getString(R.string.currency_country_try), getString(R.string.currency_symbol_try), R.drawable.tr));
        currencies.add(new Currency(getString(R.string.currency_name_gbp), getString(R.string.currency_code_gbp), getString(R.string.currency_country_gbp), getString(R.string.currency_symbol_gbp), R.drawable.gb));
        currencies.add(new Currency(getString(R.string.currency_name_usd), getString(R.string.currency_code_usd), getString(R.string.currency_country_usd), getString(R.string.currency_symbol_usd), R.drawable.us));
        currencies.add(new Currency(getString(R.string.currency_name_eur), getString(R.string.currency_code_eur), getString(R.string.currency_country_eur), getString(R.string.currency_symbol_eur), R.drawable.de));

        // Append only: preferences persist positions. Legacy BGN/HRK positions now use EUR.
        currencies.add(new Currency(getString(R.string.currency_name_aed), getString(R.string.currency_code_aed), getString(R.string.currency_country_aed), getString(R.string.currency_symbol_aed), R.drawable.ae));
        currencies.add(new Currency(getString(R.string.currency_name_amd), getString(R.string.currency_code_amd), getString(R.string.currency_country_amd), getString(R.string.currency_symbol_amd), R.drawable.am));
        currencies.add(new Currency(getString(R.string.currency_name_azn), getString(R.string.currency_code_azn), getString(R.string.currency_country_azn), getString(R.string.currency_symbol_azn), R.drawable.az));
        currencies.add(new Currency(getString(R.string.currency_name_bdt), getString(R.string.currency_code_bdt), getString(R.string.currency_country_bdt), getString(R.string.currency_symbol_bdt), R.drawable.bd));
        currencies.add(new Currency(getString(R.string.currency_name_bhd), getString(R.string.currency_code_bhd), getString(R.string.currency_country_bhd), getString(R.string.currency_symbol_bhd), R.drawable.bh));
        currencies.add(new Currency(getString(R.string.currency_name_btn), getString(R.string.currency_code_btn), getString(R.string.currency_country_btn), getString(R.string.currency_symbol_btn), R.drawable.bt));
        currencies.add(new Currency(getString(R.string.currency_name_gel), getString(R.string.currency_code_gel), getString(R.string.currency_country_gel), getString(R.string.currency_symbol_gel), R.drawable.ge));
        currencies.add(new Currency(getString(R.string.currency_name_iqd), getString(R.string.currency_code_iqd), getString(R.string.currency_country_iqd), getString(R.string.currency_symbol_iqd), R.drawable.iq));
        currencies.add(new Currency(getString(R.string.currency_name_jod), getString(R.string.currency_code_jod), getString(R.string.currency_country_jod), getString(R.string.currency_symbol_jod), R.drawable.jo));
        currencies.add(new Currency(getString(R.string.currency_name_kgs), getString(R.string.currency_code_kgs), getString(R.string.currency_country_kgs), getString(R.string.currency_symbol_kgs), R.drawable.kg));
        currencies.add(new Currency(getString(R.string.currency_name_khr), getString(R.string.currency_code_khr), getString(R.string.currency_country_khr), getString(R.string.currency_symbol_khr), R.drawable.kh));
        currencies.add(new Currency(getString(R.string.currency_name_kwd), getString(R.string.currency_code_kwd), getString(R.string.currency_country_kwd), getString(R.string.currency_symbol_kwd), R.drawable.kw));
        currencies.add(new Currency(getString(R.string.currency_name_kzt), getString(R.string.currency_code_kzt), getString(R.string.currency_country_kzt), getString(R.string.currency_symbol_kzt), R.drawable.kz));
        currencies.add(new Currency(getString(R.string.currency_name_lak), getString(R.string.currency_code_lak), getString(R.string.currency_country_lak), getString(R.string.currency_symbol_lak), R.drawable.la));
        currencies.add(new Currency(getString(R.string.currency_name_lbp), getString(R.string.currency_code_lbp), getString(R.string.currency_country_lbp), getString(R.string.currency_symbol_lbp), R.drawable.lb));
        currencies.add(new Currency(getString(R.string.currency_name_lkr), getString(R.string.currency_code_lkr), getString(R.string.currency_country_lkr), getString(R.string.currency_symbol_lkr), R.drawable.lk));
        currencies.add(new Currency(getString(R.string.currency_name_mmk), getString(R.string.currency_code_mmk), getString(R.string.currency_country_mmk), getString(R.string.currency_symbol_mmk), R.drawable.mm));
        currencies.add(new Currency(getString(R.string.currency_name_mnt), getString(R.string.currency_code_mnt), getString(R.string.currency_country_mnt), getString(R.string.currency_symbol_mnt), R.drawable.mn));
        currencies.add(new Currency(getString(R.string.currency_name_npr), getString(R.string.currency_code_npr), getString(R.string.currency_country_npr), getString(R.string.currency_symbol_npr), R.drawable.np));
        currencies.add(new Currency(getString(R.string.currency_name_omr), getString(R.string.currency_code_omr), getString(R.string.currency_country_omr), getString(R.string.currency_symbol_omr), R.drawable.om));
        currencies.add(new Currency(getString(R.string.currency_name_pkr), getString(R.string.currency_code_pkr), getString(R.string.currency_country_pkr), getString(R.string.currency_symbol_pkr), R.drawable.pk));
        currencies.add(new Currency(getString(R.string.currency_name_qar), getString(R.string.currency_code_qar), getString(R.string.currency_country_qar), getString(R.string.currency_symbol_qar), R.drawable.qa));
        currencies.add(new Currency(getString(R.string.currency_name_sar), getString(R.string.currency_code_sar), getString(R.string.currency_country_sar), getString(R.string.currency_symbol_sar), R.drawable.sa));
        currencies.add(new Currency(getString(R.string.currency_name_tjs), getString(R.string.currency_code_tjs), getString(R.string.currency_country_tjs), getString(R.string.currency_symbol_tjs), R.drawable.tj));
        currencies.add(new Currency(getString(R.string.currency_name_twd), getString(R.string.currency_code_twd), getString(R.string.currency_country_twd), getString(R.string.currency_symbol_twd), R.drawable.tw));
        currencies.add(new Currency(getString(R.string.currency_name_uzs), getString(R.string.currency_code_uzs), getString(R.string.currency_country_uzs), getString(R.string.currency_symbol_uzs), R.drawable.uz));
        currencies.add(new Currency(getString(R.string.currency_name_vnd), getString(R.string.currency_code_vnd), getString(R.string.currency_country_vnd), getString(R.string.currency_symbol_vnd), R.drawable.vn));
        currencies.add(new Currency(getString(R.string.currency_name_all), getString(R.string.currency_code_all), getString(R.string.currency_country_all), getString(R.string.currency_symbol_all), R.drawable.al));
        currencies.add(new Currency(getString(R.string.currency_name_bam), getString(R.string.currency_code_bam), getString(R.string.currency_country_bam), getString(R.string.currency_symbol_bam), R.drawable.ba));
        currencies.add(new Currency(getString(R.string.currency_name_byn), getString(R.string.currency_code_byn), getString(R.string.currency_country_byn), getString(R.string.currency_symbol_byn), R.drawable.by));
        currencies.add(new Currency(getString(R.string.currency_name_mdl), getString(R.string.currency_code_mdl), getString(R.string.currency_country_mdl), getString(R.string.currency_symbol_mdl), R.drawable.md));
        currencies.add(new Currency(getString(R.string.currency_name_mkd), getString(R.string.currency_code_mkd), getString(R.string.currency_country_mkd), getString(R.string.currency_symbol_mkd), R.drawable.mk));
        currencies.add(new Currency(getString(R.string.currency_name_rsd), getString(R.string.currency_code_rsd), getString(R.string.currency_country_rsd), getString(R.string.currency_symbol_rsd), R.drawable.rs));
        currencies.add(new Currency(getString(R.string.currency_name_uah), getString(R.string.currency_code_uah), getString(R.string.currency_country_uah), getString(R.string.currency_symbol_uah), R.drawable.ua));
        currencies.add(new Currency(getString(R.string.currency_name_ars), getString(R.string.currency_code_ars), getString(R.string.currency_country_ars), getString(R.string.currency_symbol_ars), R.drawable.ar));
        currencies.add(new Currency(getString(R.string.currency_name_bbd), getString(R.string.currency_code_bbd), getString(R.string.currency_country_bbd), getString(R.string.currency_symbol_bbd), R.drawable.bb));
        currencies.add(new Currency(getString(R.string.currency_name_bob), getString(R.string.currency_code_bob), getString(R.string.currency_country_bob), getString(R.string.currency_symbol_bob), R.drawable.bo));
        currencies.add(new Currency(getString(R.string.currency_name_bsd), getString(R.string.currency_code_bsd), getString(R.string.currency_country_bsd), getString(R.string.currency_symbol_bsd), R.drawable.bs));
        currencies.add(new Currency(getString(R.string.currency_name_bzd), getString(R.string.currency_code_bzd), getString(R.string.currency_country_bzd), getString(R.string.currency_symbol_bzd), R.drawable.bz));
        currencies.add(new Currency(getString(R.string.currency_name_clp), getString(R.string.currency_code_clp), getString(R.string.currency_country_clp), getString(R.string.currency_symbol_clp), R.drawable.cl));
        currencies.add(new Currency(getString(R.string.currency_name_cop), getString(R.string.currency_code_cop), getString(R.string.currency_country_cop), getString(R.string.currency_symbol_cop), R.drawable.co));
        currencies.add(new Currency(getString(R.string.currency_name_crc), getString(R.string.currency_code_crc), getString(R.string.currency_country_crc), getString(R.string.currency_symbol_crc), R.drawable.cr));
        currencies.add(new Currency(getString(R.string.currency_name_bnd), getString(R.string.currency_code_bnd), getString(R.string.currency_country_bnd), getString(R.string.currency_symbol_bnd), R.drawable.bn));
        currencies.add(new Currency(getString(R.string.currency_name_gtq), getString(R.string.currency_code_gtq), getString(R.string.currency_country_gtq), getString(R.string.currency_symbol_gtq), R.drawable.gt));
        currencies.add(new Currency(getString(R.string.currency_name_gyd), getString(R.string.currency_code_gyd), getString(R.string.currency_country_gyd), getString(R.string.currency_symbol_gyd), R.drawable.gy));
        currencies.add(new Currency(getString(R.string.currency_name_hnl), getString(R.string.currency_code_hnl), getString(R.string.currency_country_hnl), getString(R.string.currency_symbol_hnl), R.drawable.hn));
        currencies.add(new Currency(getString(R.string.currency_name_jmd), getString(R.string.currency_code_jmd), getString(R.string.currency_country_jmd), getString(R.string.currency_symbol_jmd), R.drawable.jm));
        currencies.add(new Currency(getString(R.string.currency_name_pen), getString(R.string.currency_code_pen), getString(R.string.currency_country_pen), getString(R.string.currency_symbol_pen), R.drawable.pe));
        currencies.add(new Currency(getString(R.string.currency_name_pyg), getString(R.string.currency_code_pyg), getString(R.string.currency_country_pyg), getString(R.string.currency_symbol_pyg), R.drawable.py));
        currencies.add(new Currency(getString(R.string.currency_name_ttd), getString(R.string.currency_code_ttd), getString(R.string.currency_country_ttd), getString(R.string.currency_symbol_ttd), R.drawable.tt));
        currencies.add(new Currency(getString(R.string.currency_name_uyu), getString(R.string.currency_code_uyu), getString(R.string.currency_country_uyu), getString(R.string.currency_symbol_uyu), R.drawable.uy));
        currencies.add(new Currency(getString(R.string.currency_name_aoa), getString(R.string.currency_code_aoa), getString(R.string.currency_country_aoa), getString(R.string.currency_symbol_aoa), R.drawable.ao));
        currencies.add(new Currency(getString(R.string.currency_name_bwp), getString(R.string.currency_code_bwp), getString(R.string.currency_country_bwp), getString(R.string.currency_symbol_bwp), R.drawable.bw));
        currencies.add(new Currency(getString(R.string.currency_name_dzd), getString(R.string.currency_code_dzd), getString(R.string.currency_country_dzd), getString(R.string.currency_symbol_dzd), R.drawable.dz));
        currencies.add(new Currency(getString(R.string.currency_name_egp), getString(R.string.currency_code_egp), getString(R.string.currency_country_egp), getString(R.string.currency_symbol_egp), R.drawable.eg));
        currencies.add(new Currency(getString(R.string.currency_name_etb), getString(R.string.currency_code_etb), getString(R.string.currency_country_etb), getString(R.string.currency_symbol_etb), R.drawable.et));
        currencies.add(new Currency(getString(R.string.currency_name_ghs), getString(R.string.currency_code_ghs), getString(R.string.currency_country_ghs), getString(R.string.currency_symbol_ghs), R.drawable.gh));
        currencies.add(new Currency(getString(R.string.currency_name_kes), getString(R.string.currency_code_kes), getString(R.string.currency_country_kes), getString(R.string.currency_symbol_kes), R.drawable.ke));
        currencies.add(new Currency(getString(R.string.currency_name_mad), getString(R.string.currency_code_mad), getString(R.string.currency_country_mad), getString(R.string.currency_symbol_mad), R.drawable.ma));
        currencies.add(new Currency(getString(R.string.currency_name_mur), getString(R.string.currency_code_mur), getString(R.string.currency_country_mur), getString(R.string.currency_symbol_mur), R.drawable.mu));
        currencies.add(new Currency(getString(R.string.currency_name_ngn), getString(R.string.currency_code_ngn), getString(R.string.currency_country_ngn), getString(R.string.currency_symbol_ngn), R.drawable.ng));
        currencies.add(new Currency(getString(R.string.currency_name_tnd), getString(R.string.currency_code_tnd), getString(R.string.currency_country_tnd), getString(R.string.currency_symbol_tnd), R.drawable.tn));
        currencies.add(new Currency(getString(R.string.currency_name_tzs), getString(R.string.currency_code_tzs), getString(R.string.currency_country_tzs), getString(R.string.currency_symbol_tzs), R.drawable.tz));
        currencies.add(new Currency(getString(R.string.currency_name_ugx), getString(R.string.currency_code_ugx), getString(R.string.currency_country_ugx), getString(R.string.currency_symbol_ugx), R.drawable.ug));
        currencies.add(new Currency(getString(R.string.currency_name_zmw), getString(R.string.currency_code_zmw), getString(R.string.currency_country_zmw), getString(R.string.currency_symbol_zmw), R.drawable.zm));
        currencies.add(new Currency(getString(R.string.currency_name_fjd), getString(R.string.currency_code_fjd), getString(R.string.currency_country_fjd), getString(R.string.currency_symbol_fjd), R.drawable.fj));
        currencies.add(new Currency(getString(R.string.currency_name_pgk), getString(R.string.currency_code_pgk), getString(R.string.currency_country_pgk), getString(R.string.currency_symbol_pgk), R.drawable.pg));

    }

    private void showCurrencyPicker(boolean source) {
        ArrayList<String> labels = new ArrayList<>(), details = new ArrayList<>(), keywords = new ArrayList<>();
        int[] flags = new int[currencies.size()];
        for (int i = 0; i < currencies.size(); i++) {
            Currency currency = currencies.get(i);
            labels.add(currency.getCurrencyName());
            details.add(getString(R.string.currency_country_code_symbol, currency.getCountry(), currency.getCurrencyISOCode(), currency.getCurrencySymbol().isEmpty() ? currency.getCurrencyISOCode() : currency.getCurrencySymbol()));
            keywords.add(currency.getCountry() + " " + currency.getCurrencySymbol());
            flags[i] = currency.getFlagImageResource();
        }
        String title = getString(source ? R.string.converter_picker_from_title : R.string.converter_picker_to_title,
                getString(R.string.currency_converter));
        ConverterPickerDialogFragment.newInstance(title, source, 0,
                source ? prefs.getFromCurrencyIndex() : prefs.getToCurrencyIndex(),
                labels, details, keywords, flags).showFor(this);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        getChildFragmentManager().setFragmentResultListener(ConverterPickerDialogFragment.RESULT_KEY,
                getViewLifecycleOwner(), (key, result) -> {
                    int index = result.getInt(ConverterPickerDialogFragment.INDEX, -1);
                    if (index >= 0 && index < currencies.size()) {
                        onChooseCurrency(currencies.get(index),
                                result.getBoolean(ConverterPickerDialogFragment.SOURCE) ? "FROM" : "TO");
                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyRecorder = new IdleHistoryRecorder(this, savedInstanceState);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        historyRecorder.saveState(state);
    }

    public void historyVisibilityChanged() {
        if (historyRecorder != null) historyRecorder.visibilityChanged();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        historyVisibilityChanged();
    }

    @Override
    public boolean onLongClick(View view) {

        try {
            if (view.getId() == R.id.output) {
                ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.brand_name), ((EditText) view).getText().toString());
                clipboard.setPrimaryClip(clip);
                GenericFunctions.showToast(view, R.string.toast_copied_clipboard);
            }
        } catch (Exception ignored) { // Ignored
        }
        return false;
    }

    private void copyToClipboard(String toClipBoard) {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.brand_name), toClipBoard);
        clipboard.setPrimaryClip(clip);
    }

    public void onClick(View view) {

        int id = view.getId();
        String previous = inputValue.getText().toString();
        if (id == R.id.fromUnit) {
            showCurrencyPicker(true);
            InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
        } else if (id == R.id.toUnit) {
            showCurrencyPicker(false);
            InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
        } else if (id == R.id.reverse) {
            int tempFromIndex = prefs.getFromCurrencyIndex();
            int tempToIndex = prefs.getToCurrencyIndex();
            onChooseCurrency(currencies.get(tempToIndex), "FROM");
            onChooseCurrency(currencies.get(tempFromIndex), "TO");
        } else if (id == R.id.backspace) {
            if (previous.isEmpty() || previous.equals("0")) return;
            String substr = inputValue.getText().toString().substring(0, inputValue.getText().toString().length() - 1);
            inputValue.setText(substr);
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.dot) {
            if (inputValue.getText().toString().isEmpty()) inputValue.setText(R.string.decimal_input_start);
            else if (inputValue.getText().toString().equals("-")) inputValue.append(getString(R.string.decimal_input_start));
            else if (!previous.contains(".")) inputValue.append(".");
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.minus) {
            inputValue.setText(previous.startsWith("-") ? previous.substring(1) : "-" + previous);
            inputValue.setSelection(inputValue.length());
        } else if (id == R.id.ac) {
            inputValue.setText("");
        } else if (id == R.id.zero) {
            String currentInput1 = inputValue.getText().toString();
            if (!currentInput1.equals("0")) inputValue.append("0");
        } else if (id == R.id.double_zero) {
            String currentInput = inputValue.getText().toString();
            if (currentInput.equals("0")) {
                //Do nothing if already 0 is added and user is trying to add more 0s.
            } else if (currentInput.isEmpty()) inputValue.append("0");
            else inputValue.append("00");
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.copy) {
            if (inputValue.getText().toString().isEmpty() || outputValue.getText().toString().isEmpty() || inputValue.getText().toString().equals("0") || outputValue.getText().toString().equals("0")) {
                Toast.makeText(context, R.string.toast_no_results_to_copy, Toast.LENGTH_SHORT).show();
                return;
            }
            String shareBody = getString(R.string.currency_copy_format, inputValue.getText(), fromCurrency.getText(), fromCurrencyISO.getText(), outputValue.getText(), toCurrency.getText(), toCurrencyISO.getText());

            try {
                copyToClipboard(shareBody);
                Toast.makeText(context, R.string.toast_result_copied, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, R.string.toast_unable_to_copy, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.refresh) {//Log.d("SHRIKI","Refreshing currency rates");
            refreshCurrencyRates(true);
        } else {
            Button button = (Button) view;
            String data = button.getText().toString();
            String currentInput2 = inputValue.getText().toString();
            if (currentInput2.equals("0")) inputValue.setText(data);
            else inputValue.append(data);
            inputValue.setSelection(inputValue.getText().length());
        }

        if (id == R.id.reverse || id == R.id.refresh || !previous.equals(inputValue.getText().toString())) {
            InteractionFeedbackManager.perform(view,
                    id == R.id.reverse || id == R.id.refresh || id == R.id.backspace || id == R.id.ac
                            ? InteractionFeedbackManager.Type.ACTION
                            : InteractionFeedbackManager.Type.INPUT);
        }

    }

    public void convertAndDisplay(String inStr) {
        historyRecorder.invalidate();

        if (inStr.isEmpty() || inStr.equals("-")) {
            outputValue.setText("");
            if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
            return;
        }

        String inCurrency = fromCurrencyISO.getText().toString();
        String outCurrency = toCurrencyISO.getText().toString();
        BigDecimal inRate = currencyRates.get(inCurrency);
        BigDecimal outRate = currencyRates.get(outCurrency);
        if (inCurrency.equals(outCurrency)) {
            inRate = BigDecimal.ONE;
            outRate = BigDecimal.ONE;
        }

        if (inRate != null && outRate != null) {
            try {
                BigDecimal input = NumberUtils.parseDecimal(inStr);
                BigDecimal out = DecimalMath.currency(input, inRate, outRate);
                BigDecimal rate = DecimalMath.currency(BigDecimal.ONE, inRate, outRate);
                NumberFormat format = NumberFormat.getInstance();
                format.setMaximumFractionDigits(2);
                format.setGroupingUsed(false);
                String formattedOut = format.format(out);
                outputValue.setText(formattedOut);
                historyRecorder.update(getString(R.string.history_conversion_input, inStr, inCurrency, outCurrency),
                        getString(R.string.history_conversion_result, formattedOut, outCurrency),
                        CalculatorCatalog.ID_CURRENCY);
                updateParentGraphy(inStr, inCurrency, outCurrency, formattedOut,
                        rate.doubleValue(), input.doubleValue(), out.doubleValue());
            } catch (IllegalArgumentException | ArithmeticException invalid) {
                outputValue.setText("");
                if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
            }
        } else {
            outputValue.setText("");
            if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
        }
    }

    private void updateParentGraphy(String inStr, String inCurrency, String outCurrency,
                                    String formattedOut, double rate, double input, double result) {
        if (!(getParentFragment() instanceof ConverterFragment converter)) return;
        Context context = getContext();
        if (context == null || !isAdded()) return;
        if (inStr.trim().isEmpty() || inStr.equals("-")) {
            converter.clearConversionGraphy();
            return;
        }
        GraphyOutput output = ConversionGraphAdapter.currency(context, input, result, rate,
                inStr + " " + inCurrency, formattedOut + " " + outCurrency,
                inCurrency, outCurrency, rateSnapshot == null ? 0 : rateSnapshot.fetchedAt);
        converter.updateConversionGraphy(output, output.getExpression());
    }

    private void initUILayout(View view) {

        Button one = view.findViewById(R.id.one);
        Button two = view.findViewById(R.id.two);
        Button three = view.findViewById(R.id.three);
        Button four = view.findViewById(R.id.four);
        Button five = view.findViewById(R.id.five);
        Button six = view.findViewById(R.id.six);
        Button seven = view.findViewById(R.id.seven);
        Button eight = view.findViewById(R.id.eight);
        Button nine = view.findViewById(R.id.nine);
        Button zero = view.findViewById(R.id.zero);

        ImageButton reverse = view.findViewById(R.id.reverse);
        ImageButton backspace = view.findViewById(R.id.backspace);
        ImageButton copy = view.findViewById(R.id.copy);
        refresh = view.findViewById(R.id.refresh);

        Button subtract = view.findViewById(R.id.minus);
        Button ac = view.findViewById(R.id.ac);
        ac.setVisibility(View.VISIBLE);
        Button dot = view.findViewById(R.id.dot);
        Button double_zero = view.findViewById(R.id.double_zero);

        inputValue = view.findViewById(R.id.input);
        outputValue = view.findViewById(R.id.output);

        inputSymbol = view.findViewById(R.id.inputSymbol);
        outputSymbol = view.findViewById(R.id.outputSymbol);

        rlFromUnit = view.findViewById(R.id.fromUnit);
        rlToUnit = view.findViewById(R.id.toUnit);
        for (int id : new int[]{R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.double_zero,
                R.id.dot, R.id.minus, R.id.reverse, R.id.backspace, R.id.ac, R.id.refresh,
                R.id.fromUnit, R.id.toUnit}) {
            InteractionFeedbackManager.configure(view.findViewById(id));
        }

        llMain = view.findViewById(R.id.ll_main);

        fromFlag = view.findViewById(R.id.fromFlag);
        fromCurrency = view.findViewById(R.id.fromCurrency);
        fromCurrencyISO = view.findViewById(R.id.fromCurrencyISO);

        toFlag = view.findViewById(R.id.toFlag);
        toCurrency = view.findViewById(R.id.toCurrency);
        toCurrencyISO = view.findViewById(R.id.toCurrencyISO);
        txtRate = view.findViewById(R.id.txtRate);

        lastRefreshTime = view.findViewById(R.id.txtLastUpdateTime);

        //Set Listeners
        one.setOnClickListener(this);
        two.setOnClickListener(this);
        three.setOnClickListener(this);
        four.setOnClickListener(this);
        five.setOnClickListener(this);
        six.setOnClickListener(this);
        seven.setOnClickListener(this);
        eight.setOnClickListener(this);
        nine.setOnClickListener(this);
        zero.setOnClickListener(this);
        subtract.setOnClickListener(this);
        dot.setOnClickListener(this);
        double_zero.setOnClickListener(this);
        reverse.setOnClickListener(this);
        backspace.setOnClickListener(this);
        ac.setOnClickListener(this);
        copy.setOnClickListener(this);
        refresh.setOnClickListener(this);

        rlFromUnit.setOnClickListener(this);
        rlToUnit.setOnClickListener(this);

        inputValue.setShowSoftInputOnFocus(false);

        inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        inputValue.setCursorVisible(false);
        inputValue.setFocusable(false);
        inputValue.setClickable(false);
        inputValue.setLongClickable(false);

        //Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        //AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_currency_converter, container, false);
        this.context = this.getActivity();
        initCurrencies();
        initUILayout(view);

        prefs = Preferences.getInstance(context);
        viewGeneration++;
        refreshing = false;

        currencyRates = new HashMap<>();
        rateSnapshot = CurrencyRateStore.readCache(prefs.getCurrencyResponse(), prefs.getCurrencyLastUpdateDate());
        if (rateSnapshot != null) currencyRates.putAll(rateSnapshot.rates);

        onChooseCurrency(currencies.get(prefs.getFromCurrencyIndex()), "FROM");
        onChooseCurrency(currencies.get(prefs.getToCurrencyIndex()), "TO");
        refreshCurrencyRates(false);
        displayRates();

        updateRateStatus();

        TextWatcher inputTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not required
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not required
            }

            @Override
            public void afterTextChanged(Editable s) {
                convertAndDisplay(s.toString());
            }
        };

        inputValue.addTextChangedListener(inputTextWatcher);

        final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) llMain.getLayoutParams();
        mlp.setMargins(0, 0, 0, 0);
        llMain.setLayoutParams(mlp);
        ThemeApplier.apply(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeApplier.apply(getView());
        convertAndDisplay(inputValue.getText().toString());
    }

    public void startAnimationImageView(ImageView img) {
        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(700);

        img.startAnimation(anim);
    }

    public void refreshCurrencyRates(boolean forceRefresh) {
        if (refreshing) return;
        if (forceRefresh) net.smartlogic.unitconverter.helper.UsageAnalytics.event("currency_refresh_requested", "source", "manual");
        if (!forceRefresh && rateSnapshot != null && "Frankfurter".equals(rateSnapshot.provider)
                && rateSnapshot.isFresh(System.currentTimeMillis())) {
            updateRateStatus();
            return;
        }
        refreshing = true;
        final int generation = viewGeneration;
        final Preferences storage = prefs;
        startAnimationImageView(refresh);
        RATE_EXECUTOR.execute(() -> {
            CurrencyRateStore.Snapshot result = null;
            try {
                result = CurrencyRateStore.refresh(storage, forceRefresh, System.currentTimeMillis(),
                        () -> new HttpHandler().makeServiceCall(CurrencyRateStore.ENDPOINT));
            } catch (Exception failure) {
                Log.w("CurrencyRates", "Refresh failed: " + failure.getClass().getSimpleName());
            }
            final CurrencyRateStore.Snapshot loaded = result;
            mainHandler.post(() -> {
                if (generation != viewGeneration || getView() == null || !isAdded() || getContext() == null) return;
                refreshing = false;
                net.smartlogic.unitconverter.helper.UsageAnalytics.event("currency_refresh_result", "outcome", loaded == null ? "failed" : "available");
                refresh.clearAnimation();
                if (loaded != null) {
                    rateSnapshot = loaded;
                    currencyRates.clear();
                    currencyRates.putAll(loaded.rates);
                } else {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, R.string.currency_refresh_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                updateRateStatus();
                displayRates();
                convertAndDisplay(inputValue.getText().toString());
            });
        });
    }

    private void updateRateStatus() {
        if (rateSnapshot == null) {
            lastRefreshTime.setText(R.string.currency_unavailable);
        } else {
            lastRefreshTime.setText(getString(rateSnapshot.isFresh(System.currentTimeMillis())
                            ? R.string.currency_cached_at : R.string.currency_stale_at,
                    AppConst.simpleDateFormat.format(rateSnapshot.fetchedAt)));
        }
    }

    @Override
    public void onDestroyView() {
        historyRecorder.invalidate();
        viewGeneration++;
        refreshing = false;
        refresh.clearAnimation();
        super.onDestroyView();
    }
    public void onChooseCurrency(Currency c, String type) {
        //Log.d("SHRIKI","Clicked currency in frag -> " + c.getCountry() + " Type: " + type);
        String name = c.getCurrencyName();
        String symbol = c.getCurrencySymbol();
        String displayName = (symbol != null && !symbol.isEmpty()) ? getString(R.string.unit_label_symbol_format, name, symbol) : name;

        if (type.equals("FROM")) {
            fromFlag.setImageResource(c.getFlagImageResource());
            fromCurrency.setText(displayName);
            fromCurrencyISO.setText(c.getCurrencyISOCode());
            rlFromUnit.setContentDescription(getString(R.string.converter_source_currency, name, c.getCurrencyISOCode()));
            prefs.setFromCurrencyIndex(currencies.indexOf(c));
            inputSymbol.setText(symbol != null && !symbol.isEmpty() ? symbol : c.getCurrencyISOCode());
        } else if (type.equals("TO")) {
            toFlag.setImageResource(c.getFlagImageResource());
            toCurrency.setText(displayName);
            toCurrencyISO.setText(c.getCurrencyISOCode());
            rlToUnit.setContentDescription(getString(R.string.converter_destination_currency, name, c.getCurrencyISOCode()));
            prefs.setToCurrencyIndex(currencies.indexOf(c));
            outputSymbol.setText(symbol != null && !symbol.isEmpty() ? symbol : c.getCurrencyISOCode());
        }
        convertAndDisplay(inputValue.getText().toString());
        displayRates();
    }

    @SuppressLint("DefaultLocale")
    public void displayRates() {
        String inCurrency = fromCurrencyISO.getText().toString();
        String outCurrency = toCurrencyISO.getText().toString();
        BigDecimal inRate = currencyRates.get(inCurrency);
        BigDecimal outRate = currencyRates.get(outCurrency);
        if (inCurrency.equals(outCurrency)) {
            inRate = BigDecimal.ONE;
            outRate = BigDecimal.ONE;
        }

        if (inRate != null && outRate != null) {
            BigDecimal rate = DecimalMath.currency(BigDecimal.ONE, inRate, outRate);
            //Log.d("SHRIKI", "1 " + inCurrency + " = " + rate + " " + outCurrency);
            txtRate.setText(getString(R.string.currency_rate_format, inCurrency,
                    rate.setScale(4, java.math.RoundingMode.HALF_EVEN).toPlainString(), outCurrency));
            txtRate.setVisibility(View.VISIBLE);
        } else {
            txtRate.setText(R.string.currency_unavailable);
            txtRate.setVisibility(View.VISIBLE);
        }
    }

}
