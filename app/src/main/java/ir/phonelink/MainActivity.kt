
package ir.phonelink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.database.Cursor
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var info: TextView
    private val prefs by lazy {
        getSharedPreferences("phone_link", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPermissionPage()
    }

    private fun setupPage(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 42, 36, 36)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        root.addView(TextView(this).apply {
            text = "Alzahra Univercity"
            textSize = 26f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 8, 0, 24)
        })

        root.addView(TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 8, 0, 18)
        })

        info = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF172033.toInt())
        }

        setContentView(root)
    }

    private fun addButton(label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun showPermissionPage() {
        setupPage("دسترسی‌های برنامه")

        root.addView(TextView(this).apply {
            text = "برای استفاده از برنامه، پوشه موردنظر خود را انتخاب کنید و در صورت نیاز دسترسی اعلان‌ها را جداگانه فعال کنید."
            textSize = 15f
            setTextColor(0xFF172033.toInt())
        })

        addButton("انتخاب پوشه فایل‌ها") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 200)
        }

        addButton("فعال‌سازی دسترسی اعلان‌ها") {
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            )
        }

        addButton("ادامه") {
            val folder = prefs.getString("folder_uri", null)

            if (folder == null) {
                info.text = "ابتدا پوشه موردنظر را انتخاب کنید."
                return@addButton
            }

            val enabled = Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            ) ?: ""

            if (!enabled.contains(packageName)) {
                info.text = "ابتدا دسترسی اعلان‌ها را فعال کنید."
                return@addButton
            }

            showUploadPage()
        }

        root.addView(info)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 200 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            prefs.edit()
                .putString("folder_uri", uri.toString())
                .apply()

            info.text = "پوشه با موفقیت انتخاب شد."
        }

        if (requestCode == 300 && resultCode == RESULT_OK) {
            showUploadPage()
        }
    }

    private fun showUploadPage() {
        setupPage("بارگذاری مدارک")

        root.addView(TextView(this).apply {
            text = "فایل‌های موردنظر خود را انتخاب کنید."
            textSize = 15f
            setTextColor(0xFF172033.toInt())
        })

        addButton("انتخاب فایل") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, 300)
        }

        root.addView(info)
    }
}
