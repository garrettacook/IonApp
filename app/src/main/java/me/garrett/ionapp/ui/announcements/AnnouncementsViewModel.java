package me.garrett.ionapp.ui.announcements;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AnnouncementsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AnnouncementsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is announcements fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}