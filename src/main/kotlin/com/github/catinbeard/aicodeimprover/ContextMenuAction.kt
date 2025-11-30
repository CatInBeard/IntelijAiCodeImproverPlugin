package com.github.catinbeard.aicodeimprover

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.NlsSafe


class ContextMenuAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val document = editor.document

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd

        val selectedText = editor.selectionModel.selectedText ?: return

        val notification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
            .createNotification("AI is thinking...", NotificationType.INFORMATION)
        notification.notify(project)

        ApplicationManager.getApplication().executeOnPooledThread {
            val improved = improveText(selectedText)
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    document.replaceString(start, end, improved)
                }
                notification.expire()
                val successNotification = NotificationGroupManager.getInstance().getNotificationGroup("AI Code Improver")
                    .createNotification("Code improved!", NotificationType.INFORMATION)
                successNotification.notify(project)
            }
        }
    }

    fun improveText(code: @NlsSafe String?): String {
        val client = LMStudioClient()
        println("Code: $code")
        val response = client.getSimpleAnswer(
            "google/gemma-3-12b",
            "You receive code, add usefully but dummy comments before every line of code. Answer only code, without markdown formatting, ONLY CODE",
            code?:"")

        val regex = Regex("<think>.*?</think>", RegexOption.MULTILINE)
        val responseNoThink = response.replace("\r", "").replace(regex, "")
        println("Response: $responseNoThink")
        return responseNoThink
    }
}
