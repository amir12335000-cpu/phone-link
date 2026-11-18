package ir.phonelink

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var serverInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("phone_link", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 42, 36, 36)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        fun addText(text: String, size: Float, bold: Boolean = false) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(0xFF172033.toInt())
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 8)
            })
        }
        fun addField(hint: String, value: String = ""): EditText {
            val field = EditText(this).apply {
                this.hint = hint
                setText(value)
                textSize = 16f
                singleLine = true
                setPadding(18, 12, 18, 12)
                background = getDrawable(android.R.drawable.edit_text)
            }
            root.addView(field, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 18 })
            return field
        }
        fun addButton(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 })
        }

        addText("Phone Link", 26f, true)
        addText("تنظیمات اتصال", 15f)
        addText("۱) آدرس سرور WebSocket", 14f, true)
        serverInput = addField("مثلاً wss://your-server.example", prefs.getString("server", "") ?: "")
        addText("۲) کد اتصال ۱۲ کاراکتری", 14f, true)
        codeInput = addField("کد اتصال", prefs.getString("code", "") ?: "")
        addButton("ذخیره تنظیمات") {
            val server = serverInput.text.toString().trim().trimEnd('/')
            val code = codeInput.text.toString().trim().uppercase()
            if (!(server.startsWith("wss://") || server.startsWith("ws://"))) {
                info.text = "آدرس باید با wss:// یا ws:// شروع شود."
                return@addButton
            }
            if (!code.matches(Regex("[A-Z0-9]{12}"))) {
                info.text = "کد باید دقیقاً ۱۲ حرف یا عدد انگلیسی باشد."
                return@addButton
            }
            prefs.edit().putString("server", server).putString("code", code).apply()
            info.text = "تنظیمات اتصال ذخیره شد."
        }
        addText("قابلیت انتقال اعلان‌ها از این نسخه حذف شده است.", 13f)
        info = TextView(this).apply {
            text = "آدرس سرور و کد اتصال را وارد و ذخیره کن."
            textSize = 13f
            setTextColor(0xFF315A80.toInt())
            setPadding(0, 18, 0, 0)
        }
        root.addView(info)
        setContentView(root)
    }
}
