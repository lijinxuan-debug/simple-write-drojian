package com.example.accounting.ui.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.accounting.adapter.MessageAdapter
import com.example.accounting.data.model.Message
import com.example.accounting.databinding.FragmentDiscoveryBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception

class DiscoveryFragment : Fragment() {
    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 需要先初始化recyclerview
        messageAdapter = MessageAdapter()
        binding.recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(context)
        }

        val welcomeList = listOf(
            Message("你好！我是你的智能理财助手。你可以问我“本月超支了吗？”或者“如何优化餐饮支出？”，我会为你提供建议。", false)
        )
        messageAdapter.submitList(welcomeList)

        // 添加监听事件，发送消息
        binding.btnSend.setOnClickListener {
            val content = binding.etInput.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                binding.etInput.text.clear()
            }
        }
    }

    /**
     * 发送消息使用
     */
    private fun sendMessage(content: String) {
        // 也要构建AI的信息体
        val loadingMessage = Message("", isMine = false, isLoading = true)
        // 需要先将用户信息添加到列表
        val newList = messageAdapter.currentList.toMutableList()
        newList.add(Message(content, true))
        newList.add(loadingMessage)
        messageAdapter.submitList(newList) {
            // 将用户视角移动到最新的消息哪里
            binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
        }

        // 调用网络服务接口
        fetchAiResponse(content,loadingMessage)
    }

    private fun fetchAiResponse(question: String,loadingMessage: Message) {
        // 先构建请求json
        val client = OkHttpClient()

        val requestBodyJson = JSONObject()
        requestBodyJson.put("message", question)

        // 实现mediaType对象
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://8.130.105.49:8080/api/ai/chat")
            .post(body)
            .build()

        // 发送异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 因为要更新页面，所以要在主线程执行
                activity?.runOnUiThread {
                    Toast.makeText(context,"当前网络不佳，请检查网络正常后再重试",1000)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                if (response.isSuccessful && responseString != null) {
                    try {
                        // 获取里面的answer字段
                        val jsonObject = JSONObject(responseString)
                        val content = jsonObject.optString("answer", "服务器异常，请联系管理员")

                        activity?.runOnUiThread {
                            val newList = messageAdapter.currentList.toMutableList()
                            // 移除等待消息
                            newList.remove(loadingMessage)
                            newList.add(Message(content,false))
                            messageAdapter.submitList(newList) {
                                // 自动滑动到底部即可
                                binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        })
    }

}