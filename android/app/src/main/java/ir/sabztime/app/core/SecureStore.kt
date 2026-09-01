package ir.sabztime.app.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private val Context.secureDataStore by preferencesDataStore(name = "sabz_secure")

/**
 * ذخیره امن توکن نشست:
 * - کلید AES/GCM داخل Android Keystore ساخته می‌شود (خارج از دستگاه قابل استخراج نیست)
 * - مقدار رمزشده (IV + ciphertext) در DataStore ذخیره می‌شود
 */
class SecureStore(private val context: Context) {

    private val dataStore = context.secureDataStore

    private suspend fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private suspend fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    private suspend fun decrypt(encoded: String): String? {
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val ct = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            null // کلید عوض شده (مثلاً restore) → نشست نامعتبر
        }
    }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[KEY_TOKEN] = encrypt(token) }
    }

    suspend fun token(): String? {
        val raw = dataStore.data.first()[KEY_TOKEN] ?: return null
        return decrypt(raw)?.takeIf { it.isNotBlank() }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_TOKEN) }
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sabztime_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val KEY_TOKEN = stringPreferencesKey("app_token")
    }
}
