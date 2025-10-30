package pl.edu.pjwstk.engineeringthesis.font

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import pl.edu.pjwstk.engineeringthesis.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interFamily = FontFamily(
    Font(GoogleFont("Inter"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Inter"), provider, weight = FontWeight.SemiBold),
    Font(GoogleFont("Inter"), provider, weight = FontWeight.Bold),
    Font(GoogleFont("Inter"), provider, weight = FontWeight.Medium),

)
