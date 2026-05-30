package net.smartlogic.unitconverter.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.adapter.ConversionAdapter;
import net.smartlogic.unitconverter.adapter.UnitAdapter;
import net.smartlogic.unitconverter.helper.HorizontalListView;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.utils.Conversions;
import net.smartlogic.unitconverter.utils.GenericFunctions;
import net.smartlogic.unitconverter.utils.NumberUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;


public class UnitConverterFragment extends Fragment implements OnClickListener, OnLongClickListener {

    Button one, two, three;
    Button four, five, six, seven, eight, nine, zero;
    Button  subtract;
    Button dot, double_zero;
    EditText inputValue, outputValue;
    TextView inputSymbol, outputSymbol;
    HorizontalListView horizontalListView;
    private Context context;
    private Spinner fromUnit, toUnit;

    Preferences mPrefs;

    CoordinatorLayout mCoordinatorLayout;

    private Conversions conversions;
    private Conversion selectedConversion;

    ImageButton reverse, copy, backspace;

    TextWatcher inputTextWatcher;

    public UnitConverterFragment() {
        // Required empty public constructor
    }

    public static UnitConverterFragment newInstance() {
        UnitConverterFragment fragment = new UnitConverterFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.context = this.getActivity();
        mPrefs = Preferences.getInstance(getActivity());
        //df = getDecimalFormat();
    }

    public void initUILayout(View view) {

        one = view.findViewById(R.id.one);
        two = view.findViewById(R.id.two);
        three = view.findViewById(R.id.three);
        four = view.findViewById(R.id.four);
        five = view.findViewById(R.id.five);
        six = view.findViewById(R.id.six);
        seven = view.findViewById(R.id.seven);
        eight = view.findViewById(R.id.eight);
        nine = view.findViewById(R.id.nine);
        zero = view.findViewById(R.id.zero);

        reverse = view.findViewById(R.id.reverse);
        backspace = view.findViewById(R.id.backspace);
        copy = view.findViewById(R.id.copy);

        subtract = view.findViewById(R.id.minus);

        dot = view.findViewById(R.id.dot);
        double_zero = view.findViewById(R.id.double_zero);


        inputValue = view.findViewById(R.id.input);
        outputValue = view.findViewById(R.id.output);

        inputSymbol = view.findViewById(R.id.inputSymbol);
        outputSymbol = view.findViewById(R.id.outputSymbol);

        horizontalListView = view.findViewById(R.id.horizontal_list);
        fromUnit = view.findViewById(R.id.fromUnit);
        toUnit = view.findViewById(R.id.toUnit);

        mCoordinatorLayout = view.findViewById(R.id.cl);

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
        copy.setOnClickListener(this);

        outputValue.setOnLongClickListener(this);
        backspace.setOnLongClickListener(this);

        inputValue.requestFocus();

        inputValue.setShowSoftInputOnFocus(false);

    }

    public void showToast(int message) {
        Snackbar sb = Snackbar.make(mCoordinatorLayout, message, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundResource(R.color.alwaysDarkText);
        sb.show();
    }

    @Override
    public boolean onLongClick(View view) {

        try {
            int id = view.getId();
            if (id == R.id.output) {
                ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.app_name), ((EditText) view).getText().toString());
                clipboard.setPrimaryClip(clip);
                showToast(R.string.toast_copied_positive);
            } else if (id == R.id.backspace) {
                inputValue.setText("");
                convertAndDisplay("");
            }
        }
        catch (Exception ignored) { }
        return false;
    }

    public void onClick(View view) {

        try {
            int id = view.getId();
            if (id == R.id.reverse) {
                int fromPos = fromUnit.getSelectedItemPosition();
                int toPos = toUnit.getSelectedItemPosition();
                fromUnit.setSelection(toPos);
                toUnit.setSelection(fromPos);
            } else if (id == R.id.backspace) {
                String substr = inputValue.getText().toString().substring(0, inputValue.getText().toString().length() - 1);
                inputValue.setText(substr);
                inputValue.setSelection(inputValue.getText().length());
            } else if (id == R.id.dot) {
                if (inputValue.getText().toString().isEmpty())
                    inputValue.setText("0.");
                else if (inputValue.getText().toString().equals("-"))
                    inputValue.append("0.");
                else
                    inputValue.append(".");
                inputValue.setSelection(inputValue.getText().length());
            } else if (id == R.id.minus) {
                inputValue.append("-");
                inputValue.setSelection(inputValue.getText().length());
            } else if (id == R.id.zero) {
                String currentInput1 = inputValue.getText().toString();
                if (!currentInput1.equals("0")) {
                    inputValue.append("0");
                }
            } else if (id == R.id.double_zero) {
                String currentInput = inputValue.getText().toString();

                if (currentInput.equals("0")) {
                    //Do nothing if already 0 is added and user is trying to add more 0s.
                } else if (currentInput.isEmpty()) {
                    inputValue.append("0");
                } else {
                    inputValue.append("00");
                }
                inputValue.setSelection(inputValue.getText().length());
            } else if (id == R.id.copy) {
                if (!inputValue.getText().toString().isEmpty()) {

                    ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);

                    String result = inputValue.getText().toString() + " " +
                            context.getString(selectedConversion.getUnits().get(fromUnit.getSelectedItemPosition()).getLabelResource()) + " = " +
                            outputValue.getText().toString() + " " +
                            context.getString(selectedConversion.getUnits().get(toUnit.getSelectedItemPosition()).getLabelResource());
                    ClipData clip = ClipData.newPlainText(getString(R.string.app_name), result);
                    clipboard.setPrimaryClip(clip);
                    showToast(R.string.toast_copied_positive);
                } else {
                    showToast(R.string.toast_copied_negative);
                }
            } else {
                Button button = (Button) view;
                String data = button.getText().toString();

                String currentInput2 = inputValue.getText().toString();

                if (currentInput2.equals("0")) {
                    inputValue.setText(data);
                } else
                    inputValue.append(data);
                inputValue.setSelection(inputValue.getText().length());
            }
        }
        catch(Exception e) {
            //Log.d("SHRIKI", e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        convertAndDisplay(inputValue.getText().toString());
    }

    private DecimalFormat getDecimalFormat() {

        DecimalFormat formatter = new DecimalFormat();

        //Set maximum number of decimal places
        formatter.setMaximumFractionDigits(mPrefs.getNumberDecimals());

        //Set group and decimal separators
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setDecimalSeparator(mPrefs.getDecimalSeparator().charAt(0));

        String groupSeparator = mPrefs.getGroupSeparator();
        boolean isSeparatorUsed = !groupSeparator.equals(context.getString(R.string.group_separator_none));
        formatter.setGroupingUsed(isSeparatorUsed);
        if (isSeparatorUsed) {
            symbols.setGroupingSeparator(groupSeparator.charAt(0));
        }

        formatter.setDecimalFormatSymbols(symbols);
        return formatter;
    }

    public String applyFormatting(double res) {
        String s;
        try {
            s = getDecimalFormat().format(res);
        }
        catch (Exception e){
            s = "";
        }
        return s;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_unit_converter, container, false);

        conversions = Conversions.getInstance();
        selectedConversion = new Conversion();

        initUILayout(view);

        Integer[] mCustomData = {0,1,2,3,4,5,6,7,8,9,10,11,12,13};

        final ConversionAdapter adapter = new ConversionAdapter(requireActivity(), mCustomData);

        horizontalListView.setAdapter(adapter);
        horizontalListView.setOnItemClickListener((parent, view1, position, id) -> {

            if (position == Conversion.TEMPERATURE) {
                inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
            else {
                inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
            inputValue.setText("");
            convertAndDisplay("");
            mPrefs.setLastConversion(position);
            mPrefs.setLastFromConversion(0);
            mPrefs.setLastToConversion(1);
            initialDropDown(conversions.getById(position));
            convertAndDisplay(inputValue.getText().toString());
            adapter.setSelectedItem(position);
            adapter.notifyDataSetChanged();
        });

        adapter.setSelectedItem(mPrefs.getLastConversion());
        adapter.notifyDataSetChanged();

        initialDropDown(conversions.getById(mPrefs.getLastConversion()));

        inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        inputTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                convertAndDisplay(s.toString());
                GenericFunctions.adjustTextSize(inputValue, 21);
            }
        };

        inputValue.addTextChangedListener(inputTextWatcher);

        GenericFunctions.adjustTextSize(inputValue, 21);
        GenericFunctions.adjustTextSize(outputValue, 21);

        return view;
    }

    private void convertAndDisplay(String in) {

        //Log.d("SHRIKI","Convert value: " + in + " from " + fromUnit.getSelectedItem() + " to " + toUnit.getSelectedItem());
        //Log.d("SHRIKI","Convert value: " + in + " from " + fromUnit.getSelectedItemPosition() + " to " + toUnit.getSelectedItemPosition());

        Unit from = selectedConversion.getUnits().get(fromUnit.getSelectedItemPosition());
        Unit to = selectedConversion.getUnits().get(toUnit.getSelectedItemPosition());

        /*Unit from = selectedConversion.getUnitByLabelResource(fromUnit.getSelectedItem());
        Unit to = selectedConversion.getUnitByLabelResource(toUnit.getSelectedItem()); */

        //Log.d("SHRIKI","Convert value: " + in + " from " + context.getString(from.getLabelResource()) + " to " + context.getString(to.getLabelResource()));

        double result;

        String fromSym = from.getSymbol();
        if (fromSym == null || fromSym.isEmpty()) fromSym = String.valueOf(from.getId());
        inputSymbol.setText(fromSym);

        String toSym = to.getSymbol();
        if (toSym == null || toSym.isEmpty()) toSym = String.valueOf(to.getId());
        outputSymbol.setText(toSym);

        if (selectedConversion.getId() == Conversion.TEMPERATURE) {
            result = conversions.convertTemperatureValue(NumberUtils.parseDouble(in),from, to);
            result = in.isEmpty() ? 0 : result;
        }
        else if (selectedConversion.getId() == Conversion.FUEL) {
            result = conversions.convertFuelValue(NumberUtils.parseDouble(in),from, to);
        }
        else
            result = conversions.convert(NumberUtils.parseDouble(in),from, to);

        String finalStr = applyFormatting(result);

        outputValue.setText(finalStr);
        GenericFunctions.adjustTextSize(outputValue, 21);
    }

    public void initialDropDown(Conversion s) {

        selectedConversion = s;
        //Log.d("SHRIKI", "------Conversion: " + getString(s.getLabelResource()));

        List<Unit> unitList = s.getUnits();

        /*Log.d("SHRIKI", "--------Units: " + unitList.size());
        for (int i=0; i < unitList.size(); i++) {
            Log.d("SHRIKI", "Unit-"+ i + ": "+ getString(unitList.get(i).getLabelResource()));
        }*/

        UnitAdapter unitAdapter = new UnitAdapter(context,unitList);

        fromUnit.setAdapter(unitAdapter);
        toUnit.setAdapter(unitAdapter);

        fromUnit.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(ContextCompat.getColor(requireActivity(),R.color.display_result_text_color));
                convertAndDisplay(inputValue.getText().toString());
                mPrefs.setLastFromConversion(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        toUnit.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(ContextCompat.getColor(requireActivity(),R.color.display_result_text_color));
                convertAndDisplay(inputValue.getText().toString());
                mPrefs.setLastToConversion(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fromUnit.setSelection(mPrefs.getLastFromConversion());
        toUnit.setSelection(mPrefs.getLastToConversion());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
}
