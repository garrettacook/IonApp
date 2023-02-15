package me.garrett.ionapp.ui.bus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.garrett.ionapp.databinding.FragmentBusBinding;

public class BusFragment extends Fragment {

    private FragmentBusBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        BusViewModel busViewModel =
                new ViewModelProvider(this).get(BusViewModel.class);

        binding = FragmentBusBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textBus;
        busViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}