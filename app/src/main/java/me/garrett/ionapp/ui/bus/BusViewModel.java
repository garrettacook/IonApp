package me.garrett.ionapp.ui.bus;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BusViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public BusViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is bus fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}