package com.student.chatify.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.student.chatify.R
import com.student.chatify.recyclerView.ChatAdapter
import com.student.chatify.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private val chatAdapter by lazy { ChatAdapter() }
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        messageEditText = view.findViewById(R.id.messageEditText)
        sendButton = view.findViewById(R.id.sendButton)

        setupRecyclerView()
        setupSendButton()
        setupMessageInputListener()
        observeViewModel()

        chatViewModel.loadChatHistory()
        view.viewTreeObserver.addOnGlobalLayoutListener {
            chatRecyclerView.post {
                chatAdapter.currentList.let { list ->
                    if (list.isNotEmpty()) {
                        chatRecyclerView.scrollToPosition(list.size - 1)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                chatViewModel.sendUserMessage(text)
                messageEditText.text.clear()
            }
        }
    }

    private fun setupMessageInputListener() {
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        chatViewModel.chatItems.observe(viewLifecycleOwner) { items ->
            chatAdapter.submitList(items)
            if (!items.isNullOrEmpty()) {
                chatRecyclerView.scrollToPosition(items.size - 1)
            }
        }

        chatViewModel.typingStatus.observe(viewLifecycleOwner) { isTyping ->
            chatAdapter.updateTypingStatus(isTyping)
            if (isTyping && chatAdapter.itemCount > 0) {
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }
}
