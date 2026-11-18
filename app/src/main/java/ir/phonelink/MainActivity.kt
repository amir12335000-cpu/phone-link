
package ir.phonelink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showUploadPage()
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
            setPadding(0, 20, 0, 0)
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

    private fun showUploadPage() {
        setupPage("بارگذاری مدارک")

        root.addView(TextView(this).apply {
            text = "برای بارگذاری مدارک، فایل‌های موردنظر خود را انتخاب کنید."
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

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 300 && resultCode == RESULT_OK) {
            val selectedFiles = mutableListOf<Uri>()

            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    selectedFiles.add(clip.getItemAt(i).uri)
                }
            } ?: data?.data?.let {
                selectedFiles.add(it)
            }

            info.text = if (selectedFiles.isEmpty()) {
                "فایلی انتخاب نشد."
            } else {
                "فایل‌های انتخاب‌شده:\n\n" +
                    selectedFiles.joinToString("\n") {
                        it.lastPathSegment ?: "فایل انتخاب‌شده"
                    }
            }
        }
    }
}
