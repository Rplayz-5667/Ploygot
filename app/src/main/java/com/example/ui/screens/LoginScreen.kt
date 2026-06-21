package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showGooglePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWarm)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .background(Color.White, RoundedCornerShape(32.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Header Title (Linguist Serif)
            Text(
                text = "Your Path",
                style = MaterialTheme.typography.labelLarge,
                color = DeepGreen,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Polyglot",
                style = MaterialTheme.typography.displayLarge,
                color = DarkOlive,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Master over 150 languages using spaced repetition flashcards and accent conversations.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Tabs toggle: Email Login vs Sign up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .background(CreamCard, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { isSignUp = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSignUp) Color.White else Color.Transparent,
                        contentColor = DarkOlive
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = if (!isSignUp) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text("Login", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { isSignUp = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignUp) Color.White else Color.Transparent,
                        contentColor = DarkOlive
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = if (isSignUp) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text("Sign Up", fontWeight = FontWeight.SemiBold)
                }
            }

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "EmailIcon", tint = DeepGreen) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("email_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGreen,
                    unfocusedBorderColor = SandyBorder
                )
            )

            if (isSignUp) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nickname") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "UserIcon", tint = DeepGreen) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("username_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        unfocusedBorderColor = SandyBorder
                    )
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon", tint = DeepGreen) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "TogglePassword",
                            tint = TextGray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("password_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepGreen,
                    unfocusedBorderColor = SandyBorder
                )
            )

            // Loading / Error
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(color = DeepGreen, modifier = Modifier.padding(8.dp))
            } else if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (isSignUp) {
                        authViewModel.signUp(email, username, password)
                    } else {
                        authViewModel.login(email, password)
                    }
                },
                enabled = uiState !is AuthUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("login_button"),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = SandyBorder)
                Text(
                    text = " OR ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Divider(modifier = Modifier.weight(1f), color = SandyBorder)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google SSO Authenticator Button!
            OutlinedButton(
                onClick = { showGooglePicker = true },
                border = BorderStroke(1.dp, SandyBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkOlive)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Styled Google multi-color visual representation
                    Row(modifier = Modifier.padding(end = 12.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEA4335)))
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4285F4)))
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFBBC05)))
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF34A853)))
                    }
                    Text(
                        text = "Sign in with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Google Account Picker Dialog
    if (showGooglePicker) {
        Dialog(onDismissRequest = { showGooglePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Branding
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "o",
                            color = Color(0xFFEA4335),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "o",
                            color = Color(0xFFFBBC05),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "g",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "l",
                            color = Color(0xFF34A853),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "e",
                            color = Color(0xFFEA4335),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Text(
                        text = "Choose an account to continue to Polyglot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Account list mapping
                    val googleAccounts = listOf(
                        Triple("Roshani Patel", "roshani.patel@gmail.com", "🦁"),
                        Triple("Alex Scholar", "alex.scholar@gmail.com", "🦊"),
                        Triple("Sam Linguist", "sam.linguist@gmail.com", "🦉")
                    )

                    googleAccounts.forEach { (name, emailAddr, emoji) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showGooglePicker = false
                                    authViewModel.handleGoogleAuth(emailAddr, name, emoji)
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BgWarm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                Text(emailAddr, fontSize = 12.sp, color = TextGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showGooglePicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = DeepGreen),
                        elevation = null,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
