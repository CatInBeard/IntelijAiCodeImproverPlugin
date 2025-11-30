package com.github.catinbeard.aicodeimprover

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit


data class ModelData(
    val id: String,
)

data class ResponseModelData(
    val data: List<ModelData>
)

data class Message(val role: String, val content: String)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    val max_tokens: Int,
    val stream: Boolean
)


data class Choice(val message: Message)
data class ResponseCompleteData(val choices: List<Choice>)

class LMStudioClient {
    companion object {
        private const val BASE_URL = "http://localhost:1234/api/v0"
    }

    private val client = OkHttpClient()

    fun getModels(): List<String> {
        val request = Request.Builder()
            .url("$BASE_URL/models")
            .get()
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get models: ${response.code}")
        }

        val modelsJson = response.body.string()

        val gson = Gson()
        val responseData = gson.fromJson(modelsJson, ResponseModelData::class.java)
        return responseData.data.map { it.id }
    }


    fun getSimpleAnswer(modelName: String, systemPrompt: String, userPrompt: String): String {

        val client = OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build()

        val gson = Gson()
        val mediaType = "application/json".toMediaType()

        val requestBody = ChatCompletionRequest(
            model = modelName,
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userPrompt)
            ),
            temperature = 0.1,
            max_tokens = -1,
            stream = false
        )

        val json = gson.toJson(requestBody)
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .post(body)
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get answer: ${response.code}")
        }

        val answerJson = response.body.string()

        val responseData = gson.fromJson(answerJson, ResponseCompleteData::class.java)
        return responseData.choices.lastOrNull()?.message?.content ?: ""
    }


}
