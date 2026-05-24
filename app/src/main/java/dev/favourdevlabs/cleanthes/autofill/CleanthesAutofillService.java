package dev.favourdevlabs.cleanthes.autofill;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;

import java.util.List;
import javax.crypto.SecretKey;

public class CleanthesAutofillService extends AutofillService {

	@Override
	public void onFillRequest(
			@NonNull FillRequest request,
			@NonNull CancellationSignal signal,
			@NonNull FillCallback callback) {

		List<FillContext> contexts = request.getFillContexts();
		AssistStructure structure = contexts.get(contexts.size() - 1).getStructure();
		StructureParser.ParsedFields parsed = StructureParser.parse(structure);

		if (parsed.usernameId == null || parsed.passwordId == null) {
			callback.onSuccess(null);
			return;
		}

		String key = parsed.webDomain != null ? parsed.webDomain : parsed.packageName;

		Intent authIntent = new Intent(this, AutofillAuthActivity.class)
				.putExtra(AutofillAuthActivity.EXTRA_PACKAGE_NAME, parsed.packageName)
				.putExtra(AutofillAuthActivity.EXTRA_WEB_DOMAIN, parsed.webDomain)
				.putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID, parsed.usernameId)
				.putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID, parsed.passwordId);

		PendingIntent pending = PendingIntent.getActivity(
				this,
				key.hashCode(),
				authIntent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		RemoteViews locked = new RemoteViews(getPackageName(), R.layout.autofill_item);
		locked.setTextViewText(R.id.autofill_label, "Cleanthes \u2014 tap to fill");

		FillResponse response = new FillResponse.Builder()
				.setAuthentication(
						new AutofillId[] { parsed.usernameId, parsed.passwordId },
						pending.getIntentSender(),
						locked)
				.setSaveInfo(new SaveInfo.Builder(
						SaveInfo.SAVE_DATA_TYPE_PASSWORD,
						new AutofillId[] { parsed.usernameId, parsed.passwordId }).build())
				.build();

		callback.onSuccess(response);
	}

	@Override
	public void onSaveRequest(
			@NonNull SaveRequest request,
			@NonNull SaveCallback callback) {

		SecretKey secretKey = SessionManager.getSessionKey();
		if (secretKey == null) {
			callback.onSuccess();
			return;
		}

		List<FillContext> contexts = request.getFillContexts();
		AssistStructure structure = contexts.get(contexts.size() - 1).getStructure();
		StructureParser.ParsedFields parsed = StructureParser.parse(structure);

		if (parsed.usernameId == null || parsed.passwordId == null) {
			callback.onSuccess();
			return;
		}

		String username = extractValue(structure, parsed.usernameId);
		String password = extractValue(structure, parsed.passwordId);
		String key = parsed.webDomain != null ? parsed.webDomain : parsed.packageName;

		if (username != null && password != null) {
			try {
				VaultRepository.getInstance(this)
						.addEntry(key, username, password, key, "Autofill", "", false,
								secretKey);
			} catch (Exception ignored) {
			}
		}

		callback.onSuccess();
	}

	private String extractValue(AssistStructure structure, AutofillId target) {
		for (int i = 0; i < structure.getWindowNodeCount(); i++) {
			String v = findValue(structure.getWindowNodeAt(i).getRootViewNode(), target);
			if (v != null)
				return v;
		}
		return null;
	}

	private String findValue(AssistStructure.ViewNode node, AutofillId target) {
		if (target.equals(node.getAutofillId())) {
			AutofillValue v = node.getAutofillValue();
			if (v != null && v.isText())
				return v.getTextValue().toString();
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			String r = findValue(node.getChildAt(i), target);
			if (r != null)
				return r;
		}
		return null;
	}
}
