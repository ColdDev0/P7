package com.openclassroom.go4lunch.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import com.google.firebase.firestore.FirebaseFirestore;
import com.openclassroom.go4lunch.BuildConfig;
import com.openclassroom.go4lunch.R;
import com.openclassroom.go4lunch.model.User;
import com.openclassroom.go4lunch.model.placedetailsapi.DetailsResult;
import com.openclassroom.go4lunch.model.placedetailsapi.PlaceDetailsResponse;
import com.openclassroom.go4lunch.view.recyclerview.ParticipantListAdapter;
import com.openclassroom.go4lunch.databinding.ActivityRestaurantDetailBinding;
import com.openclassroom.go4lunch.viewmodel.UserInfoViewModel;
import com.openclassroom.go4lunch.viewmodel.listener.OnToggledLike;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.openclassroom.go4lunch.repository.Repository.getRepository;

public class RestaurantDetailActivity extends AppCompatActivity {

    ActivityRestaurantDetailBinding mBinding;

    String mCurrentPlaceID = null;
    DetailsResult mDetailsResult = null;
    UserInfoViewModel mUserInfoViewModel;
    private final ParticipantListAdapter mParticipantListAdapter = new ParticipantListAdapter(new ArrayList<>(), this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityRestaurantDetailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mUserInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);

        Intent startingIntent = getIntent();
        mCurrentPlaceID = startingIntent.getStringExtra("placeID");

        updateAppearance();

        configureRecyclerView();
        configureButtons();
    }

    private void updateAppearance() {
        Call<PlaceDetailsResponse> callDetails = getRepository().getRetrofitService().getDetails(mCurrentPlaceID);
        callDetails.enqueue(new Callback<PlaceDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PlaceDetailsResponse> call, @NonNull Response<PlaceDetailsResponse> response) {
                mDetailsResult = Objects.requireNonNull(response.body()).getResult();
                mBinding.restaurantNameDetail.setText(mDetailsResult.getName());
                mBinding.restaurantAddressDetail.setText(mDetailsResult.getFormattedAddress());

                if (mDetailsResult.getPhotos() != null) {
                    String photo_reference = mDetailsResult.getPhotos().get(0).getPhotoReference();
                    if (!photo_reference.equals("")) {
                        Picasso.Builder builder = new Picasso.Builder(RestaurantDetailActivity.this);
                        builder.downloader(new OkHttp3Downloader(RestaurantDetailActivity.this));
                        builder.build().load("https://maps.googleapis.com/maps/api/place/photo?parameters&key=" + BuildConfig.MAPS_API_KEY + "&photoreference=" + photo_reference + "&maxwidth=512&maxheight=512")
                                .into(mBinding.restaurantPhotoDetail);
                    }
                }

                mUserInfoViewModel.updateUserList((currentUser, userList) -> {
                    UpdateLike(currentUser);

                    if (currentUser.getEatingAt() != null && !currentUser.getEatingAt().equals("") && currentUser.getEatingAt().equals(mCurrentPlaceID)) {
                        mBinding.fabEatingAt.setSupportImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
                    } else {
                        mBinding.fabEatingAt.setSupportImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.lightgrey)));
                    }

                    mParticipantListAdapter.clearUserList();
                    for (User user : userList) {
                        if (
                                user.getEatingAt().equals(mCurrentPlaceID) &&
                                        !user.getUid().equals(Objects.requireNonNull(currentUser).getUid())
                        )
                            mParticipantListAdapter.addUserList(user);
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Call<PlaceDetailsResponse> call, @NotNull Throwable t) {

            }
        });

    }

    @SuppressLint("RestrictedApi")
    private void UpdateLike(User currentUser) {
        boolean isLiked = false;
        for (String like : currentUser.getLikeList()) {
            if (like.equals(mCurrentPlaceID)) {
                isLiked = true;
                break;
            }
        }
        if (isLiked) {
            mBinding.likeStar.setSupportImageTintList(ColorStateList.valueOf(RestaurantDetailActivity.this.getResources().getColor(R.color.orange_500)));
        } else
            mBinding.likeStar.setSupportImageTintList(ColorStateList.valueOf(RestaurantDetailActivity.this.getResources().getColor(R.color.lightgrey)));
    }

    private void configureButtons() {
        mBinding.returnButton.setOnClickListener(v -> {
            finish();
        });

        mBinding.layoutClickableCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + mDetailsResult.getFormattedPhoneNumber()));
            startActivity(intent);
        });

        mBinding.layoutClickableLike.setOnClickListener(v -> {
            mUserInfoViewModel.toggleLike(mCurrentPlaceID, result -> {
                mUserInfoViewModel.updateUserList((currentUser, userList) -> {
                    UpdateLike(currentUser);
                });
            });
        });

        mBinding.layoutClickableWebsite.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mDetailsResult.getWebsite()));
            startActivity(browserIntent);
        });
    }

    private void configureRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                linearLayoutManager.getOrientation());

        mBinding.participantList.setLayoutManager(linearLayoutManager);
        mBinding.participantList.setAdapter(mParticipantListAdapter);
        mBinding.participantList.addItemDecoration(dividerItemDecoration);
    }
}