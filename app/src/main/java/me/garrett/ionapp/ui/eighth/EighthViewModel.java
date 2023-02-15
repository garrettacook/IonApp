package me.garrett.ionapp.ui.eighth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EighthViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public EighthViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is eighth fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}