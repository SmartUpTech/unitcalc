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
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.adapter.UnitAdapter;
import net.smartlogic.unitconverter.graphy.integration.ConversionGraphAdapter;
import net.smartlogic.unitconverter.graphy.model.GraphyOutput;
import net.smartlogic.unitconverter.helper.Preferences;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.Unit;
import net.smartlogic.unitconverter.theme.ThemeApplier;
import net.smartlogic.unitconverter.theme.ThemeManager;
import net.smartlogic.unitconverter.utils.Conversions;
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
    private View categorySelector;
    private ImageView categoryIcon;
    private TextView categoryTitle, unitRate;
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
        subtract.setVisibility(View.VISIBLE);

        dot = view.findViewById(R.id.dot);
        double_zero = view.findViewById(R.id.double_zero);

        inputValue = view.findViewById(R.id.input);
        outputValue = view.findViewById(R.id.output);

        inputSymbol = view.findViewById(R.id.inputSymbol);
        outputSymbol = view.findViewById(R.id.outputSymbol);

        categorySelector = view.findViewById(R.id.category_selector);
        categoryIcon = view.findViewById(R.id.category_icon);
        categoryTitle = view.findViewById(R.id.category_title);
        unitRate = view.findViewById(R.id.unit_rate);
        fromUnit = view.findViewById(R.id.fromUnit);
        toUnit = view.findViewById(R.id.toUnit);
        for (int id : new int[]{R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.double_zero,
                R.id.dot, R.id.minus, R.id.reverse, R.id.backspace,
                R.id.fromUnit, R.id.toUnit, R.id.category_selector}) {
            InteractionFeedbackManager.configure(view.findViewById(id));
        }

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
                ClipData clip = ClipData.newPlainText(getString(R.string.brand_name), ((EditText) view).getText().toString());
                clipboard.setPrimaryClip(clip);
                showToast(R.string.toast_copied_positive);
            } else if (id == R.id.backspace) {
                boolean hadInput = inputValue.length() > 0;
                inputValue.setText("");
                convertAndDisplay("");
                if (hadInput) InteractionFeedbackManager.perform(view, InteractionFeedbackManager.Type.ACTION);
                return true;
            }
        }
        catch (Exception ignored) { }
        return false;
    }

    public void onClick(View view) {

        try {
            int id = view.getId();
            String previous = inputValue.getText().toString();
            if (id == R.id.reverse) {
                int fromPos = fromUnit.getSelectedItemPosition();
                int toPos = toUnit.getSelectedItemPosition();
                fromUnit.setSelection(toPos);
                toUnit.setSelection(fromPos);
            } else if (id == R.id.backspace) {
                if (previous.isEmpty()) return;
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
                    ClipData clip = ClipData.newPlainText(getString(R.string.brand_name), result);
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
            if (id == R.id.reverse || !previous.equals(inputValue.getText().toString())) {
                InteractionFeedbackManager.perform(view,
                        id == R.id.reverse || id == R.id.backspace
                                ? InteractionFeedbackManager.Type.ACTION
                                : InteractionFeedbackManager.Type.INPUT);
            }
        }
        catch(Exception e) {
            //Log.d("SHRIKI", e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ThemeApplier.apply(getView());
        updateCategoryPicker();
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

        categorySelector.setOnClickListener(v -> showCategoryPicker());
        initialDropDown(conversions.getById(mPrefs.getLastConversion()));
        updateCategoryPicker();
        updateInputType(getSelectedCategoryId());

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
            }
        };

        inputValue.addTextChangedListener(inputTextWatcher);
        ThemeApplier.apply(view);

        return view;
    }

    private void convertAndDisplay(String in) {

        //Log.d("SHRIKI","Convert value: " + in + " from " + fromUnit.getSelectedItem() + " to " + toUnit.getSelectedItem());
        //Log.d("SHRIKI","Convert value: " + in + " from " + fromUnit.getSelectedItemPosition() + " to " + toUnit.getSelectedItemPosition());

        Unit from = selectedConversion.getUnits().get(fromUnit.getSelectedItemPosition());
        Unit to = selectedConversion.getUnits().get(toUnit.getSelectedItemPosition());
        String fromRateSymbol = from.getSymbol();
        if (fromRateSymbol == null || fromRateSymbol.isEmpty()) fromRateSymbol = String.valueOf(from.getId());
        String toRateSymbol = to.getSymbol();
        if (toRateSymbol == null || toRateSymbol.isEmpty()) toRateSymbol = String.valueOf(to.getId());
        double oneUnit = selectedConversion.getId() == Conversion.TEMPERATURE
                ? conversions.convertTemperatureValue(1, from, to)
                : selectedConversion.getId() == Conversion.FUEL
                    ? conversions.convertFuelValue(1, from, to)
                    : conversions.convert(1, from, to);
        unitRate.setText(getString(R.string.converter_unit_rate,
                getString(R.string.one), fromRateSymbol, applyFormatting(oneUnit), toRateSymbol));


        /*Unit from = selectedConversion.getUnitByLabelResource(fromUnit.getSelectedItem());
        Unit to = selectedConversion.getUnitByLabelResource(toUnit.getSelectedItem()); */

        //Log.d("SHRIKI","Convert value: " + in + " from " + context.getString(from.getLabelResource()) + " to " + context.getString(to.getLabelResource()));

        double input;
        try { input = NumberUtils.parseDouble(in); }
        catch (NumberFormatException invalid) {
            if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
            return;
        }
        if (!Double.isFinite(input)) {
            if (getParentFragment() instanceof ConverterFragment converter) converter.clearConversionGraphy();
            return;
        }
        double result;

        String fromSym = from.getSymbol();
        if (fromSym == null || fromSym.isEmpty()) fromSym = String.valueOf(from.getId());
        inputSymbol.setText(fromSym);

        String toSym = to.getSymbol();
        if (toSym == null || toSym.isEmpty()) toSym = String.valueOf(to.getId());
        outputSymbol.setText(toSym);

        if (selectedConversion.getId() == Conversion.TEMPERATURE) {
            result = conversions.convertTemperatureValue(input,from, to);
            result = in.isEmpty() ? 0 : result;
        }
        else if (selectedConversion.getId() == Conversion.FUEL) {
            result = conversions.convertFuelValue(input,from, to);
        }
        else
            result = conversions.convert(input,from, to);

        String finalStr = applyFormatting(result);

        outputValue.setText(finalStr);
        updateParentGraphy(in, from, to, fromSym, toSym, finalStr, result);
    }

    private void updateParentGraphy(String in, Unit from, Unit to, String fromSym,
                                    String toSym, String finalStr, double result) {
        if (!(getParentFragment() instanceof ConverterFragment converter)) return;
        if (in.trim().isEmpty() || in.equals("-")) {
            converter.clearConversionGraphy();
            return;
        }
        GraphyOutput output = ConversionGraphAdapter.unit(requireContext(), conversions,
                selectedConversion.getId(), from, to, NumberUtils.parseDouble(in), result,
                in + " " + fromSym, finalStr + " " + toSym, fromSym, toSym);
        converter.updateConversionGraphy(output, output.getExpression());
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
        View.OnTouchListener selectionFeedback = (selector, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                InteractionFeedbackManager.perform(selector, InteractionFeedbackManager.Type.ACTION);
            }
            return false;
        };
        fromUnit.setOnTouchListener(selectionFeedback);
        toUnit.setOnTouchListener(selectionFeedback);

        fromUnit.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getChildCount() > 0 && parent.getChildAt(0) instanceof TextView label) {
                    label.setTextColor(ThemeManager.get().mainText());
                }
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
                if (parent.getChildCount() > 0 && parent.getChildAt(0) instanceof TextView label) {
                    label.setTextColor(ThemeManager.get().mainText());
                }
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

    public int getSelectedCategoryId() {
        if (selectedConversion != null) {
            return selectedConversion.getId();
        }
        return mPrefs != null ? mPrefs.getLastConversion() : Conversion.LENGTH;
    }

    private void showCategoryPicker() {
        PopupMenu menu = new PopupMenu(requireContext(), categorySelector);
        for (int categoryId = Conversion.LENGTH; categoryId < Conversion.CURRENCY; categoryId++) {
            Conversion category = conversions.getById(categoryId);
            menu.getMenu().add(0, categoryId, categoryId, category.getLabelResource())
                    .setCheckable(true).setChecked(categoryId == getSelectedCategoryId());
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == getSelectedCategoryId()) return true;
            selectCategory(item.getItemId());
            InteractionFeedbackManager.perform(categorySelector, InteractionFeedbackManager.Type.ACTION);
            return true;
        });
        menu.show();
    }

    private void updateCategoryPicker() {
        if (selectedConversion == null || categorySelector == null) return;
        categoryTitle.setText(selectedConversion.getLabelResource());
        categoryIcon.setImageResource(selectedConversion.getImageResource());
        categoryIcon.setColorFilter(ThemeManager.get().mainText());
        categorySelector.setContentDescription(getString(selectedConversion.getLabelResource()));
    }

    private void updateInputType(int categoryId) {
        inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | (categoryId == Conversion.TEMPERATURE ? InputType.TYPE_NUMBER_FLAG_SIGNED : 0));
        inputValue.setShowSoftInputOnFocus(false);
    }

    public void selectCategory(int categoryId) {
        if (categorySelector == null || conversions == null) return;
        updateInputType(categoryId);
        inputValue.setText("");
        convertAndDisplay("");
        mPrefs.setLastConversion(categoryId);
        mPrefs.setLastFromConversion(0);
        mPrefs.setLastToConversion(1);
        initialDropDown(conversions.getById(categoryId));
        updateCategoryPicker();
        convertAndDisplay(inputValue.getText().toString());
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
