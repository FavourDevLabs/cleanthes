package dev.favourdevlabs.cleanthes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import dev.favourdevlabs.cleanthes.ui.theme.*

@Composable
fun CleanthesPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value                  = value,
        onValueChange          = onValueChange,
        label                  = { Text(label) },
        singleLine             = true,
        enabled                = enabled,
        visualTransformation   = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions        = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction    = imeAction,
        ),
        keyboardActions        = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
        trailingIcon           = {
            IconButton(onClick = onVisibilityToggle, enabled = enabled) {
                Icon(
                    imageVector        = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint               = if (enabled) TextMuted else TextMuted.copy(alpha = 0.4f),
                )
            }
        },
        colors                 = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = GoldPrimary,
            unfocusedBorderColor  = TextMuted.copy(alpha = 0.35f),
            disabledBorderColor   = TextMuted.copy(alpha = 0.15f),
            focusedLabelColor     = GoldPrimary,
            unfocusedLabelColor   = TextMuted,
            disabledLabelColor    = TextMuted.copy(alpha = 0.4f),
            cursorColor           = GoldPrimary,
            focusedTextColor      = TextPrimary,
            unfocusedTextColor    = TextPrimary,
            disabledTextColor     = TextPrimary.copy(alpha = 0.4f),
        ),
        modifier               = modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordStrengthBar(score: Int, modifier: Modifier = Modifier) {
    val segmentColors = listOf(
        if (score >= 1) StrengthVeryWeak   else SurfaceModal,
        if (score >= 2) StrengthWeak       else SurfaceModal,
        if (score >= 3) StrengthFair       else SurfaceModal,
        if (score >= 4) StrengthStrong     else SurfaceModal,
        if (score >= 5) StrengthVeryStrong else SurfaceModal,
    )
    Row(
        modifier              = modifier.fillMaxWidth().height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segmentColors.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
