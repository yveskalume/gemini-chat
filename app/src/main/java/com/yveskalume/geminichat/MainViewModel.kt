package com.yveskalume.geminichat

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // set your API key here
    private val apiKey = " "

    val conversations = mutableStateListOf<Triple<String, String, List<Bitmap>?>>()

    private lateinit var lastUsedModel: String
    private var chat: Chat? = null

    private fun getChat(modelName: String): Chat {

        if (chat == null) {
            chat = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            ).startChat(
                history = conversations.toList().map {
                    content(role = it.first) {
                        text(it.second)
                        if (it.third != null) {
                            it.third!!.forEach { image -> image(image) }
                        }
                    }
                }
            )
            lastUsedModel = modelName
        } else if (lastUsedModel != modelName) {
            chat = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            ).startChat(
                history = conversations.toList().map {
                    content(role = it.first) {
                        text(it.second)
                        if (it.third != null) {
                            it.third!!.forEach { image -> image(image) }
                        }
                    }
                }
            )
            lastUsedModel = modelName
        }

        return chat!!
    }

    val isGenerating = mutableStateOf(false)


    fun sendText(textPrompt: String, images: SnapshotStateList<Bitmap>) {

        isGenerating.value = true

        viewModelScope.launch {
            conversations.add(Triple("user", textPrompt, images.toList()))

            // just to simulate a chat delay
            delay(1000)
            conversations.add(Triple("model", "", null))

            val chat = getChat(if (images.isNotEmpty()) "gemini-pro-vision" else "gemini-pro")

            val inputContent = content {
                images.forEach { imageBitmap ->
                    image(imageBitmap)
                }
                text(textPrompt)
            }

            // unfortunately, the sendMessageStream is throwing an exception when use images.
            // waiting for a fix.
            chat.sendMessageStream(textPrompt).collect { chunk ->
                conversations[conversations.lastIndex] = Triple(
                    "model",
                    conversations.last().second + chunk.text,
                    null
                )
            }
            isGenerating.value = false
        }
    }

}