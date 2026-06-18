package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiAssistant {

    suspend fun summarizeDocument(documentTitle: String, contentText: String): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty()) return@withContext "خطا: کلید API مفقود شده است. لطفاً آن را در بخش تنظیمات تنظیم کنید."

        val prompt = """
            شما یک دستیار هوش مصنوعی متخصص در مهندسی و مستندات فنی هستید.
            لطفاً یک خلاصه تخصصی، ساختاریافته و کاملاً فارسی از سند فنی با عنوان "$documentTitle" بر اساس متن زیر ارائه دهید.
            خلاصه باید شامل موارد زیر باشد:
            ۱. هدف اصلی سند
            ۲. نکات کلیدی و فنی برجسته
            ۳. الزامات مهم اجرایی یا استانداردها به صورت لیست موردی (bullet-points)
            
            متن سند:
            $contentText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = "شما یک مهندس ارشد و دستیار فنی استانداردها با تخصص در آیین‌نامه‌ها هستید.")))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "خطا: هوش مصنوعی پاسخی ارسال نکرد."
        } catch (e: Exception) {
            "خطا در ارتباط با هوش مصنوعی: ${e.localizedMessage}"
        }
    }

    suspend fun generateChecklist(documentTitle: String, contentText: String): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty()) return@withContext "خطا: کلید API مفقود شده است."

        val prompt = """
            بر اساس متن سند فنی زیر برای "$documentTitle"، یک چک‌لیست کاربردی و نظارتی مهندسی برای کارهای اجرایی استخراج کنید.
            هر آیتم باید واضح، دقیق و سنجش‌پذیر باشد. فرمت خروجی فارسی شیوا و حرفه‌ای باشد.
            
            متن سند:
            $contentText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f),
            systemInstruction = Content(parts = listOf(Part(text = "شما متخصص ممیزی فنی و چک‌لیست‌های استاندارد مهندسی هستید.")))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "خطا: امکان استخراج چک‌لیست فراهم نشد."
        } catch (e: Exception) {
            "خطا: ${e.localizedMessage}"
        }
    }

    suspend fun askDocumentQuestion(documentTitle: String, contentText: String, question: String): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty()) return@withContext "خطا: کلید API مفقود شده است."

        val prompt = """
            با استفاده از محتوای سند فنی زیر برای "$documentTitle"، به این سوال به صورت دقیق، علمی و کاملاً فارسی پاسخ دهید.
            سوال: $question
            
            محتوای سند:
            $contentText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = "شما با ارجاع دقیق به متن مهندسی، سوالات کاربران را پاسخ می‌دهید.")))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "یافته‌ای برای پاسخ به سوال شما در متن سند یافت نشد."
        } catch (e: Exception) {
            "خطا در تحلیل سوال: ${e.localizedMessage}"
        }
    }

    suspend fun performOcrOnText(scannedContentText: String): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isEmpty()) return@withContext "خطا: کلید API مفقود شده است."

        val prompt = """
            لطفاً متن خروجی حاصل از تصویر اسکن شده فنی زیر را تصفیه و تصحیح املایی کنید. به حروف فارسی گاف، چ، پ، ژ اهمیت بدهید و کلمات لاتین یا فرمول‌ها را منظم نگهداری کنید. حاصل کار باید یک متن تمیز مهندسی باشد.
            
            متن اسکن شده خام:
            $scannedContentText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = "شما یک موتور تصحیح OCR و مترجم تصاویر اسکن شده فنی متبحر هستید.")))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: scannedContentText
        } catch (e: Exception) {
            scannedContentText
        }
    }
}
