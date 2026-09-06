package net.smartlogic.unitconverter.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.activity.SettingsActivity;

public class ExploreFragment extends Fragment {

    public ExploreFragment() {
    }

    public static ExploreFragment newInstance() {
        return new ExploreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btn_open_settings).setOnClickListener(v -> {
            Intent settingsActivity = new Intent(requireContext(), SettingsActivity.class);
            startActivity(settingsActivity);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }
}
