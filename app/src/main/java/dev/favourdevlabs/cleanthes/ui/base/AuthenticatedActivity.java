package dev.favourdevlabs.cleanthes.ui.base;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;

public abstract class AuthenticatedActivity extends AppCompatActivity {

	@Override
	protected void onStart() {
		super.onStart();
		if (!SessionManager.isUnlocked()) {
			redirectToLogin();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Only refresh — never make redirect decisions here
		if (SessionManager.isUnlocked()) {
			SessionManager.refreshSession();
		}
	}

	protected void redirectToLogin() {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
}
