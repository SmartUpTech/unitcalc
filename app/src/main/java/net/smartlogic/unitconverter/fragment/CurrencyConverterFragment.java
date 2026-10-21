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
import net.smartlogic.unitconverter.fragment.BottomSheetCurrencyDialogFragment.OnChooseCurrencyListener;
import net.smartlogic.unitconverter.graphy.integration.ConversionGraphAdapter;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.helper.HttpHandler;
import net.smartlogic.unitconverter.helper.CurrencyRateStore;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.model.Currency;
import net.smartlogic.unitconverter.utils.GenericFunctions;
import net.smartlogic.unitconverter.utils.NumberUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class CurrencyConverterFragment extends Fragment implements OnClickListener, OnLongClickListener, OnChooseCurrencyListener {

    private static final ExecutorService RATE_EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean refreshing;
    private int viewGeneration;
    private CurrencyRateStore.Snapshot rateSnapshot;

    private EditText inputValue, outputValue;
    private TextView inputSymbol, outputSymbol;
    private Context context;
    private HashMap<String, Float> currencyRates;
    private Preferences prefs;
    private TextView fromCurrency, fromCurrencyISO, toCurrency, toCurrencyISO, txtRate;
    private TextView lastRefreshTime;
    private ImageView fromFlag, toFlag, refresh;
    private LinearLayout llMain;
    private RelativeLayout rlFromUnit, rlToUnit;
    private ArrayList<Currency> currencies;
    private BottomSheetCurrencyDialogFragment bottomSheetCurrencyDialogFragment;

    public CurrencyConverterFragment() {
        // Required empty public constructor
    }

    public static CurrencyConverterFragment newInstance() {
        CurrencyConverterFragment fragment = new CurrencyConverterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public int getDrawableResourceId(String name) {
        try {
            return getResources().getIdentifier(name, "drawable", context.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    public void initCurrencies() {
        currencies = new ArrayList<>();

        currencies.add(new Currency("Australian dollar", "AUD", "Australia", "$", getDrawableResourceId("au")));
        currencies.add(new Currency("Brazilian real", "BRL", "Brazil", "R$", getDrawableResourceId("br")));
        currencies.add(new Currency("Bulgarian lev", "BGN", "Bulgaria", "лв", getDrawableResourceId("bg")));
        currencies.add(new Currency("Canadian dollar", "CAD", "Canada", "$", getDrawableResourceId("ca")));
        currencies.add(new Currency("Chinese yuan", "CNY", "China", "¥", getDrawableResourceId("cn")));
        currencies.add(new Currency("Croatian kuna", "HRK", "Croatia", "kn", getDrawableResourceId("cr")));
        currencies.add(new Currency("Czech koruna", "CZK", "Czechia", "Kč", getDrawableResourceId("cz")));
        currencies.add(new Currency("Danish krone", "DKK", "Denmark", "kr", getDrawableResourceId("dk")));
        currencies.add(new Currency("Hong Kong dollar", "HKD", "Hong Kong", "$", getDrawableResourceId("hk")));
        currencies.add(new Currency("Hungarian forint", "HUF", "Hungary", "Ft", getDrawableResourceId("hu")));
        currencies.add(new Currency("Icelandic króna", "ISK", "Iceland", "kr", getDrawableResourceId("is")));
        currencies.add(new Currency("Indian rupee", "INR", "India", "₹", getDrawableResourceId("in")));
        currencies.add(new Currency("Indonesian rupiah", "IDR", "Indonesia", "Rp", getDrawableResourceId("id")));
        currencies.add(new Currency("Israeli new shekel", "ILS", "Israel", "₪", getDrawableResourceId("il")));
        currencies.add(new Currency("Japanese yen", "JPY", "Japan", "¥", getDrawableResourceId("jp")));
        currencies.add(new Currency("South Korean won", "KRW", "South Korea", "₩", getDrawableResourceId("kr")));
        currencies.add(new Currency("Malaysian ringgit", "MYR", "Malaysia", "RM", getDrawableResourceId("my")));
        currencies.add(new Currency("Mexican peso", "MXN", "Mexico", "$", getDrawableResourceId("mx")));
        currencies.add(new Currency("New Zealand dollar", "NZD", "New Zealand", "$", getDrawableResourceId("nz")));
        currencies.add(new Currency("Norwegian krone", "NOK", "Norway", "kr", getDrawableResourceId("no")));
        currencies.add(new Currency("Philippine peso", "PHP", "Philippines", "₱", getDrawableResourceId("ph")));
        currencies.add(new Currency("Polish złoty", "PLN", "Poland", "zł", getDrawableResourceId("pl")));
        currencies.add(new Currency("Romanian leu", "RON", "Romania", "lei", getDrawableResourceId("ro")));
        currencies.add(new Currency("Russian ruble", "RUB", "Russia", "", getDrawableResourceId("ru")));
        currencies.add(new Currency("Singapore dollar", "SGD", "Singapore", "$", getDrawableResourceId("sg")));
        currencies.add(new Currency("South African rand", "ZAR", "South Africa", "R", getDrawableResourceId("za")));
        currencies.add(new Currency("Swedish krona", "SEK", "Sweden", "kr", getDrawableResourceId("se")));
        currencies.add(new Currency("Swiss franc", "CHF", "Switzerland", "Fr", getDrawableResourceId("ch")));
        currencies.add(new Currency("Thai baht", "THB", "Thailand", "฿", getDrawableResourceId("th")));
        currencies.add(new Currency("Turkish lira", "TRY", "Turkey", "", getDrawableResourceId("tr")));
        currencies.add(new Currency("British pound", "GBP", "United Kingdom", "£", getDrawableResourceId("gb")));
        currencies.add(new Currency("US dollar", "USD", "United States", "$", getDrawableResourceId("us")));
        currencies.add(new Currency("Euro", "EUR", "European Union", "€", getDrawableResourceId("eu")));

    }

    public void showBottomSheet(OnChooseCurrencyListener listener, String type) {
        bottomSheetCurrencyDialogFragment = new BottomSheetCurrencyDialogFragment(listener, currencies, type);
        bottomSheetCurrencyDialogFragment.show(getChildFragmentManager(), "ABC");
    }

    public void hideBottomSheet() {
        if (bottomSheetCurrencyDialogFragment != null) bottomSheetCurrencyDialogFragment.dismiss();
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
            showBottomSheet((currency, type) -> {
                onChooseCurrency(currency, type);
                InteractionFeedbackManager.perform(rlFromUnit, InteractionFeedbackManager.Type.ACTION);
            }, "FROM");
            InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
        } else if (id == R.id.toUnit) {
            showBottomSheet((currency, type) -> {
                onChooseCurrency(currency, type);
                InteractionFeedbackManager.perform(rlToUnit, InteractionFeedbackManager.Type.ACTION);
            }, "TO");
            InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
        } else if (id == R.id.reverse) {
            int tempFromIndex = prefs.getFromCurrencyIndex();
            int tempToIndex = prefs.getToCurrencyIndex();
            onChooseCurrency(currencies.get(tempToIndex), "FROM");
            onChooseCurrency(currencies.get(tempFromIndex), "TO");
        } else if (id == R.id.backspace) {
            if (previous.isEmpty() || previous.equals("0")) return;
            String substr = inputValue.getText().toString().substring(0, inputValue.getText().toString().length() - 1);
            if (substr.isEmpty()) substr = "0";
            inputValue.setText(substr);
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.dot) {
            if (inputValue.getText().toString().isEmpty()) inputValue.setText("0.");
            else if (inputValue.getText().toString().equals("-")) inputValue.append("0.");
            else inputValue.append(".");
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.minus) {
            inputValue.setText(previous.startsWith("-") ? previous.substring(1) : "-" + previous);
            inputValue.setSelection(inputValue.length());
        } else if (id == R.id.ac) {
            inputValue.setText("0");
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

        String inCurrency = fromCurrencyISO.getText().toString();
        String outCurrency = toCurrencyISO.getText().toString();
        Float inRate = currencyRates.get(inCurrency);
        Float outRate = currencyRates.get(outCurrency);

        if (inRate != null && outRate != null) {
            Float in;
            try { in = NumberUtils.parseFloat(inStr); }
            catch (NumberFormatException invalid) {
                outputValue.setText("");
                if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
                return;
            }
            //Log.d("SHRIKI", "Input:" + in + " " + inCurrency + " at rate (EUR) " + inRate);
            float out = in * (outRate / inRate);
            //Log.d("SHRIKI", "Output:" + out + " " + outCurrency + " at rate (EUR) " + outRate);

            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            format.setGroupingUsed(false);
            //format.setRoundingMode(RoundingMode.CEILING);
            String formattedOut = format.format(out);
            outputValue.setText(formattedOut);
            updateParentGraphy(inStr, inCurrency, outCurrency, formattedOut, outRate / inRate, in, out);
        } else {
            outputValue.setText("");
            if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
        }
    }

    private void updateParentGraphy(String inStr, String inCurrency, String outCurrency,
                                    String formattedOut, float rate, float input, float result) {
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
    @Override
    public void onChooseCurrency(Currency c, String type) {
        //Log.d("SHRIKI","Clicked currency in frag -> " + c.getCountry() + " Type: " + type);
        hideBottomSheet();
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
        Float inRate = currencyRates.get(inCurrency);
        Float outRate = currencyRates.get(outCurrency);

        if (inRate != null && outRate != null) {
            float rate = outRate / inRate;
            //Log.d("SHRIKI", "1 " + inCurrency + " = " + rate + " " + outCurrency);
            txtRate.setText(String.format("1 %s = %.4f %s", inCurrency, rate, outCurrency));
            txtRate.setVisibility(View.VISIBLE);
        } else {
            txtRate.setText(R.string.currency_unavailable);
            txtRate.setVisibility(View.VISIBLE);
        }
    }

}
