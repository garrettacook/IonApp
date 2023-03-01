package me.garrett.ionapp.ui.announcements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.openid.appauth.AuthorizationService;

import me.garrett.ionapp.AnnouncementFragment;
import me.garrett.ionapp.R;
import me.garrett.ionapp.api.Announcement;
import me.garrett.ionapp.api.IonApi;
import me.garrett.ionapp.databinding.FragmentAnnouncementsBinding;

public class AnnouncementsFragment extends Fragment {

    private FragmentAnnouncementsBinding binding;

    private AuthorizationService authService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAnnouncementsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        authService = new AuthorizationService(getContext());

        IonApi.getInstance(getContext()).getAnnouncements(authService, 1).thenAccept(announcements -> {

            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            for (Announcement announcement : announcements) {
                AnnouncementFragment fragment = AnnouncementFragment.newInstance(announcement);

                transaction.add(R.id.announcements_container, fragment);
            }

            transaction.commit();

        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        authService.dispose();
        binding = null;
    }
}