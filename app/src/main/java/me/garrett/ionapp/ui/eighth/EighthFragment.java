package me.garrett.ionapp.ui.eighth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.garrett.ionapp.databinding.FragmentEighthBinding;

public class EighthFragment extends Fragment {

    private FragmentEighthBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        EighthViewModel eighthViewModel =
                new ViewModelProvider(this).get(EighthViewModel.class);

        binding = FragmentEighthBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textEighth;
        eighthViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}