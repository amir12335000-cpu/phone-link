
package ir.phonelink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class MainActivity : Activity() {

    private val serverUrl =
        "https://smooth-pix-copyright-recorder.trycloudflare.com"

    private val selectedFiles = mutableListOf<Uri>()

    private lateinit var root: LinearLayout
    private lateinit var info: TextView
    private lateinit var tokenInput: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkFilePermission()
    }

    override fun onResume() {
        super.onResume()

        if (::root.isInitialized &&
            Environment.isExternalStorageManager()) {
            showUploadPage()
        }
    }

    private fun checkFilePermission() {
        if (Environment.isExternalStorageManager()) {
            showUploadPage()
        } else {
            showPermissionPage()
        }
    }

    private fun setupPage(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 36)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        root.addView(TextView(this).apply {
            text = "Alzahra Univercity"
            textSize = 25f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 8, 0, 25)
        })

        root.addView(TextView(this).apply {
            text = title
            textSize = 21f
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

    private fun addButton(
        label: String,
        action: () -> Unit
    ) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun showPermissionPage() {
        setupPage("دسترسی به فایل‌ها")

        root.addView(TextView(this).apply {
            text = """
                برای استفاده از برنامه، دسترسی گسترده به فایل‌های حافظه مشترک گوشی لازم است.

                این دسترسی از تنظیمات اندروید فعال می‌شود.
                برنامه فقط فایل‌هایی را که خودتان برای ارسال انتخاب می‌کنید بارگذاری می‌کند.
            """.trimIndent()

            textSize = 16f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 8, 0, 25)
        })

        addButton("فعال‌سازی دسترسی") {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                )
                startActivity(intent)
            }
        }

        root.addView(info)
    }

    private fun showUploadPage() {
        selectedFiles.clear()
        setupPage("بارگذاری مدارک")

        root.addView(TextView(this).apply {
            text = "دسترسی فایل‌ها فعال است."
            textSize = 15f
            setTextColor(0xFF16803C.toInt())
            setPadding(0, 5, 0, 15)
        })

        root.addView(TextView(this).apply {
            text = "فایل‌هایی را که می‌خواهید ارسال شوند، خودتان انتخاب کنید."
            textSize = 15f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 5, 0, 15)
        })

        tokenInput = EditText(this).apply {
            hint = "رمز ارسال سرور"
            inputType = 129
        }
        root.addView(tokenInput)

        addButton("انتخاب مدارک") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, 300)
        }

        sendButton = Button(this).apply {
            text = "ارسال مدارک انتخاب‌شده"
            setOnClickListener {
                uploadFiles()
            }
        }
        root.addView(sendButton)

        info = TextView(this).apply {
            text = "هنوز فایلی انتخاب نشده است."
            textSize = 15f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 20, 0, 0)
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
            selectedFiles.clear()

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
                "تعداد فایل‌های انتخاب‌شده: ${selectedFiles.size}\n\n" +
                    selectedFiles.joinToString("\n") {
                        getFileName(it)
                    }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "selected_file"

        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name
    }

    private fun uploadFiles() {
        if (selectedFiles.isEmpty()) {
            info.text = "ابتدا مدارک را انتخاب کنید."
            return
        }

        val token = tokenInput.text.toString().trim()

        if (token.isEmpty()) {
            info.text = "رمز ارسال را وارد کنید."
            return
        }

        val filesToSend = selectedFiles.toList()

        sendButton.isEnabled = false
        info.text = "در حال ارسال فایل‌ها..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val boundary = "----Android${UUID.randomUUID()}"

                connection = (URL("$serverUrl/upload")
                    .openConnection() as HttpURLConnection).apply {

                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 60000
                    doOutput = true

                    setRequestProperty(
                        "X-Upload-Token",
                        token
                    )

                    setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary"
                    )

                    setChunkedStreamingMode(0)
                }

                connection.outputStream.use { output ->

                    for (uri in filesToSend) {

                        val name = getFileName(uri)
                            .replace("\"", "_")
                            .replace("\r", "_")
                            .replace("\n", "_")

                        output.write(
                            ("--$boundary\r\n" +
                            "Content-Disposition: form-data; " +
                            "name=\"files\"; filename=\"$name\"\r\n" +
                            "Content-Type: application/octet-stream\r\n\r\n")
                                .toByteArray(Charsets.UTF_8)
                        )

                        val input = contentResolver.openInputStream(uri)
                            ?: throw Exception("خواندن فایل ممکن نیست.")

                        input.use {
                            it.copyTo(output)
                        }

                        output.write("\r\n".toByteArray())
                    }

                    output.write(
                        "--$boundary--\r\n".toByteArray()
                    )
                }

                val code = connection.responseCode

                val response = (
                    if (code in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                )?.bufferedReader()?.use {
                    it.readText()
                } ?: "بدون پاسخ"

                runOnUiThread {
                    info.text = if (code in 200..299) {
                        "ارسال با موفقیت انجام شد.\n\n$response"
                    } else {
                        "خطا در ارسال: $code\n$response"
                    }

                    sendButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    info.text = "خطا در ارسال: ${e.message}"
                    sendButton.isEnabled = true
                }
            } finally {
                connection?.disconnect()
            }

        }.start()
    }
}
