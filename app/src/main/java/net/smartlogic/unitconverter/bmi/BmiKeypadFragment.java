package net.smartlogic.unitconverter.bmi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.helper.InteractionFeedbackManager;
import net.smartlogic.unitconverter.theme.ThemeApplier;

/** Embedded, collapsible keypad using the converter's canonical keys and equals style. */
public final class BmiKeypadFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.converter_keypad, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        view.setPadding(0, 0, 0, 0);
        View copy = view.findViewById(R.id.copy);
        ViewGroup copyRow = (ViewGroup) copy.getParent();
        int copyIndex = copyRow.indexOfChild(copy);
        ViewGroup.LayoutParams copyParams = copy.getLayoutParams();
        copyRow.removeView(copy);
        copyRow.addView(new android.widget.Space(requireContext()), copyIndex, copyParams);
        View minus = view.findViewById(R.id.minus);
        ViewGroup row = (ViewGroup) minus.getParent();
        int index = row.indexOfChild(minus);
        ViewGroup.LayoutParams params = minus.getLayoutParams();
        row.removeView(minus);
        View done = getLayoutInflater().inflate(R.layout.bmi_keypad_done, row, false);
        row.addView(done, index, params);
        for (int id : new int[]{R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four,
                R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.double_zero,
                R.id.dot, R.id.ac, R.id.backspace, R.id.equal}) {
            View key = view.findViewById(id);
            InteractionFeedbackManager.configure(key);
            key.setOnClickListener(v -> {
                InteractionFeedbackManager.perform(v, InteractionFeedbackManager.Type.INPUT);
                ((BmiCalculatorFragment) requireParentFragment()).onKeypadKey(id);
            });
        }
        ThemeApplier.apply(view);
    }
}
