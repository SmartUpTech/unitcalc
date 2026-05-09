package net.smartlogic.unitconverter.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
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

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.app.AppConst;
import net.smartlogic.unitconverter.fragment.BottomSheetCurrencyDialogFragment.OnChooseCurrencyListener;
import net.smartlogic.unitconverter.helper.HttpHandler;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.Currency;
import net.smartlogic.unitconverter.utils.GenericFunctions;
import net.smartlogic.unitconverter.utils.NumberUtils;
import net.smartlogic.unitconverter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class CurrencyConverterFragment extends Fragment implements OnClickListener, OnLongClickListener, OnChooseCurrencyListener {

    private static final String exchangeUrl1 = "https://api.freecurrencyapi.com/v1/latest";
    private static final String exchangeUrl1Key1 = "fca_live_2zeiOdgJr0ZukYsoLsLcQgCxzD5MdihXeNYDOijN"; // info-smartupapps
    private static final String exchangeUrl2 = "https://api.freecurrencyapi.com/v1/latest";
    private static final String exchangeUrl2Key1 = "fca_live_8McvHo9qLx5TlQob16WmnFogeRnyiq0tuwQMWXZe"; // smartlogic
    private static final String exchangeUrl3 = "https://api.freecurrencyapi.com/v1/latest";
    private static final String exchangeUrl3Key1 = "fca_live_Ri1lstwGfoGLRJnWDYn02zZG1Oejj5dZArvsEcVB"; // mlc
    private static final String exchangeUrl4 = "";
    private static final String exchangeUrl4Key1 = "";

    private EditText inputValue, outputValue;
    private Context context;
    private HashMap<String, Float> currencyRates;
    private Preferences prefs;
    private TextView fromCurrency, fromCurrencyISO, toCurrency, toCurrencyISO, txtRate;
    private TextView lastRefreshTime;
    private ImageView fromFlag, toFlag, refresh;
    private LinearLayout llMain;
    private ArrayList<Currency> currencies;
    private BottomSheetCurrencyDialogFragment bottomSheetCurrencyDialogFragment;
    private OnChooseCurrencyListener onChooseCurrencyListener;
    private Utils utils;
    private final TextWatcher outputTextSizeAdjuster = new TextWatcher() {
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
            adjustTextSize(outputValue);
        }
    };

    public CurrencyConverterFragment() {
        // Required empty public constructor
    }

    public static CurrencyConverterFragment newInstance() {
        CurrencyConverterFragment fragment = new CurrencyConverterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void adjustTextSize(EditText editText) {
        if (editText.length() >= 10 && editText.length() <= 14)
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        else if (editText.length() > 14) editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        else editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
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
                ClipData clip = ClipData.newPlainText(getString(R.string.app_name), ((EditText) view).getText().toString());
                clipboard.setPrimaryClip(clip);
                GenericFunctions.showToast(view, R.string.toast_copied_clipboard);
            }
        } catch (Exception ignored) { // Ignored
        }
        return false;
    }

    private void copyToClipboard(String toClipBoard) {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), toClipBoard);
        clipboard.setPrimaryClip(clip);
    }

    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.fromUnit) {
            showBottomSheet(onChooseCurrencyListener, "FROM");
        } else if (id == R.id.toUnit) {
            showBottomSheet(onChooseCurrencyListener, "TO");
        } else if (id == R.id.reverse) {
            int tempFromIndex = prefs.getFromCurrencyIndex();
            int tempToIndex = prefs.getToCurrencyIndex();
            onChooseCurrency(currencies.get(tempToIndex), "FROM");
            onChooseCurrency(currencies.get(tempFromIndex), "TO");
        } else if (id == R.id.backspace) {
            String substr = inputValue.getText().toString().substring(0, inputValue.getText().toString().length() - 1);
            if (substr.equals("")) substr = "0";
            inputValue.setText(substr);
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.dot) {
            if (inputValue.getText().toString().isEmpty()) inputValue.setText("0.");
            else if (inputValue.getText().toString().equals("-")) inputValue.append("0.");
            else inputValue.append(".");
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.minus) {
                // Do nothing
        } else if (id == R.id.ac) {
            inputValue.setText("0");
        } else if (id == R.id.zero) {
            String currentInput1 = inputValue.getText().toString();
            if (!currentInput1.equals("0")) inputValue.append("0");
        } else if (id == R.id.double_zero) {
            String currentInput = inputValue.getText().toString();
            if (currentInput.equals("0")) {
                //Do nothing if already 0 is added and user is trying to add more 0s.
            } else if (currentInput.length() == 0) inputValue.append("0");
            else inputValue.append("00");
            inputValue.setSelection(inputValue.getText().length());
        } else if (id == R.id.copy) {
            if (inputValue.getText().toString().equals("") || outputValue.getText().toString().equals("") || inputValue.getText().toString().equals("0") || outputValue.getText().toString().equals("0")) {
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
        } else if (id == R.id.share) {
            String shareBody;
            if (inputValue.getText().toString().equals("") || outputValue.getText().toString().equals("") || inputValue.getText().toString().equals("0") || outputValue.getText().toString().equals("0")) {
                shareBody = getString(R.string.note_share_body) + getString(R.string.url_app_short_link);
            } else {
                shareBody = String.format("%s %s (%s) = %s %s (%s)", inputValue.getText(), fromCurrency.getText(), fromCurrencyISO.getText(), outputValue.getText(), toCurrency.getText(), toCurrencyISO.getText());
            }

            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
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

    }

    public void convertAndDisplay(String inStr) {

        String inCurrency = fromCurrencyISO.getText().toString();
        String outCurrency = toCurrencyISO.getText().toString();
        Float inRate = currencyRates.get(inCurrency);
        Float outRate = currencyRates.get(outCurrency);

        if (inRate != null && outRate != null) {
            Float in = NumberUtils.parseFloat(inStr);
            //Log.d("SHRIKI", "Input:" + in + " " + inCurrency + " at rate (EUR) " + inRate);
            float out = in * (outRate / inRate);
            //Log.d("SHRIKI", "Output:" + out + " " + outCurrency + " at rate (EUR) " + outRate);

            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            format.setGroupingUsed(false);
            //format.setRoundingMode(RoundingMode.CEILING);
            outputValue.setText(format.format(out));
        }
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
        ImageButton share = view.findViewById(R.id.share);
        refresh = view.findViewById(R.id.refresh);

        //Button subtract = view.findViewById(R.id.minus);
        Button ac = view.findViewById(R.id.ac);
        Button dot = view.findViewById(R.id.dot);
        Button double_zero = view.findViewById(R.id.double_zero);

        inputValue = view.findViewById(R.id.input);
        outputValue = view.findViewById(R.id.output);

        RelativeLayout rlFromUnit = view.findViewById(R.id.fromUnit);
        LinearLayout rlToUnit = view.findViewById(R.id.toUnit);

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
//        subtract.setOnClickListener(this);
        dot.setOnClickListener(this);
        double_zero.setOnClickListener(this);
        reverse.setOnClickListener(this);
        backspace.setOnClickListener(this);
        ac.setOnClickListener(this);
        copy.setOnClickListener(this);
        share.setOnClickListener(this);
        refresh.setOnClickListener(this);

        rlFromUnit.setOnClickListener(this);
        rlToUnit.setOnClickListener(this);

        inputValue.requestFocus();
        //inputValue.setTextIsSelectable(false);

        inputValue.setShowSoftInputOnFocus(false);

        inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        outputValue.setOnLongClickListener(this);

        //Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        //AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_currency_converter, container, false);
        this.context = this.getActivity();
        initCurrencies();
        initUILayout(view);
        onChooseCurrencyListener = this;

        prefs = Preferences.getInstance(context);
        utils = new Utils();

        try {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorHeader));
        } catch (Exception ignored) {
        }

/*        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setIcon(R.drawable.ic_currency_converter);
        getActivity().setTitle("");
        TextView title = view.findViewById(R.id.title);*/

        outputValue.addTextChangedListener(outputTextSizeAdjuster);

        //   title.setText(String.format("   %s", context.getString(R.string.currency_converter)));

        currencyRates = new HashMap<>();

        onChooseCurrency(currencies.get(prefs.getFromCurrencyIndex()), "FROM");
        onChooseCurrency(currencies.get(prefs.getToCurrencyIndex()), "TO");
        refreshCurrencyRates(false);
        displayRates();

        lastRefreshTime.setText(AppConst.simpleDateFormat.format(prefs.getCurrencyLastUpdateDate()));

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


        return view;
    }

    public void startAnimationImageView(ImageView img) {
        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(700);

        img.startAnimation(anim);
    }

    public void refreshCurrencyRates(boolean forceRefresh) {

        if (utils.isNetworkAvailable(context)) {

            AsyncTask asyncFetchCurrency = new AsyncTask() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    startAnimationImageView(refresh);
                }

                @Override
                protected Void doInBackground(Object[] objects) {
                    fetchCurrencyData(forceRefresh);
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    lastRefreshTime.setText(AppConst.simpleDateFormat.format(prefs.getCurrencyLastUpdateDate()));
                    refresh.setAnimation(null);
                    displayRates();
                }
            };
            asyncFetchCurrency.execute();
        } else {
            if (forceRefresh)
                Toast.makeText(context, "No Internet connectivity.", Toast.LENGTH_SHORT).show();

            String last_response = prefs.getCurrencyResponse();

            if (!last_response.equals("")) {
                try {
                    JSONObject json = new JSONObject(last_response).getJSONObject("rates");
                    //Log.d("SHRIKI","JSON:" + json);

                    Iterator<String> iter = json.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            float rate = NumberUtils.parseFloat(json.get(key).toString());
                            //Log.d("SHRIKI","Key->Value: " + key + "->" + rate);
                            currencyRates.put(key, rate);
                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onChooseCurrency(Currency c, String type) {
        //Log.d("SHRIKI","Clicked currency in frag -> " + c.getCountry() + " Type: " + type);
        hideBottomSheet();
        if (type.equals("FROM")) {
            fromFlag.setImageResource(c.getFlagImageResource());
            fromCurrency.setText(c.getCurrencyName());
            fromCurrencyISO.setText(c.getCurrencyISOCode());
            prefs.setFromCurrencyIndex(currencies.indexOf(c));
        } else if (type.equals("TO")) {
            toFlag.setImageResource(c.getFlagImageResource());
            toCurrency.setText(c.getCurrencyName());
            toCurrencyISO.setText(c.getCurrencyISOCode());
            prefs.setToCurrencyIndex(currencies.indexOf(c));
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
        }
    }

    public String getAndValidateResponse(String url, String access_key) {
        String response;
        if (access_key != null && !access_key.isEmpty()) {
            url = url + "?apikey=" + access_key;
        }

        //Log.d("SHRIKI", "URL inside getAndValidateResponse:" + url);

        HttpHandler handle = new HttpHandler();
        response = handle.makeServiceCall(url);
        //Log.d("SHRIKI", "Response inside getAndValidateResponse:" + response);

        try {
            new JSONObject(response).getJSONObject("data");
        } catch (Exception e) {
            //Log.d("SHRIKI", "Exception: " + e.getMessage());
            return null;
        }
        return response;
    }

    public void fetchCurrencyData(boolean forced) {
        Calendar calNow = Calendar.getInstance();
        long current_time = calNow.getTimeInMillis();
        String last_response = prefs.getCurrencyResponse();
        String response;

        long last_refresh_time = prefs.getCurrencyLastUpdateDate();
        int hours_since_refresh = (int) (current_time - last_refresh_time) / (1000 * 60);
        //hours_since_refresh = 20;

        //Log.d("SHRIKI","current_time & last_refresh_time " + current_time + " " + last_refresh_time);
        //Log.d("SHRIKI","Hours since last refresh:" + hours_since_refresh);

        // Refresh data only when it is older than 12 hours and stored response is not empty
        // Or when forced flag is true
        if ((hours_since_refresh > 12 || last_response.equals("")) || forced) {

            response = getAndValidateResponse(exchangeUrl1, exchangeUrl1Key1);
            if (response == null) {
                response = getAndValidateResponse(exchangeUrl2, exchangeUrl2Key1);
            }
            if (response == null) {
                response = getAndValidateResponse(exchangeUrl3, exchangeUrl3Key1);
            }
            if (response == null) {
                response = getAndValidateResponse(exchangeUrl4, exchangeUrl4Key1);
            }

            prefs.setCurrencyLastUpdateDate(current_time);
            prefs.setCurrencyResponse(response);

        } else {
            response = last_response;
        }

        try {
            JSONObject json = new JSONObject(response).getJSONObject("data");
            //Log.d("SHRIKI", "JSON:" + json);

            Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    float rate = NumberUtils.parseFloat(json.get(key).toString());
                    //Log.d("SHRIKI","Key->Value: " + key + "->" + rate);
                    currencyRates.put(key, rate);
                } catch (JSONException e) {
                    // Something went wrong!
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}