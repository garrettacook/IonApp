package me.garrett.ionapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import me.garrett.ionapp.api.Announcement;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AnnouncementFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AnnouncementFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_ANNOUNCEMENT = "announcement";

    private Announcement mAnnouncement;

    public AnnouncementFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param announcement Announcement.
     * @return A new instance of fragment AnnouncementFragment.
     */
    public static AnnouncementFragment newInstance(Announcement announcement) {
        AnnouncementFragment fragment = new AnnouncementFragment();
        Bundle args = new Bundle();
        announcement.putInBundle(args);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAnnouncement = Announcement.getFromBundle(getArguments());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_announcement, container, false);

        ((TextView) root.findViewById(R.id.title)).setText(mAnnouncement.getTitle());
        ((TextView) root.findViewById(R.id.content)).setText(mAnnouncement.getContent());

        return root;
    }
}