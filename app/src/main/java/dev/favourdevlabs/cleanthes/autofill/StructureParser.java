package dev.favourdevlabs.cleanthes.autofill;

import android.app.assist.AssistStructure;
import android.text.InputType;
import android.view.View;
import android.view.autofill.AutofillId;

public class StructureParser {

    // This is what we're trying to find after parsing
    public static class ParsedFields {
        public AutofillId usernameId;  // the AutofillId of the email/username field
        public AutofillId passwordId;  // the AutofillId of the password field
        public String packageName;     // which app owns this screen e.g. "com.google.android.gm"
        public String webDomain;       // if it's a browser, the actual site domain
    }

    public static ParsedFields parse(AssistStructure structure) {
        ParsedFields result = new ParsedFields();

        // An AssistStructure can have multiple windows (e.g. a dialog over an activity)
        // We iterate all of them
        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);

            // The window title is formatted as "packageName/ActivityName"
            // Split on "/" to get just the package name
            String title = windowNode.getTitle() != null
                    ? windowNode.getTitle().toString()
                    : "";
            if (title.contains("/")) {
                result.packageName = title.split("/")[0];
            }

            // Now walk the entire view tree starting from the root
            traverseNode(windowNode.getRootViewNode(), result);
        }

        return result;
    }

    private static void traverseNode(AssistStructure.ViewNode node, ParsedFields result) {
        if (node == null) return;

        // If this node is inside a browser WebView, it has a webDomain
        if (node.getWebDomain() != null) {
            result.webDomain = node.getWebDomain();
        }

        // --- Primary detection: autofillHints ---
        // This is the reliable way. App developers label their fields.
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint : hints) {
                if (isUsernameHint(hint)) {
                    result.usernameId = node.getAutofillId();
                }
                if (isPasswordHint(hint)) {
                    result.passwordId = node.getAutofillId();
                }
            }
        }

        // --- Fallback detection: inputType ---
        // Many apps don't set autofillHints. Check the keyboard type instead.
        // A password field will have TYPE_TEXT_VARIATION_PASSWORD set.
        if (result.passwordId == null) {
            int inputType = node.getInputType();
            boolean isPassword =
                    (inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0
                    || (inputType & InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0
                    || (inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0;

            if (isPassword) {
                result.passwordId = node.getAutofillId();
            }
        }

        // --- Fallback detection: field name/hint text ---
        // Some apps use hint text like "Enter email" — check that too
        if (result.usernameId == null) {
            CharSequence hint = node.getHint();
            if (hint != null) {
                String hintStr = hint.toString().toLowerCase();
                if (hintStr.contains("email")
                        || hintStr.contains("username")
                        || hintStr.contains("user name")
                        || hintStr.contains("phone")) {
                    result.usernameId = node.getAutofillId();
                }
            }
        }

        // Recurse into children
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChildAt(i), result);
        }
    }

    private static boolean isUsernameHint(String hint) {
        return hint.equalsIgnoreCase(View.AUTOFILL_HINT_USERNAME)
                || hint.equalsIgnoreCase(View.AUTOFILL_HINT_EMAIL_ADDRESS)
                || hint.equalsIgnoreCase(View.AUTOFILL_HINT_PHONE)
                || hint.equalsIgnoreCase("email")
                || hint.equalsIgnoreCase("username");
    }

    private static boolean isPasswordHint(String hint) {
        return hint.equalsIgnoreCase(View.AUTOFILL_HINT_PASSWORD)
                || hint.equalsIgnoreCase("password")
                || hint.equalsIgnoreCase("current-password");
    }
}
