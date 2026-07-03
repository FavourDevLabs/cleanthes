package dev.favourdevlabs.cleanthes.feature.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

object StructureParser {

    data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var repeatPasswordId: AutofillId? = null,
        var packageName: String? = null,
        var webDomain: String? = null,
    )

    private data class Candidate(
        val id: AutofillId,
        val isFocused: Boolean,
        val tier: Int, // 1 = autofillHints, 2 = inputType, 3 = hint text
    )

    private class Accumulator {
        val usernames = mutableListOf<Candidate>()
        val passwords = mutableListOf<Candidate>()
        var packageName: String? = null
        var webDomain: String? = null
    }

    fun parse(structure: AssistStructure): ParsedFields {
        val acc = Accumulator()
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            windowNode.title?.toString()?.let { title ->
                if (title.contains("/")) acc.packageName = title.split("/")[0]
            }
            traverseNode(windowNode.rootViewNode, acc)
        }
        return resolve(acc)
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode?,
        acc: Accumulator,
    ) {
        if (node == null) return

        node.webDomain?.let { acc.webDomain = it }

        if (isSupportedInput(node)) {
            classifyNode(node, acc)
        }

        for (i in 0 until node.childCount) traverseNode(node.getChildAt(i), acc)
    }

    private fun isSupportedInput(node: AssistStructure.ViewNode): Boolean =
        node.className == "android.widget.EditText" ||
            node.className == "android.widget.AutoCompleteTextView"

    private fun classifyNode(
        node: AssistStructure.ViewNode,
        acc: Accumulator,
    ) {
        val id = node.autofillId ?: return
        val focused = node.isFocused

        // --- Tier 1: explicit autofillHints ---
        var matchedTier1 = false
        node.autofillHints?.forEach { hint ->
            if (isUsernameHint(hint)) {
                acc.usernames.add(Candidate(id, focused, tier = 1))
                matchedTier1 = true
            }
            if (isPasswordHint(hint)) {
                acc.passwords.add(Candidate(id, focused, tier = 1))
                matchedTier1 = true
            }
        }
        if (matchedTier1) return

        // --- Tier 2: inputType ---
        val inputType = node.inputType
        val isPasswordType =
            (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
        if (isPasswordType) {
            acc.passwords.add(Candidate(id, focused, tier = 2))
            return
        }

        // --- Tier 3: hint text (heuristic, weakest signal — no "phone") ---
        node.hint?.toString()?.lowercase()?.let { hintStr ->
            if (hintStr.contains("email") ||
                hintStr.contains("username") ||
                hintStr.contains("user name")
            ) {
                acc.usernames.add(Candidate(id, focused, tier = 3))
            }
        }
    }

    private fun isUsernameHint(hint: String): Boolean =
        hint.equals(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
            hint.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true) ||
            hint.equals(View.AUTOFILL_HINT_PHONE, ignoreCase = true) ||
            hint.equals("email", ignoreCase = true) ||
            hint.equals("username", ignoreCase = true)

    private fun isPasswordHint(hint: String): Boolean =
        hint.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) ||
            hint.equals("password", ignoreCase = true) ||
            hint.equals("current-password", ignoreCase = true)

    private val candidatePriority = compareByDescending<Candidate> { it.isFocused }.thenBy { it.tier }

    private fun resolve(acc: Accumulator): ParsedFields {
        val bestUsername = acc.usernames.sortedWith(candidatePriority).firstOrNull()
        val sortedPasswords = acc.passwords.sortedWith(candidatePriority)
        val bestPassword = sortedPasswords.firstOrNull()
        val repeatPassword = sortedPasswords.drop(1).firstOrNull { it.id != bestPassword?.id }

        return ParsedFields(
            usernameId = bestUsername?.id,
            passwordId = bestPassword?.id,
            repeatPasswordId = repeatPassword?.id,
            packageName = acc.packageName,
            webDomain = acc.webDomain,
        )
    }
}

