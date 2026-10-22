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

        currencies.add(new Currency("Australian dollar", "AUD", "Australia", "$", R.drawable.au));
        currencies.add(new Currency("Brazilian real", "BRL", "Brazil", "R$", R.drawable.br));
        currencies.add(new Currency("Euro", "EUR", "Bulgaria", "€", R.drawable.bg));
        currencies.add(new Currency("Canadian dollar", "CAD", "Canada", "$", R.drawable.ca));
        currencies.add(new Currency("Chinese yuan", "CNY", "China", "¥", R.drawable.cn));
        currencies.add(new Currency("Euro", "EUR", "Croatia", "€", R.drawable.hr));
        currencies.add(new Currency("Czech koruna", "CZK", "Czechia", "Kč", R.drawable.cz));
        currencies.add(new Currency("Danish krone", "DKK", "Denmark", "kr", R.drawable.dk));
        currencies.add(new Currency("Hong Kong dollar", "HKD", "Hong Kong", "$", R.drawable.hk));
        currencies.add(new Currency("Hungarian forint", "HUF", "Hungary", "Ft", R.drawable.hu));
        currencies.add(new Currency("Icelandic króna", "ISK", "Iceland", "kr", R.drawable.is));
        currencies.add(new Currency("Indian rupee", "INR", "India", "₹", R.drawable.in));
        currencies.add(new Currency("Indonesian rupiah", "IDR", "Indonesia", "Rp", R.drawable.id));
        currencies.add(new Currency("Israeli new shekel", "ILS", "Israel", "₪", R.drawable.il));
        currencies.add(new Currency("Japanese yen", "JPY", "Japan", "¥", R.drawable.jp));
        currencies.add(new Currency("South Korean won", "KRW", "South Korea", "₩", R.drawable.kr));
        currencies.add(new Currency("Malaysian ringgit", "MYR", "Malaysia", "RM", R.drawable.my));
        currencies.add(new Currency("Mexican peso", "MXN", "Mexico", "$", R.drawable.mx));
        currencies.add(new Currency("New Zealand dollar", "NZD", "New Zealand", "$", R.drawable.nz));
        currencies.add(new Currency("Norwegian krone", "NOK", "Norway", "kr", R.drawable.no));
        currencies.add(new Currency("Philippine peso", "PHP", "Philippines", "₱", R.drawable.ph));
        currencies.add(new Currency("Polish złoty", "PLN", "Poland", "zł", R.drawable.pl));
        currencies.add(new Currency("Romanian leu", "RON", "Romania", "lei", R.drawable.ro));
        currencies.add(new Currency("Russian ruble", "RUB", "Russia", "", R.drawable.ru));
        currencies.add(new Currency("Singapore dollar", "SGD", "Singapore", "$", R.drawable.sg));
        currencies.add(new Currency("South African rand", "ZAR", "South Africa", "R", R.drawable.za));
        currencies.add(new Currency("Swedish krona", "SEK", "Sweden", "kr", R.drawable.se));
        currencies.add(new Currency("Swiss franc", "CHF", "Switzerland", "Fr", R.drawable.ch));
        currencies.add(new Currency("Thai baht", "THB", "Thailand", "฿", R.drawable.th));
        currencies.add(new Currency("Turkish lira", "TRY", "Turkey", "", R.drawable.tr));
        currencies.add(new Currency("British pound", "GBP", "United Kingdom", "£", R.drawable.gb));
        currencies.add(new Currency("US dollar", "USD", "United States", "$", R.drawable.us));
        currencies.add(new Currency("Euro", "EUR", "Germany", "€", R.drawable.de));

        // Append only: preferences persist positions. Legacy BGN/HRK positions now use EUR.
        currencies.add(new Currency("United Arab Emirates Dirham", "AED", "United Arab Emirates", "د.إ", R.drawable.ae));
        currencies.add(new Currency("Armenian Dram", "AMD", "Armenia", "֏", R.drawable.am));
        currencies.add(new Currency("Azerbaijani Manat", "AZN", "Azerbaijan", "₼", R.drawable.az));
        currencies.add(new Currency("Bangladeshi Taka", "BDT", "Bangladesh", "৳", R.drawable.bd));
        currencies.add(new Currency("Bahraini Dinar", "BHD", "Bahrain", "د.ب", R.drawable.bh));
        currencies.add(new Currency("Bhutanese Ngultrum", "BTN", "Bhutan", "Nu.", R.drawable.bt));
        currencies.add(new Currency("Georgian Lari", "GEL", "Georgia", "₾", R.drawable.ge));
        currencies.add(new Currency("Iraqi Dinar", "IQD", "Iraq", "ع.د", R.drawable.iq));
        currencies.add(new Currency("Jordanian Dinar", "JOD", "Jordan", "د.ا", R.drawable.jo));
        currencies.add(new Currency("Kyrgyzstani Som", "KGS", "Kyrgyzstan", "som", R.drawable.kg));
        currencies.add(new Currency("Cambodian Riel", "KHR", "Cambodia", "៛", R.drawable.kh));
        currencies.add(new Currency("Kuwaiti Dinar", "KWD", "Kuwait", "د.ك", R.drawable.kw));
        currencies.add(new Currency("Kazakhstani Tenge", "KZT", "Kazakhstan", "₸", R.drawable.kz));
        currencies.add(new Currency("Lao Kip", "LAK", "Laos", "₭", R.drawable.la));
        currencies.add(new Currency("Lebanese Pound", "LBP", "Lebanon", "ل.ل", R.drawable.lb));
        currencies.add(new Currency("Sri Lankan Rupee", "LKR", "Sri Lanka", "₨", R.drawable.lk));
        currencies.add(new Currency("Myanmar Kyat", "MMK", "Myanmar", "K", R.drawable.mm));
        currencies.add(new Currency("Mongolian Tögrög", "MNT", "Mongolia", "₮", R.drawable.mn));
        currencies.add(new Currency("Nepalese Rupee", "NPR", "Nepal", "Rs.", R.drawable.np));
        currencies.add(new Currency("Omani Rial", "OMR", "Oman", "ر.ع.", R.drawable.om));
        currencies.add(new Currency("Pakistani Rupee", "PKR", "Pakistan", "₨", R.drawable.pk));
        currencies.add(new Currency("Qatari Riyal", "QAR", "Qatar", "ر.ق", R.drawable.qa));
        currencies.add(new Currency("Saudi Riyal", "SAR", "Saudi Arabia", "ر.س", R.drawable.sa));
        currencies.add(new Currency("Tajikistani Somoni", "TJS", "Tajikistan", "ЅМ", R.drawable.tj));
        currencies.add(new Currency("New Taiwan Dollar", "TWD", "Taiwan", "$", R.drawable.tw));
        currencies.add(new Currency("Uzbekistan Som", "UZS", "Uzbekistan", "so'm", R.drawable.uz));
        currencies.add(new Currency("Vietnamese Đồng", "VND", "Vietnam", "₫", R.drawable.vn));
        currencies.add(new Currency("Albanian Lek", "ALL", "Albania", "L", R.drawable.al));
        currencies.add(new Currency("Bosnia and Herzegovina Convertible Mark", "BAM", "Bosnia and Herzegovina", "КМ", R.drawable.ba));
        currencies.add(new Currency("Belarusian Ruble", "BYN", "Belarus", "Br", R.drawable.by));
        currencies.add(new Currency("Moldovan Leu", "MDL", "Moldova", "L", R.drawable.md));
        currencies.add(new Currency("Macedonian Denar", "MKD", "North Macedonia", "ден", R.drawable.mk));
        currencies.add(new Currency("Serbian Dinar", "RSD", "Serbia", "RSD", R.drawable.rs));
        currencies.add(new Currency("Ukrainian Hryvnia", "UAH", "Ukraine", "₴", R.drawable.ua));
        currencies.add(new Currency("Argentine Peso", "ARS", "Argentina", "$", R.drawable.ar));
        currencies.add(new Currency("Barbadian Dollar", "BBD", "Barbados", "$", R.drawable.bb));
        currencies.add(new Currency("Bolivian Boliviano", "BOB", "Bolivia", "Bs.", R.drawable.bo));
        currencies.add(new Currency("Bahamian Dollar", "BSD", "Bahamas", "$", R.drawable.bs));
        currencies.add(new Currency("Belize Dollar", "BZD", "Belize", "$", R.drawable.bz));
        currencies.add(new Currency("Chilean Peso", "CLP", "Chile", "$", R.drawable.cl));
        currencies.add(new Currency("Colombian Peso", "COP", "Colombia", "$", R.drawable.co));
        currencies.add(new Currency("Costa Rican Colón", "CRC", "Costa Rica", "₡", R.drawable.cr));
        currencies.add(new Currency("Brunei Dollar", "BND", "Brunei", "$", R.drawable.bn));
        currencies.add(new Currency("Guatemalan Quetzal", "GTQ", "Guatemala", "Q", R.drawable.gt));
        currencies.add(new Currency("Guyanese Dollar", "GYD", "Guyana", "$", R.drawable.gy));
        currencies.add(new Currency("Honduran Lempira", "HNL", "Honduras", "L", R.drawable.hn));
        currencies.add(new Currency("Jamaican Dollar", "JMD", "Jamaica", "$", R.drawable.jm));
        currencies.add(new Currency("Peruvian Sol", "PEN", "Peru", "S/", R.drawable.pe));
        currencies.add(new Currency("Paraguayan Guaraní", "PYG", "Paraguay", "₲", R.drawable.py));
        currencies.add(new Currency("Trinidad and Tobago Dollar", "TTD", "Trinidad and Tobago", "$", R.drawable.tt));
        currencies.add(new Currency("Uruguayan Peso", "UYU", "Uruguay", "$U", R.drawable.uy));
        currencies.add(new Currency("Angolan Kwanza", "AOA", "Angola", "Kz", R.drawable.ao));
        currencies.add(new Currency("Botswana Pula", "BWP", "Botswana", "P", R.drawable.bw));
        currencies.add(new Currency("Algerian Dinar", "DZD", "Algeria", "د.ج", R.drawable.dz));
        currencies.add(new Currency("Egyptian Pound", "EGP", "Egypt", "ج.م", R.drawable.eg));
        currencies.add(new Currency("Ethiopian Birr", "ETB", "Ethiopia", "Br", R.drawable.et));
        currencies.add(new Currency("Ghanaian Cedi", "GHS", "Ghana", "₵", R.drawable.gh));
        currencies.add(new Currency("Kenyan Shilling", "KES", "Kenya", "KSh", R.drawable.ke));
        currencies.add(new Currency("Moroccan Dirham", "MAD", "Morocco", "د.م.", R.drawable.ma));
        currencies.add(new Currency("Mauritian Rupee", "MUR", "Mauritius", "₨", R.drawable.mu));
        currencies.add(new Currency("Nigerian Naira", "NGN", "Nigeria", "₦", R.drawable.ng));
        currencies.add(new Currency("Tunisian Dinar", "TND", "Tunisia", "د.ت", R.drawable.tn));
        currencies.add(new Currency("Tanzanian Shilling", "TZS", "Tanzania", "Sh", R.drawable.tz));
        currencies.add(new Currency("Ugandan Shilling", "UGX", "Uganda", "USh", R.drawable.ug));
        currencies.add(new Currency("Zambian Kwacha", "ZMW", "Zambia", "K", R.drawable.zm));
        currencies.add(new Currency("Fijian Dollar", "FJD", "Fiji", "$", R.drawable.fj));
        currencies.add(new Currency("Papua New Guinean Kina", "PGK", "Papua New Guinea", "K", R.drawable.pg));

    }

    private void showCurrencyPicker(boolean source) {
        ArrayList<String> labels = new ArrayList<>(), details = new ArrayList<>(), keywords = new ArrayList<>();
        int[] flags = new int[currencies.size()];
        for (int i = 0; i < currencies.size(); i++) {
            Currency currency = currencies.get(i);
            labels.add(currency.getCurrencyName());
            details.add(getString(R.string.currency_country_code, currency.getCountry(), currency.getCurrencyISOCode()));
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
            if (inputValue.getText().toString().isEmpty()) inputValue.setText("0.");
            else if (inputValue.getText().toString().equals("-")) inputValue.append("0.");
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
            String shareBody = String.format("%s %s (%s) = %s %s (%s)", inputValue.getText(), fromCurrency.getText(), fromCurrencyISO.getText(), outputValue.getText(), toCurrency.getText(), toCurrencyISO.getText());

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
        if (inStr.trim().isEmpty() || inStr.equals("-")) {
            converter.clearConversionGraphy();
            return;
        }
        GraphyOutput output = ConversionGraphAdapter.currency(requireContext(), input, result, rate,
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

        inputValue.requestFocus();
        //inputValue.setTextIsSelectable(false);

        inputValue.setShowSoftInputOnFocus(false);

        inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        outputValue.setOnLongClickListener(this);

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
                if (generation != viewGeneration || getView() == null) return;
                refreshing = false;
                refresh.clearAnimation();
                if (loaded != null) {
                    rateSnapshot = loaded;
                    currencyRates.clear();
                    currencyRates.putAll(loaded.rates);
                } else {
                    Toast.makeText(requireContext(), R.string.currency_refresh_failed, Toast.LENGTH_SHORT).show();
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
        viewGeneration++;
        refreshing = false;
        refresh.clearAnimation();
        super.onDestroyView();
    }
    public void onChooseCurrency(Currency c, String type) {
        //Log.d("SHRIKI","Clicked currency in frag -> " + c.getCountry() + " Type: " + type);
        String name = c.getCurrencyName();
        String symbol = c.getCurrencySymbol();
        String displayName = (symbol != null && !symbol.isEmpty()) ? String.format("%s (%s)", name, symbol) : name;

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
            txtRate.setText(String.format("1 %s = %s %s", inCurrency,
                    rate.setScale(4, java.math.RoundingMode.HALF_EVEN).toPlainString(), outCurrency));
            txtRate.setVisibility(View.VISIBLE);
        } else {
            txtRate.setText(R.string.currency_unavailable);
            txtRate.setVisibility(View.VISIBLE);
        }
    }

}
