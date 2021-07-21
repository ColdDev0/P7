package com.openclassroom.go4lunch.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.openclassroom.go4lunch.model.User;
import com.openclassroom.go4lunch.utils.ex.ViewModelEX;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import com.google.firebase.auth.FirebaseUser;

public class UserInfoViewModel extends ViewModelEX {
    MutableLiveData<User> currentUser = new MutableLiveData<>();

    MutableLiveData<List<User>> mUserList;
    Observer<List<User>> mUserListObserver;

    public MutableLiveData<List<User>> getUserList() {
        return mUserList;
    }

    public MutableLiveData<User> getCurrentUser() {
        return currentUser;
    }

    public void updateUserList()
    {
        getRepository().updateUserList();
    }

    public interface OnUpdateListListener
    {
        void onUserListUpdated(List<User> userList);
    }

    public void updateUserList(OnUpdateListListener listener)
    {
        getRepository().updateUserList();
        getRepository().getUsersListLiveData().observeForever(listener::onUserListUpdated);
    }

    public UserInfoViewModel(@NonNull @NotNull Application application) {
        super(application);

        getRepository().updateUserList();

        mUserList = getRepository().getUsersListLiveData();

        mUserListObserver = users -> {
            FirebaseUser firebaseUser = getRepository().getCurrentUser();
            for (User user : Objects.requireNonNull(getRepository().getUsersListLiveData().getValue())) {
                if (user.getUid().equals(firebaseUser.getUid()))
                    currentUser.setValue(user);
            }
        };

        mUserList.observeForever(mUserListObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mUserList.removeObserver(mUserListObserver);
    }
}